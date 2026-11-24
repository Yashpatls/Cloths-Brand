package com.stylehub.controller;

import com.stylehub.service.ReviewService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/reviews/add")
    public String addReview(
            @RequestParam Long productId,
            @RequestParam int rating,
            @RequestParam(required = false) String title,
            @RequestParam String comment,
            Authentication auth,
            RedirectAttributes redirectAttributes) {

        try {
            reviewService.addReview(auth.getName(), productId, rating, title, comment);
            redirectAttributes.addFlashAttribute("reviewSuccess", "Thank you! Your product review has been submitted.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("reviewError", e.getMessage());
        }
        return "redirect:/products/" + productId;
    }
}
