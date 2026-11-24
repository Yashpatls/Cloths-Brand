package com.stylehub.service;

import com.stylehub.exception.ResourceNotFoundException;
import com.stylehub.model.Product;
import com.stylehub.model.User;
import com.stylehub.model.Wishlist;
import com.stylehub.repo.ProductRepository;
import com.stylehub.repo.WishlistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WishlistService {

    private final WishlistRepository wishlistRepo;
    private final ProductRepository productRepo;
    private final UserService userService;

    public WishlistService(WishlistRepository wishlistRepo, ProductRepository productRepo, UserService userService) {
        this.wishlistRepo = wishlistRepo;
        this.productRepo = productRepo;
        this.userService = userService;
    }

    public List<Wishlist> getUserWishlist(String email) {
        User user = userService.getByEmail(email);
        return wishlistRepo.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    public boolean isWishlisted(String email, Long productId) {
        try {
            User user = userService.getByEmail(email);
            return wishlistRepo.existsByUserIdAndProductId(user.getId(), productId);
        } catch (Exception e) {
            return false;
        }
    }

    @Transactional
    public boolean toggle(String email, Long productId) {
        User user = userService.getByEmail(email);
        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (wishlistRepo.existsByUserIdAndProductId(user.getId(), productId)) {
            wishlistRepo.deleteByUserIdAndProductId(user.getId(), productId);
            return false; // Removed
        } else {
            wishlistRepo.save(new Wishlist(user, product));
            return true; // Added
        }
    }

    public long getCount(String email) {
        try {
            User user = userService.getByEmail(email);
            return wishlistRepo.countByUserId(user.getId());
        } catch (Exception e) {
            return 0;
        }
    }
}
