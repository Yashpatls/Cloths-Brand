package com.stylehub.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.stylehub.dto.RazorpayOrderResponse;
import com.stylehub.model.*;
import com.stylehub.repo.PaymentRepository;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.SignatureException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepo;

    @Value("${razorpay.key.id:rzp_test_l99uVs9OtIIqFL}")
    private String keyId;

    @Value("${razorpay.key.secret:jSfm5ENBwuQRwAqPNViQwHCm}")
    private String keySecret;

    @Value("${razorpay.currency:INR}")
    private String currency;

    public PaymentService(PaymentRepository paymentRepo) {
        this.paymentRepo = paymentRepo;
    }

    public RazorpayOrderResponse createRazorpayOrder(Long orderId, String orderNumber, BigDecimal amount) {
        // Amount in paise (1 INR = 100 paise)
        int amountInPaise = amount.multiply(BigDecimal.valueOf(100)).intValue();
        String razorpayOrderId = "order_mock_" + UUID.randomUUID().toString().substring(0, 8);

        try {
            if (keyId != null && !keyId.startsWith("rzp_test_mock") && keySecret != null && !keySecret.startsWith("mock_")) {
                RazorpayClient client = new RazorpayClient(keyId, keySecret);
                JSONObject orderRequest = new JSONObject();
                orderRequest.put("amount", amountInPaise);
                orderRequest.put("currency", currency);
                orderRequest.put("receipt", orderNumber);
                Order rzpOrder = client.orders.create(orderRequest);
                razorpayOrderId = rzpOrder.get("id");
                log.info("Created real Razorpay order ID: {}", razorpayOrderId);
            } else {
                log.info("Using simulated Razorpay order ID for demo/test mode: {}", razorpayOrderId);
            }
        } catch (Exception e) {
            log.warn("Razorpay API call failed or in mock mode; returning simulated order: {}", e.getMessage());
        }

        // Record initial pending payment
        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setPaymentMethod(PaymentMethod.RAZORPAY);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setAmount(amount);
        payment.setCurrency(currency);
        payment.setRazorpayOrderId(razorpayOrderId);
        payment.setTransactionId("TXN-" + System.currentTimeMillis());
        paymentRepo.save(payment);

        return new RazorpayOrderResponse(razorpayOrderId, keyId, amount, currency, orderNumber);
    }

    @Transactional
    public boolean verifyRazorpayPayment(Long orderId, String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
        boolean isValid = false;

        if (razorpayOrderId != null && razorpayOrderId.startsWith("order_mock_")) {
            // Test mock mode acceptance
            isValid = true;
        } else {
            try {
                String payload = razorpayOrderId + "|" + razorpayPaymentId;
                Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
                SecretKeySpec secret_key = new SecretKeySpec(keySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
                sha256_HMAC.init(secret_key);
                byte[] hash = sha256_HMAC.doFinal(payload.getBytes(StandardCharsets.UTF_8));
                String calculatedSignature = HexFormat.of().formatHex(hash);
                isValid = calculatedSignature.equalsIgnoreCase(razorpaySignature);
            } catch (Exception e) {
                log.error("Signature verification error: {}", e.getMessage());
            }
        }

        // Update payment status
        Payment payment = paymentRepo.findByRazorpayOrderId(razorpayOrderId)
                .orElseGet(() -> {
                    Payment p = new Payment();
                    p.setOrderId(orderId);
                    p.setPaymentMethod(PaymentMethod.RAZORPAY);
                    p.setRazorpayOrderId(razorpayOrderId);
                    return p;
                });

        payment.setRazorpayPaymentId(razorpayPaymentId);
        payment.setRazorpaySignature(razorpaySignature);
        payment.setStatus(isValid ? PaymentStatus.COMPLETED : PaymentStatus.FAILED);
        payment.setPaymentDate(LocalDateTime.now());
        paymentRepo.save(payment);

        return isValid;
    }

    @Transactional
    public Payment recordDemoOrCodPayment(Long orderId, PaymentMethod method, BigDecimal amount) {
        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setPaymentMethod(method);
        payment.setStatus(method == PaymentMethod.DEMO ? PaymentStatus.COMPLETED : PaymentStatus.PENDING);
        payment.setAmount(amount);
        payment.setCurrency(currency);
        payment.setTransactionId(method.name() + "-" + System.currentTimeMillis());
        payment.setPaymentDate(LocalDateTime.now());
        return paymentRepo.save(payment);
    }
}
