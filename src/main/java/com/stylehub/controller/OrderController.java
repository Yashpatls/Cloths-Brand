package com.stylehub.controller;

import com.stylehub.dto.CheckoutDto;
import com.stylehub.dto.RazorpayOrderResponse;
import com.stylehub.model.Cart;
import com.stylehub.model.CustomerOrder;
import com.stylehub.model.OrderStatus;
import com.stylehub.model.PaymentStatus;
import com.stylehub.service.CartService;
import com.stylehub.service.OrderService;
import com.stylehub.service.PaymentService;
import com.stylehub.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.Map;

@Controller
public class OrderController {

    private final OrderService orderService;
    private final CartService cartService;
    private final UserService userService;
    private final PaymentService paymentService;

    @Value("${razorpay.key.id:rzp_test_l99uVs9OtIIqFL}")
    private String razorpayKeyId;

    public OrderController(OrderService orderService, CartService cartService,
                           UserService userService, PaymentService paymentService) {
        this.orderService = orderService;
        this.cartService = cartService;
        this.userService = userService;
        this.paymentService = paymentService;
    }

    @GetMapping("/checkout")
    public String checkout(Authentication auth, HttpSession session, Model model) {
        String email = auth.getName();
        Cart cart = cartService.getOrCreateCart(email, session.getId());

        if (cart.getItems().isEmpty()) {
            return "redirect:/cart";
        }

        String couponCode = (String) session.getAttribute("appliedCoupon");
        model.addAttribute("cart", cart);
        model.addAttribute("addresses", userService.getUserAddresses(userService.getByEmail(email).getId()));
        model.addAttribute("appliedCouponCode", couponCode);
        model.addAttribute("checkoutDto", new CheckoutDto());
        model.addAttribute("razorpayKeyId", razorpayKeyId);
        return "checkout";
    }

    @PostMapping("/checkout/place-order")
    public String placeOrder(
            @ModelAttribute CheckoutDto checkoutDto,
            Authentication auth,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        try {
            String email = auth.getName();
            Cart cart = cartService.getOrCreateCart(email, session.getId());

            String couponCode = (String) session.getAttribute("appliedCoupon");
            checkoutDto.setCouponCode(couponCode);

            CustomerOrder order = orderService.placeOrder(email, cart, checkoutDto);
            session.removeAttribute("appliedCoupon");

            return "redirect:/checkout-success?orderNumber=" + order.getOrderNumber();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/checkout";
        }
    }

    @PostMapping("/checkout/create-razorpay-order")
    @ResponseBody
    public ResponseEntity<?> createRazorpayOrder(
            @RequestBody Map<String, Object> payload,
            Authentication auth,
            HttpSession session) {

        try {
            String email = auth.getName();
            Cart cart = cartService.getOrCreateCart(email, session.getId());

            CheckoutDto dto = new CheckoutDto();
            dto.setFullName((String) payload.get("fullName"));
            dto.setPhone((String) payload.get("phone"));
            dto.setStreetAddress((String) payload.get("streetAddress"));
            dto.setCity((String) payload.get("city"));
            dto.setState((String) payload.get("state"));
            dto.setPostalCode((String) payload.get("postalCode"));
            dto.setPaymentMethod("RAZORPAY");
            dto.setCouponCode((String) session.getAttribute("appliedCoupon"));

            CustomerOrder order = orderService.placeOrder(email, cart, dto);
            session.removeAttribute("appliedCoupon");

            RazorpayOrderResponse response = paymentService.createRazorpayOrder(
                    order.getId(), order.getOrderNumber(), order.getTotal());

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "razorpayOrderId", response.getRazorpayOrderId(),
                    "keyId", response.getKeyId(),
                    "amount", response.getAmount().multiply(BigDecimal.valueOf(100)).intValue(),
                    "currency", response.getCurrency(),
                    "orderNumber", order.getOrderNumber(),
                    "orderId", order.getId()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    @PostMapping("/checkout/verify-payment")
    @ResponseBody
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, String> payload) {
        try {
            Long orderId = Long.parseLong(payload.get("orderId"));
            String razorpayOrderId = payload.get("razorpay_order_id");
            String razorpayPaymentId = payload.get("razorpay_payment_id");
            String razorpaySignature = payload.get("razorpay_signature");

            boolean verified = paymentService.verifyRazorpayPayment(
                    orderId, razorpayOrderId, razorpayPaymentId, razorpaySignature);

            if (verified) {
                CustomerOrder order = orderService.getById(orderId);
                order.setStatus(OrderStatus.CONFIRMED);
                order.setPaymentStatus(PaymentStatus.COMPLETED);
                orderService.updateOrderStatus(orderId, OrderStatus.CONFIRMED);
                return ResponseEntity.ok(Map.of("status", "success", "orderNumber", order.getOrderNumber()));
            } else {
                return ResponseEntity.badRequest().body(Map.of("status", "failed", "message", "Signature verification failed"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    @GetMapping("/checkout-success")
    public String checkoutSuccess(@RequestParam String orderNumber, Model model) {
        CustomerOrder order = orderService.getByOrderNumber(orderNumber);
        model.addAttribute("order", order);
        return "checkout-success";
    }

    @GetMapping("/orders")
    public String myOrders(Authentication auth, Model model) {
        model.addAttribute("orders", orderService.getCustomerOrders(auth.getName()));
        return "orders";
    }

    @GetMapping("/orders/{id}")
    public String orderDetail(@PathVariable Long id, Authentication auth, Model model) {
        CustomerOrder order = orderService.getById(id);
        // Ensure customer can only view their own order unless admin
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin && !order.getCustomer().getEmail().equalsIgnoreCase(auth.getName())) {
            return "redirect:/orders";
        }
        model.addAttribute("order", order);
        return "order-detail";
    }
}
