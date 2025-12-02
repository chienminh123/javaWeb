package com.example.demo.model;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Data;

@Entity
@Data
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer productId;

    @ManyToOne
    @JoinColumn(name = "genreId", referencedColumnName = "genreId")
    private Genre genre;

    @ManyToOne
    @JoinColumn(name = "providerId", referencedColumnName = "providerId") 
    private Provider provider;
    
    private String productName;
    @Column(length = 10000)
    private String description;
    private float basisPrice;
    private float sellPrice;
    private String imageUrl;
    private Integer discount = 0;
    private Float markupPercent = 0f;
    @OneToMany(mappedBy = "product", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private List<Sizes> sizes;
    
    public Genre getGenre() { return genre; }
    public void setGenre(Genre genre) { this.genre = genre; }

    public String getGenreName() {
        return genre != null ? genre.getGenreName() : "";
    }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    
    public int getTotalStockQuantity() {
        if (sizes == null || sizes.isEmpty()) {
            return 0;
        }
        return sizes.stream()
            .mapToInt(s -> s.getQuantity() != null ? s.getQuantity() : 0)
            .sum();
    }
    
    public boolean hasAutoStockDiscount(Integer minStockQuantity) {
        if (minStockQuantity == null) {
            return getTotalStockQuantity() > 20;
        }
        return getTotalStockQuantity() > minStockQuantity;
    }
    
    public int getAutoStockDiscountPercent(Integer minStockQuantity, Integer discountPercent) {
        if (hasAutoStockDiscount(minStockQuantity)) {
            return discountPercent != null ? discountPercent : 10;
        }
        return 0;
    }
    
    public int getAutoStockDiscountPercent() {
        if (currentConfig != null && currentConfig.isActiveNow()) {
            return getAutoStockDiscountPercent(currentConfig.getMinStockQuantity(), currentConfig.getDiscountPercent());
        }
        return hasAutoStockDiscount() ? 10 : 0;
    }
    
    public int getTotalDiscountPercent(Integer minStockQuantity, Integer autoDiscountPercent) {
        int manualDiscount = (discount != null ? discount : 0);
        int autoDiscount = getAutoStockDiscountPercent(minStockQuantity, autoDiscountPercent);
        return Math.min(manualDiscount + autoDiscount, 50);
    }
    
    public double getDiscountedPrice(Integer minStockQuantity, Integer autoDiscountPercent) {
        int totalDiscount = getTotalDiscountPercent(minStockQuantity, autoDiscountPercent);
        if (totalDiscount == 0) {
            return this.sellPrice;
        }
        return this.sellPrice * (1 - (totalDiscount / 100.0));
    }
    
    private static AutoStockDiscountConfig currentConfig = null;
    
    public static void setCurrentConfig(AutoStockDiscountConfig config) {
        currentConfig = config;
    }
    
    public static AutoStockDiscountConfig getCurrentConfig() {
        return currentConfig;
    }
    
    public int getTotalDiscountPercent() {
        if (currentConfig != null && currentConfig.isActiveNow()) {
            return getTotalDiscountPercent(currentConfig.getMinStockQuantity(), currentConfig.getDiscountPercent());
        }
        int manualDiscount = (discount != null ? discount : 0);
        int autoDiscount = getAutoStockDiscountPercent();
        return Math.min(manualDiscount + autoDiscount, 50);
    }
    
    public double getDiscountedPrice() {
        if (currentConfig != null && currentConfig.isActiveNow()) {
            return getDiscountedPrice(currentConfig.getMinStockQuantity(), currentConfig.getDiscountPercent());
        }
        int totalDiscount = getTotalDiscountPercent();
        if (totalDiscount == 0) {
            return this.sellPrice;
        }
        return this.sellPrice * (1 - (totalDiscount / 100.0));
    }
    
    public boolean hasAutoStockDiscount() {
        if (currentConfig != null && currentConfig.isActiveNow()) {
            return hasAutoStockDiscount(currentConfig.getMinStockQuantity());
        }
        return getTotalStockQuantity() > 20;
    }
}
