package com.stylehub.service;

import com.stylehub.exception.ResourceNotFoundException;
import com.stylehub.model.*;
import com.stylehub.repo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepo;
    private final CategoryRepository categoryRepo;
    private final ProductImageRepository imageRepo;
    private final ProductSizeRepository sizeRepo;
    private final ProductColorRepository colorRepo;

    public ProductService(ProductRepository productRepo, CategoryRepository categoryRepo,
                          ProductImageRepository imageRepo, ProductSizeRepository sizeRepo,
                          ProductColorRepository colorRepo) {
        this.productRepo = productRepo;
        this.categoryRepo = categoryRepo;
        this.imageRepo = imageRepo;
        this.sizeRepo = sizeRepo;
        this.colorRepo = colorRepo;
    }

    public List<Product> getAllActive() {
        return productRepo.findByActiveTrueOrderByIdDesc();
    }

    public List<Product> getFeatured() {
        return productRepo.findByActiveTrueAndFeaturedTrueOrderByIdDesc();
    }

    @Transactional(readOnly = true)
    public Product getById(Long id) {
        Product product = productRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));
        if (product.getSizes() != null) product.getSizes().size();
        if (product.getColors() != null) product.getColors().size();
        if (product.getImages() != null) product.getImages().size();
        if (product.getReviews() != null) product.getReviews().size();
        return product;
    }

    public List<Product> getByCategory(String categoryOrSlug) {
        if (categoryOrSlug == null || categoryOrSlug.isBlank()) {
            return getAllActive();
        }
        var catOpt = categoryRepo.findBySlugIgnoreCase(categoryOrSlug);
        if (catOpt.isPresent()) {
            return productRepo.findByActiveTrueAndCategoryIdOrderByIdDesc(catOpt.get().getId());
        }
        return productRepo.findByActiveTrueAndCategoryNameIgnoreCaseOrderByIdDesc(categoryOrSlug);
    }

    public List<Product> filter(String category, String query, BigDecimal minPrice, BigDecimal maxPrice) {
        String catParam = (category != null && !category.isBlank()) ? category : null;
        String qParam = (query != null && !query.isBlank()) ? query : null;
        return productRepo.searchAndFilter(catParam, qParam, minPrice, maxPrice);
    }

    @Transactional
    public Product save(Product product, Long categoryId, List<String> sizesList, List<String> colorsList) {
        if (categoryId != null) {
            Category cat = categoryRepo.findById(categoryId).orElse(null);
            if (cat != null) {
                product.setCategory(cat);
                product.setCategoryName(cat.getName());
            }
        }
        if (product.getStock() == null) product.setStock(0);
        if (product.getSku() == null || product.getSku().isBlank()) {
            product.setSku("SKU-" + System.currentTimeMillis() % 1000000);
        }

        Product saved = productRepo.save(product);

        // Update sizes if provided
        if (sizesList != null && !sizesList.isEmpty()) {
            sizeRepo.deleteByProductId(saved.getId());
            for (String size : sizesList) {
                if (size != null && !size.isBlank()) {
                    sizeRepo.save(new ProductSize(saved, size.trim(), saved.getStock()));
                }
            }
        }

        // Update colors if provided
        if (colorsList != null && !colorsList.isEmpty()) {
            colorRepo.deleteByProductId(saved.getId());
            for (String color : colorsList) {
                if (color != null && !color.isBlank()) {
                    colorRepo.save(new ProductColor(saved, color.trim(), "#333333"));
                }
            }
        }

        return saved;
    }

    @Transactional
    public void delete(Long id) {
        Product p = getById(id);
        p.setActive(false);
        productRepo.save(p);
    }

    public List<Product> getLowStockProducts(int threshold) {
        return productRepo.findLowStockProducts(threshold);
    }

    public long getProductCount() {
        return productRepo.countByActiveTrue();
    }
}
