package com.stylehub.controller;

import com.stylehub.model.Wishlist;
import com.stylehub.service.CartService;
import com.stylehub.service.WishlistService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
public class WishlistController {

    private final WishlistService wishlistService;
    private final CartService cartService;

    public WishlistController(WishlistService wishlistService, CartService cartService) {
        this.wishlistService = wishlistService;
        this.cartService = cartService;
    }

    @GetMapping("/wishlist")
    public String viewWishlist(Authentication auth, Model model) {
        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/login";
        }
        List<Wishlist> wishlist = wishlistService.getUserWishlist(auth.getName());
        model.addAttribute("wishlist", wishlist);
        return "wishlist";
    }

    @PostMapping("/wishlist/toggle")
    @ResponseBody
    public ResponseEntity<?> toggleWishlist(@RequestParam Long productId, Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("status", "unauthorized", "redirect", "/login"));
        }
        boolean isAdded = wishlistService.toggle(auth.getName(), productId);
        long count = wishlistService.getCount(auth.getName());
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "added", isAdded,
                "wishlistCount", count
        ));
    }

    @PostMapping("/wishlist/move-to-cart")
    public String moveToCart(
            @RequestParam Long productId,
            Authentication auth,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/login";
        }
        try {
            cartService.addItem(auth.getName(), session.getId(), productId, 1, "M", "Black");
            wishlistService.toggle(auth.getName(), productId); // remove from wishlist
            redirectAttributes.addFlashAttribute("successMessage", "Item moved to your shopping bag.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/wishlist";
    }
}
