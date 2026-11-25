package com.stylehub.repo;

import com.stylehub.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByActiveTrueOrderByIdDesc();

    List<Product> findByActiveTrueAndFeaturedTrueOrderByIdDesc();

    List<Product> findByActiveTrueAndCategoryNameIgnoreCaseOrderByIdDesc(String categoryName);

    List<Product> findByActiveTrueAndCategoryIdOrderByIdDesc(Long categoryId);

    @Query("SELECT p FROM Product p WHERE p.active = true AND " +
           "(p.category.id = :catId OR LOWER(p.categoryName) = LOWER(:catName) OR LOWER(p.category.slug) = LOWER(:slug)) " +
           "ORDER BY p.id DESC")
    List<Product> findByCategoryMatches(@Param("catId") Long catId, @Param("catName") String catName, @Param("slug") String slug);

    List<Product> findByNameContainingIgnoreCaseAndActiveTrue(String name);

    Optional<Product> findBySkuIgnoreCase(String sku);
    Optional<Product> findBySku(String sku);

    @Query("SELECT p FROM Product p WHERE p.active = true " +
           "AND (:category IS NULL OR :category = '' OR LOWER(p.categoryName) = LOWER(:category) OR LOWER(p.category.slug) = LOWER(:category)) " +
           "AND (:query IS NULL OR :query = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR p.price <= :maxPrice) " +
           "ORDER BY p.id DESC")
    List<Product> searchAndFilter(
            @Param("category") String category,
            @Param("query") String query,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice
    );

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.stock <= :threshold ORDER BY p.stock ASC")
    List<Product> findLowStockProducts(@Param("threshold") int threshold);

    long countByActiveTrue();
}
