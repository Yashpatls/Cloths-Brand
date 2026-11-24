package com.stylehub.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "combo_packages")
public class ComboPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComboTier tier = ComboTier.BUDGET;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 1000)
    private String imageUrl;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal comboPrice;

    @Column(precision = 10, scale = 2)
    private BigDecimal originalPrice;

    @Column(precision = 10, scale = 2)
    private BigDecimal promoPrice;

    private LocalDateTime promoStartDate;
    private LocalDateTime promoEndDate;

    private boolean active = true;
    private boolean featured = false;

    private String badge; // e.g. "Best Value", "Bestseller", "Save 35%"

    private int salesCount = 0;

    @Column(precision = 12, scale = 2)
    private BigDecimal totalRevenue = BigDecimal.ZERO;

    @OneToMany(mappedBy = "comboPackage", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("displayOrder ASC")
    private List<ComboItem> items = new ArrayList<>();

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    public ComboPackage() {}

    public ComboPackage(String name, String slug, ComboTier tier, String description, String imageUrl,
                        BigDecimal comboPrice, BigDecimal originalPrice, String badge) {
        this.name = name;
        this.slug = slug;
        this.tier = tier;
        this.description = description;
        this.imageUrl = imageUrl;
        this.comboPrice = comboPrice;
        this.originalPrice = originalPrice;
        this.badge = badge;
    }

    public void addItem(ComboItem item) {
        items.add(item);
        item.setComboPackage(this);
    }

    public void removeItem(ComboItem item) {
        items.remove(item);
        item.setComboPackage(null);
    }

    public BigDecimal getEffectivePrice() {
        LocalDateTime now = LocalDateTime.now();
        if (promoPrice != null && promoStartDate != null && promoEndDate != null) {
            if (now.isAfter(promoStartDate) && now.isBefore(promoEndDate)) {
                return promoPrice;
            }
        }
        return comboPrice != null ? comboPrice : BigDecimal.ZERO;
    }

    public BigDecimal getSavingsAmount() {
        BigDecimal effective = getEffectivePrice();
        if (originalPrice != null && originalPrice.compareTo(effective) > 0) {
            return originalPrice.subtract(effective);
        }
        return BigDecimal.ZERO;
    }

    public int getSavingsPercentage() {
        if (originalPrice != null && originalPrice.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal savings = getSavingsAmount();
            return savings.multiply(BigDecimal.valueOf(100))
                    .divide(originalPrice, 0, RoundingMode.HALF_UP)
                    .intValue();
        }
        return 0;
    }

    public boolean isInStock() {
        if (items == null || items.isEmpty()) return false;
        for (ComboItem item : items) {
            if (item.getProduct() == null || !item.getProduct().isActive() || item.getProduct().getStock() < item.getQuantity()) {
                return false;
            }
        }
        return true;
    }

    public int getAvailableStock() {
        if (items == null || items.isEmpty()) return 0;
        int minStock = Integer.MAX_VALUE;
        for (ComboItem item : items) {
            if (item.getProduct() == null || !item.getProduct().isActive()) return 0;
            int available = item.getProduct().getStock() / Math.max(1, item.getQuantity());
            if (available < minStock) {
                minStock = available;
            }
        }
        return minStock == Integer.MAX_VALUE ? 0 : minStock;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public ComboTier getTier() { return tier; }
    public void setTier(ComboTier tier) { this.tier = tier; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public BigDecimal getComboPrice() { return comboPrice; }
    public void setComboPrice(BigDecimal comboPrice) { this.comboPrice = comboPrice; }

    public BigDecimal getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(BigDecimal originalPrice) { this.originalPrice = originalPrice; }

    public BigDecimal getPromoPrice() { return promoPrice; }
    public void setPromoPrice(BigDecimal promoPrice) { this.promoPrice = promoPrice; }

    public LocalDateTime getPromoStartDate() { return promoStartDate; }
    public void setPromoStartDate(LocalDateTime promoStartDate) { this.promoStartDate = promoStartDate; }

    public LocalDateTime getPromoEndDate() { return promoEndDate; }
    public void setPromoEndDate(LocalDateTime promoEndDate) { this.promoEndDate = promoEndDate; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public boolean isFeatured() { return featured; }
    public void setFeatured(boolean featured) { this.featured = featured; }

    public String getBadge() { return badge; }
    public void setBadge(String badge) { this.badge = badge; }

    public int getSalesCount() { return salesCount; }
    public void setSalesCount(int salesCount) { this.salesCount = salesCount; }

    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }

    public List<ComboItem> getItems() { return items; }
    public void setItems(List<ComboItem> items) { this.items = items; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
