package com.stylehub.repo;

import com.stylehub.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByProductIdAndApprovedTrueOrderByCreatedAtDesc(Long productId);
    List<Review> findByProductIdOrderByCreatedAtDesc(Long productId);
    List<Review> findByApprovedFalseOrderByCreatedAtDesc();
    long countByApprovedFalse();
}
