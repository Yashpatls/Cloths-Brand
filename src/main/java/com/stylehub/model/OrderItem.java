package com.stylehub.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private CustomerOrder order;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = true)
    private Product product;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "combo_package_id", nullable = true)
    private ComboPackage comboPackage;

    private boolean isCombo = false;

    @Column(length = 1000)
    private String comboSelections;

    @Column(length = 500)
    private String includedProductIds;

    private String productName;

    private String productSku;

    private String productImage;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private int quantity = 1;

    private String selectedSize = "M";

    private String selectedColor = "Black";

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public CustomerOrder getOrder() { return order; }
    public void setOrder(CustomerOrder order) { this.order = order; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) {
        this.product = product;
        if (product != null) {
            this.productName = product.getName();
            this.productSku = product.getSku();
            this.productImage = product.getImageUrl();
        }
    }

    public ComboPackage getComboPackage() { return comboPackage; }
    public void setComboPackage(ComboPackage comboPackage) {
        this.comboPackage = comboPackage;
        if (comboPackage != null) {
            this.isCombo = true;
            this.productName = comboPackage.getName();
            this.productSku = "COMBO-" + comboPackage.getSlug().toUpperCase();
            this.productImage = comboPackage.getImageUrl();
        }
    }

    public boolean isCombo() { return isCombo; }
    public void setCombo(boolean combo) { isCombo = combo; }

    public String getComboSelections() { return comboSelections; }
    public void setComboSelections(String comboSelections) { this.comboSelections = comboSelections; }

    public String getIncludedProductIds() { return includedProductIds; }
    public void setIncludedProductIds(String includedProductIds) { this.includedProductIds = includedProductIds; }

    public String getProductName() {
        if (productName != null) return productName;
        if (isCombo && comboPackage != null) return comboPackage.getName();
        return product != null ? product.getName() : "Item";
    }
    public void setProductName(String productName) { this.productName = productName; }

    public String getProductSku() { return productSku; }
    public void setProductSku(String productSku) { this.productSku = productSku; }

    public String getProductImage() { return productImage; }
    public void setProductImage(String productImage) { this.productImage = productImage; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getSelectedSize() { return selectedSize; }
    public void setSelectedSize(String selectedSize) { this.selectedSize = selectedSize; }

    public String getSelectedColor() { return selectedColor; }
    public void setSelectedColor(String selectedColor) { this.selectedColor = selectedColor; }

    public BigDecimal getTotalPrice() {
        if (unitPrice == null) return BigDecimal.ZERO;
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
