package com.stylehub.service;

import com.stylehub.dto.CheckoutDto;
import com.stylehub.exception.InsufficientStockException;
import com.stylehub.exception.ResourceNotFoundException;
import com.stylehub.model.*;
import com.stylehub.repo.ComboPackageRepository;
import com.stylehub.repo.OrderRepository;
import com.stylehub.repo.ProductRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

@Service
public class OrderService {

    private final OrderRepository orderRepo;
    private final ProductRepository productRepo;
    private final ComboPackageRepository comboRepo;
    private final CartService cartService;
    private final UserService userService;
    private final CouponService couponService;
    private final PaymentService paymentService;

    @Value("${stylehub.tax.rate:0.05}")
    private BigDecimal taxRate;

    @Value("${stylehub.shipping.flat:99.00}")
    private BigDecimal flatShippingFee;

    @Value("${stylehub.shipping.free-threshold:1500.00}")
    private BigDecimal freeShippingThreshold;

    public OrderService(OrderRepository orderRepo, ProductRepository productRepo,
                        ComboPackageRepository comboRepo, CartService cartService,
                        UserService userService, CouponService couponService,
                        PaymentService paymentService) {
        this.orderRepo = orderRepo;
        this.productRepo = productRepo;
        this.comboRepo = comboRepo;
        this.cartService = cartService;
        this.userService = userService;
        this.couponService = couponService;
        this.paymentService = paymentService;
    }

    public List<CustomerOrder> getOrdersByUser(String email) {
        User user = userService.getByEmail(email);
        return orderRepo.findByCustomerIdOrderByCreatedAtDesc(user.getId());
    }

    public List<CustomerOrder> getCustomerOrders(String email) {
        return getOrdersByUser(email);
    }

