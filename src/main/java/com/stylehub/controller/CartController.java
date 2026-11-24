package com.stylehub.controller;

import com.stylehub.model.Cart;
import com.stylehub.model.Coupon;
import com.stylehub.service.CartService;
import com.stylehub.service.CouponService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.Optional;

@Controller
public class CartController {

    private final CartService cartService;
    private final CouponService couponService;

    public CartController(CartService cartService, CouponService couponService) {
        this.cartService = cartService;
        this.couponService = couponService;
    }

    @GetMapping("/cart")
    public String viewCart(Authentication auth, HttpSession session, Model model) {
        String email = auth != null ? auth.getName() : null;
        Cart cart = cartService.getOrCreateCart(email, session.getId());

        String couponCode = (String) session.getAttribute("appliedCoupon");
        BigDecimal discount = BigDecimal.ZERO;
        if (couponCode != null) {
            Optional<Coupon> couponOpt = couponService.validateCoupon(couponCode, cart.getSubtotal());
            if (couponOpt.isPresent()) {
                discount = couponService.calculateDiscount(couponOpt.get(), cart.getSubtotal());
                model.addAttribute("appliedCoupon", couponOpt.get());
            } else {
                session.removeAttribute("appliedCoupon");
            }
        }

        BigDecimal subtotal = cart.getSubtotal();
        BigDecimal netAmount = subtotal.subtract(discount);
        if (netAmount.compareTo(BigDecimal.ZERO) < 0) netAmount = BigDecimal.ZERO;

        BigDecimal shipping = (netAmount.compareTo(BigDecimal.valueOf(1500)) >= 0 || subtotal.compareTo(BigDecimal.ZERO) == 0)
                ? BigDecimal.ZERO : BigDecimal.valueOf(99);

        BigDecimal tax = netAmount.multiply(BigDecimal.valueOf(0.05)).setScale(2, BigDecimal.ROUND_HALF_UP);
        BigDecimal total = netAmount.add(shipping).add(tax);

        model.addAttribute("cart", cart);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("discount", discount);
        model.addAttribute("shipping", shipping);
        model.addAttribute("tax", tax);
        model.addAttribute("total", total);
        return "cart";
    }

    @PostMapping("/cart/add")
    public String addToCart(
            @RequestParam Long productId,
            @RequestParam(defaultValue = "1") int quantity,
            @RequestParam(defaultValue = "M") String size,
            @RequestParam(defaultValue = "Black") String color,
            @RequestParam(required = false) String buyNow,
            Authentication auth,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        try {
            String email = auth != null ? auth.getName() : null;
            cartService.addItem(email, session.getId(), productId, quantity, size, color);
            if ("true".equalsIgnoreCase(buyNow)) {
                return "redirect:/checkout";
            }
            redirectAttributes.addFlashAttribute("successMessage", "Item added to your shopping bag.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/products/" + productId;
        }
        return "redirect:/cart";
    }

    @PostMapping("/cart/update")
    public String updateQuantity(
            @RequestParam Long itemId,
            @RequestParam int quantity,
            RedirectAttributes redirectAttributes) {

        try {
            cartService.updateQuantity(itemId, quantity);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/cart/remove")
    public String removeItem(@RequestParam Long itemId) {
        cartService.removeItem(itemId);
        return "redirect:/cart";
    }

    @PostMapping("/cart/apply-coupon")
    public String applyCoupon(@RequestParam String code, Authentication auth, HttpSession session, RedirectAttributes redirectAttributes) {
        String email = auth != null ? auth.getName() : null;
        Cart cart = cartService.getOrCreateCart(email, session.getId());

        Optional<Coupon> couponOpt = couponService.validateCoupon(code, cart.getSubtotal());
        if (couponOpt.isPresent()) {
            session.setAttribute("appliedCoupon", couponOpt.get().getCode());
            redirectAttributes.addFlashAttribute("couponSuccess", "Coupon '" + code.toUpperCase() + "' applied successfully!");
        } else {
            redirectAttributes.addFlashAttribute("couponError", "Invalid, expired, or minimum cart amount not met for coupon: " + code);
        }
        return "redirect:/cart";
    }

    @PostMapping("/cart/remove-coupon")
    public String removeCoupon(HttpSession session) {
        session.removeAttribute("appliedCoupon");
        return "redirect:/cart";
    }
}
