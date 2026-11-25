package com.stylehub.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "cart_items")
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = true)
    private Product product;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "combo_package_id", nullable = true)
    private ComboPackage comboPackage;

    private boolean isCombo = false;

    @Column(length = 1000)
    private String comboSelections; // Custom sizes & shades chosen for the bundle

    @Column(length = 500)
    private String includedProductIds; // Comma-separated product IDs included in the bundle (for dynamic items)

    private String selectedSize = "M";

    private String selectedColor = "Black";

    @Column(nullable = false)
    private int quantity = 1;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Cart getCart() { return cart; }
    public void setCart(Cart cart) { this.cart = cart; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public ComboPackage getComboPackage() { return comboPackage; }
    public void setComboPackage(ComboPackage comboPackage) { this.comboPackage = comboPackage; }

    public boolean isCombo() { return isCombo; }
    public void setCombo(boolean combo) { isCombo = combo; }

    public String getComboSelections() { return comboSelections; }
    public void setComboSelections(String comboSelections) { this.comboSelections = comboSelections; }

    public String getIncludedProductIds() { return includedProductIds; }
    public void setIncludedProductIds(String includedProductIds) { this.includedProductIds = includedProductIds; }

    public String getSelectedSize() { return selectedSize; }
    public void setSelectedSize(String selectedSize) { this.selectedSize = selectedSize; }

    public String getSelectedColor() { return selectedColor; }
    public void setSelectedColor(String selectedColor) { this.selectedColor = selectedColor; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getTotalPrice() {
        if (unitPrice == null) return BigDecimal.ZERO;
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public String getItemTitle() {
        if (isCombo && comboPackage != null) {
            return comboPackage.getName();
        }
        return product != null ? product.getName() : "Item";
    }

    public String getItemImageUrl() {
        if (isCombo && comboPackage != null) {
            return comboPackage.getImageUrl();
        }
        if (product != null) {
            if (selectedColor != null && product.getColors() != null) {
                for (ProductColor c : product.getColors()) {
                    if (c.getColorName() != null && c.getColorName().equalsIgnoreCase(selectedColor)
                            && c.getImageUrl() != null && !c.getImageUrl().isBlank()) {
                        return c.getImageUrl();
                    }
                }
            }
            return product.getImageUrl();
        }
        return "/images/placeholder.jpg";
    }
}