    public CustomerOrder getById(Long id) {
        return orderRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + id));
    }

    public CustomerOrder getByOrderNumber(String orderNumber) {
        return orderRepo.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with number: " + orderNumber));
    }

    public List<CustomerOrder> getAllOrders() {
        return orderRepo.findAllByOrderByCreatedAtDesc();
    }

    public List<CustomerOrder> getOrdersByStatus(OrderStatus status) {
        return orderRepo.findByStatusOrderByCreatedAtDesc(status);
    }

    @Transactional
    public CustomerOrder placeOrder(String email, Cart cart, CheckoutDto checkoutDto) {
        if (cart == null || cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Cannot place order with an empty bag.");
        }

        User customer = userService.getByEmail(email);

        // 1. Calculate Subtotal & Verify Stock
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem item : cart.getItems()) {
            if (item.isCombo() && item.getComboPackage() != null) {
                ComboPackage combo = comboRepo.findById(item.getComboPackage().getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Combo package not found"));

                if (!combo.isActive()) {
                    throw new InsufficientStockException("Combo package '" + combo.getName() + "' is currently unavailable.");
                }

                // Check stock only for constituent products that are actually included
                List<Long> incPids = null;
                if (item.getIncludedProductIds() != null && !item.getIncludedProductIds().isBlank()) {
                    incPids = java.util.Arrays.stream(item.getIncludedProductIds().split(","))
                            .map(String::trim).filter(s -> !s.isEmpty()).map(Long::parseLong).toList();
                }

                for (ComboItem comboItem : combo.getItems()) {
                    if (incPids == null || incPids.contains(comboItem.getProduct().getId())) {
                        Product p = comboItem.getProduct();
                        int required = comboItem.getQuantity() * item.getQuantity();
                        if (p == null || !p.isActive() || p.getStock() < required) {
                            throw new InsufficientStockException("Garment '" + comboItem.getCustomTitle() + "' in bundle has insufficient stock.");
                        }
                    }
                }
                subtotal = subtotal.add(item.getTotalPrice());
            } else if (item.getProduct() != null) {
                Product p = productRepo.findById(item.getProduct().getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

                if (!p.isActive() || p.getStock() < item.getQuantity()) {
                    throw new InsufficientStockException("Product '" + p.getName() + "' is out of stock or has insufficient quantity.");
                }
                subtotal = subtotal.add(item.getTotalPrice());
            }
        }

        // 2. Calculate Coupon Discount
        BigDecimal discount = BigDecimal.ZERO;
        if (checkoutDto.getCouponCode() != null && !checkoutDto.getCouponCode().isBlank()) {
            var couponOpt = couponService.validateCoupon(checkoutDto.getCouponCode(), subtotal);
            if (couponOpt.isPresent()) {
                discount = couponService.calculateDiscount(couponOpt.get(), subtotal);
                couponService.recordUsage(checkoutDto.getCouponCode());
            }
        }

        BigDecimal taxableAmount = subtotal.subtract(discount);
        if (taxableAmount.compareTo(BigDecimal.ZERO) < 0) taxableAmount = BigDecimal.ZERO;

        // 3. Shipping Fee
        BigDecimal shipping = (taxableAmount.compareTo(freeShippingThreshold) >= 0) ? BigDecimal.ZERO : flatShippingFee;

        // 4. Tax
        BigDecimal tax = taxableAmount.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);

        // 5. Total
        BigDecimal total = taxableAmount.add(shipping).add(tax);

        // 6. Create Order Entity
        CustomerOrder order = new CustomerOrder();
        String datePrefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomSuffix = String.format("%04d", new Random().nextInt(10000));
        order.setOrderNumber("SH-" + datePrefix + "-" + randomSuffix);
        order.setCustomer(customer);
        order.setSubtotal(subtotal);
        order.setDiscountAmount(discount);
        order.setShippingFee(shipping);
        order.setTaxAmount(tax);
        order.setTotal(total);
        order.setCouponCode(checkoutDto.getCouponCode());
        order.setTrackingNumber("TRK-" + System.currentTimeMillis() % 100000000);

        // Address resolution
        String resolvedAddress = checkoutDto.getStreetAddress() + ", " + checkoutDto.getCity() + ", " +
                checkoutDto.getState() + " - " + checkoutDto.getPostalCode() + " (Phone: " + checkoutDto.getPhone() + ")";
        order.setShippingAddress(resolvedAddress);

        // Payment Method
        PaymentMethod method;
        try {
            method = PaymentMethod.valueOf(checkoutDto.getPaymentMethod().toUpperCase());
        } catch (Exception e) {
            method = PaymentMethod.ONLINE;
        }
        order.setPaymentMethod(method);

        if (method == PaymentMethod.ONLINE || method == PaymentMethod.DEMO) {
            order.setStatus(OrderStatus.CONFIRMED);
            order.setPaymentStatus(PaymentStatus.COMPLETED);
        } else {
            order.setStatus(OrderStatus.PLACED);
            order.setPaymentStatus(PaymentStatus.PENDING);
        }

        // 7. Create order items & deduct inventory
        for (CartItem ci : cart.getItems()) {
            if (ci.isCombo() && ci.getComboPackage() != null) {
                ComboPackage combo = comboRepo.findById(ci.getComboPackage().getId()).orElseThrow();

                // Parse included product IDs if custom combo
                List<Long> incPids = null;
                if (ci.getIncludedProductIds() != null && !ci.getIncludedProductIds().isBlank()) {
                    incPids = java.util.Arrays.stream(ci.getIncludedProductIds().split(","))
                            .map(String::trim).filter(s -> !s.isEmpty()).map(Long::parseLong).toList();
                }

                // Atomically deduct inventory ONLY from included products in this bundle
                for (ComboItem comboItem : combo.getItems()) {
                    if (incPids == null || incPids.contains(comboItem.getProduct().getId())) {
                        Product p = productRepo.findById(comboItem.getProduct().getId()).orElseThrow();
                        int requiredQty = comboItem.getQuantity() * ci.getQuantity();
                        p.setStock(Math.max(0, p.getStock() - requiredQty));
                        productRepo.save(p);
                    }
                }

                // Increment combo analytics
                combo.setSalesCount(combo.getSalesCount() + ci.getQuantity());
                combo.setTotalRevenue(combo.getTotalRevenue().add(ci.getTotalPrice()));
                comboRepo.save(combo);

                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(order);
                orderItem.setComboPackage(combo);
                orderItem.setCombo(true);
                if (combo.getItems() != null && !combo.getItems().isEmpty() && combo.getItems().get(0).getProduct() != null) {
                    orderItem.setProduct(combo.getItems().get(0).getProduct());
                }
                orderItem.setComboSelections(ci.getComboSelections());
                orderItem.setIncludedProductIds(ci.getIncludedProductIds());
                orderItem.setQuantity(ci.getQuantity());
                orderItem.setUnitPrice(ci.getUnitPrice());
                orderItem.setSelectedSize(ci.getComboSelections());
                orderItem.setSelectedColor("Bundle");
                order.getItems().add(orderItem);
            } else if (ci.getProduct() != null) {
                Product p = productRepo.findById(ci.getProduct().getId()).orElseThrow();
                p.setStock(p.getStock() - ci.getQuantity());
                productRepo.save(p);

                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(order);
                orderItem.setProduct(p);
                orderItem.setQuantity(ci.getQuantity());
                orderItem.setUnitPrice(ci.getUnitPrice());
                orderItem.setSelectedSize(ci.getSelectedSize());
                orderItem.setSelectedColor(ci.getSelectedColor());
                order.getItems().add(orderItem);
            }
        }

        CustomerOrder savedOrder = orderRepo.save(order);

        // Record payment record
        if (method != PaymentMethod.RAZORPAY) {
            paymentService.recordDemoOrCodPayment(savedOrder.getId(), method, total);
        }

        // Clear cart
        cartService.clearCart(cart);

        return savedOrder;
    }

    @Transactional
    public void updateOrderStatus(Long orderId, OrderStatus status) {
        CustomerOrder order = getById(orderId);
        order.setStatus(status);
        if (status == OrderStatus.DELIVERED && order.getPaymentStatus() == PaymentStatus.PENDING) {
            order.setPaymentStatus(PaymentStatus.COMPLETED);
        }
        orderRepo.save(order);
    }
}
