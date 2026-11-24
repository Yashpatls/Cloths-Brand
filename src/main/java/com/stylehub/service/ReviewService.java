package com.stylehub.service;

import com.stylehub.exception.ResourceNotFoundException;
import com.stylehub.model.Product;
import com.stylehub.model.Review;
import com.stylehub.model.User;
import com.stylehub.repo.ProductRepository;
import com.stylehub.repo.ReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepo;
    private final ProductRepository productRepo;
    private final UserService userService;

    public ReviewService(ReviewRepository reviewRepo, ProductRepository productRepo, UserService userService) {
        this.reviewRepo = reviewRepo;
        this.productRepo = productRepo;
        this.userService = userService;
    }

    public List<Review> getApprovedReviews(Long productId) {
        return reviewRepo.findByProductIdAndApprovedTrueOrderByCreatedAtDesc(productId);
    }

    @Transactional
    public Review addReview(String email, Long productId, int rating, String title, String comment) {
        User user = userService.getByEmail(email);
        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        int validRating = Math.max(1, Math.min(5, rating));
        Review review = new Review(product, user, validRating, title, comment);
        return reviewRepo.save(review);
    }

    @Transactional
    public void approveReview(Long id) {
        Review r = reviewRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        r.setApproved(true);
        reviewRepo.save(r);
    }

    @Transactional
    public void deleteReview(Long id) {
        reviewRepo.deleteById(id);
    }
}
