package com.stylehub.controller;

import com.stylehub.model.Cart;
import com.stylehub.model.Category;
import com.stylehub.service.CartService;
import com.stylehub.service.CategoryService;
import com.stylehub.service.WishlistService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@ControllerAdvice
public class GlobalModelAttributes {

    private final CartService cartService;
    private final WishlistService wishlistService;
    private final CategoryService categoryService;

    @Value("${stylehub.store.name:StyleHub}")
    private String storeName;

    public GlobalModelAttributes(CartService cartService, WishlistService wishlistService, CategoryService categoryService) {
        this.cartService = cartService;
        this.wishlistService = wishlistService;
        this.categoryService = categoryService;
    }

    @ModelAttribute("cartCount")
    public int getCartCount(Authentication auth, HttpSession session) {
        try {
            String email = auth != null ? auth.getName() : null;
            Cart cart = cartService.getOrCreateCart(email, session.getId());
            return cart.getTotalQuantity();
        } catch (Exception e) {
            return 0;
        }
    }

    @ModelAttribute("wishlistCount")
    public long getWishlistCount(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return 0;
        return wishlistService.getCount(auth.getName());
    }

    @ModelAttribute("navCategories")
    public List<Category> getNavCategories() {
        return categoryService.getAllActive();
    }

    @ModelAttribute("storeName")
    public String getStoreName() {
        return storeName;
    }
}
