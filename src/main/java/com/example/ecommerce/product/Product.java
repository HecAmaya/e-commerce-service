package com.example.ecommerce.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.SQLRestriction;
import java.math.BigDecimal;

@Entity
@Table(name = "products", indexes = {
        @jakarta.persistence.Index(name = "idx_products_category", columnList = "category"),
        @jakarta.persistence.Index(name = "idx_products_price", columnList = "price"),
        @jakarta.persistence.Index(name = "idx_products_name", columnList = "name"),
        @jakarta.persistence.Index(name = "idx_products_sku", columnList = "sku")
})
@SQLRestriction("active = true")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String sku;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer stock;

    @Column(name = "weight_kg", nullable = false, precision = 8, scale = 3)
    private BigDecimal weightKg;

    @Version
    private Long version;

    @Column(nullable = false)
    private boolean active = true;

    protected Product() {
    }

    public Product(String name, String sku, String description, String category, BigDecimal price, Integer stock, BigDecimal weightKg) {
        this.name = name;
        this.sku = sku;
        this.description = description;
        this.category = category;
        this.price = price;
        this.stock = stock;
        this.weightKg = weightKg;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getSku() { return sku; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public BigDecimal getPrice() { return price; }
    public Integer getStock() { return stock; }
    public BigDecimal getWeightKg() { return weightKg; }
    public boolean isActive() { return active; }
    public void setName(String name) { this.name = name; }
    public void setSku(String sku) { this.sku = sku; }
    public void setDescription(String description) { this.description = description; }
    public void setCategory(String category) { this.category = category; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public void setStock(Integer stock) { this.stock = stock; }
    public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }
    public void setActive(boolean active) { this.active = active; }
}
