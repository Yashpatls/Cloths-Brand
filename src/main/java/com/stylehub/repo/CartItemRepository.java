package com.stylehub.repo;

import com.stylehub.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByCartIdAndProductIdAndSelectedSizeAndSelectedColor(
            Long cartId, Long productId, String selectedSize, String selectedColor);
    void deleteByCartId(Long cartId);
}
