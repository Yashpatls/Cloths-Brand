package com.stylehub.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "combo_items")
public class ComboItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "combo_package_id", nullable = false)
    private ComboPackage comboPackage;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    private String customTitle; // e.g. "1 Shirt", "1 Jeans", "1 Belt"

    @Column(nullable = false)
    private int quantity = 1;

    @Column(precision = 10, scale = 2)
    private BigDecimal itemPrice; // Constituent bundle price of this item

    private String defaultSize = "M";

    private String defaultColor = "Default";

    private String availableSizes; // Comma-separated: "S,M,L,XL" or null to inherit product sizes

    private String availableColors; // Comma-separated: "Navy,Black,White" or null to inherit product colors

    private int displayOrder = 0;

    public ComboItem() {}

    public ComboItem(Product product, String customTitle, int quantity, String defaultSize, String defaultColor, int displayOrder) {
        this.product = product;
        this.customTitle = customTitle;
        this.quantity = quantity;
        this.defaultSize = defaultSize;
        this.defaultColor = defaultColor;
        this.displayOrder = displayOrder;
    }

    public ComboItem(Product product, String customTitle, int quantity, BigDecimal itemPrice, String defaultSize, String defaultColor, int displayOrder) {
        this.product = product;
        this.customTitle = customTitle;
        this.quantity = quantity;
        this.itemPrice = itemPrice;
        this.defaultSize = defaultSize;
        this.defaultColor = defaultColor;
        this.displayOrder = displayOrder;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ComboPackage getComboPackage() { return comboPackage; }
    public void setComboPackage(ComboPackage comboPackage) { this.comboPackage = comboPackage; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public String getCustomTitle() {
        if (customTitle != null && !customTitle.isBlank()) return customTitle;
        return product != null ? product.getName() : "Garment";
    }
    public void setCustomTitle(String customTitle) { this.customTitle = customTitle; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public BigDecimal getItemPrice() {
        if (itemPrice != null && itemPrice.compareTo(BigDecimal.ZERO) > 0) {
            return itemPrice;
        }
        if (product != null && product.getPrice() != null && comboPackage != null) {
            BigDecimal orig = comboPackage.getOriginalPrice();
            BigDecimal eff = comboPackage.getEffectivePrice();
            if (orig != null && orig.compareTo(BigDecimal.ZERO) > 0 && eff != null) {
                return product.getPrice().multiply(eff).divide(orig, 2, RoundingMode.HALF_UP);
            }
            return product.getPrice();
        }
        return BigDecimal.ZERO;
    }
    public void setItemPrice(BigDecimal itemPrice) { this.itemPrice = itemPrice; }

    public BigDecimal getOriginalPrice() {
        if (product != null && product.getPrice() != null) {
            return product.getPrice().multiply(BigDecimal.valueOf(Math.max(1, quantity)));
        }
        return getItemPrice();
    }

    public String getDefaultSize() { return defaultSize; }
    public void setDefaultSize(String defaultSize) { this.defaultSize = defaultSize; }

    public String getDefaultColor() { return defaultColor; }
    public void setDefaultColor(String defaultColor) { this.defaultColor = defaultColor; }

    public String getAvailableSizes() { return availableSizes; }
    public void setAvailableSizes(String availableSizes) { this.availableSizes = availableSizes; }

    public String getAvailableColors() { return availableColors; }
    public void setAvailableColors(String availableColors) { this.availableColors = availableColors; }

    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
}
