package com.stylehub.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String sku;

    @Column(length = 3000)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(precision = 10, scale = 2)
    private BigDecimal discountPrice;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private Category category;

    // Direct category name stored as well for convenience and backwards compatibility
    private String categoryName;

    private String imageUrl;

    private Integer stock = 0;

    private boolean featured = false;

    private boolean active = true;

    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("sortOrder ASC")
    private List<ProductImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ProductSize> sizes = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ProductColor> colors = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Review> reviews = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getDiscountPrice() { return discountPrice; }
    public void setDiscountPrice(BigDecimal discountPrice) { this.discountPrice = discountPrice; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) {
        this.category = category;
        if (category != null) {
            this.categoryName = category.getName();
        }
    }

    public String getCategoryName() {
        if (category != null) return category.getName();
        return categoryName != null ? categoryName : "Uncategorized";
    }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getImageUrl() {
        if (imageUrl != null && !imageUrl.isBlank()) return imageUrl;
        if (images != null && !images.isEmpty()) return images.get(0).getImageUrl();
        return "https://images.unsplash.com/photo-1523381294911-8d3cead13475?w=900";
    }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Integer getStock() { return stock != null ? stock : 0; }
    public void setStock(Integer stock) { this.stock = stock; }

    public boolean isFeatured() { return featured; }
    public void setFeatured(boolean featured) { this.featured = featured; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<ProductImage> getImages() { return images; }
    public void setImages(List<ProductImage> images) { this.images = images; }

    public List<ProductSize> getSizes() { return sizes; }
    public void setSizes(List<ProductSize> sizes) { this.sizes = sizes; }

    public List<ProductColor> getColors() { return colors; }
    public void setColors(List<ProductColor> colors) { this.colors = colors; }

    public List<Review> getReviews() { return reviews; }
    public void setReviews(List<Review> reviews) { this.reviews = reviews; }

    public double getAverageRating() {
        try {
            if (reviews == null || !org.hibernate.Hibernate.isInitialized(reviews) || reviews.isEmpty()) {
                return 5.0;
            }
            return reviews.stream().filter(Review::isApproved).mapToInt(Review::getRating).average().orElse(5.0);
        } catch (Throwable t) {
            return 5.0;
        }
    }

    public int getReviewCount() {
        try {
            if (reviews == null || !org.hibernate.Hibernate.isInitialized(reviews)) {
                return 0;
            }
            return (int) reviews.stream().filter(Review::isApproved).count();
        } catch (Throwable t) {
            return 0;
        }
    }
}
