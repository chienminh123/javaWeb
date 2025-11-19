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
    private Genre genre; // Quan hệ nhiều-một với Thể Loại

    @ManyToOne
    @JoinColumn(name = "providerId", referencedColumnName = "providerId") 
    private Provider provider; // Quan hệ nhiều-một với Nhà Cung Cấp
    
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
    // Getter & Setter
    public Genre getGenre() { return genre; }
    public void setGenre(Genre genre) { this.genre = genre; }

    // Để hiển thị tên thể loại
    public String getGenreName() {
        return genre != null ? genre.getGenreName() : "";
    }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    
    public double getDiscountedPrice() {
        if (discount == null || discount == 0) {
            return this.sellPrice;
        }
        return this.sellPrice * (1 - (discount / 100.0));
    }
}
