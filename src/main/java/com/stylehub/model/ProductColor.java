package com.stylehub.model;

import jakarta.persistence.*;

@Entity
@Table(name = "product_colors")
public class ProductColor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private String colorName; // Black, White, Navy, Burgundy, Olive

    private String hexCode; // #000000, #ffffff, etc.

    @Column(length = 1000)
    private String imageUrl;

    public ProductColor() {}

    public ProductColor(Product product, String colorName, String hexCode) {
        this.product = product;
        this.colorName = colorName;
        this.hexCode = hexCode;
    }

    public ProductColor(Product product, String colorName, String hexCode, String imageUrl) {
        this.product = product;
        this.colorName = colorName;
        this.hexCode = hexCode;
        this.imageUrl = imageUrl;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public String getColorName() { return colorName; }
    public void setColorName(String colorName) { this.colorName = colorName; }

    public String getHexCode() { return hexCode; }
    public void setHexCode(String hexCode) { this.hexCode = hexCode; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
