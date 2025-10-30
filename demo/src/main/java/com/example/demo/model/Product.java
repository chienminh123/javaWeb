package com.example.demo.model;

import java.util.List;

import jakarta.persistence.Entity;
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
    private String description;
    private float basisPrice;
    private float sellPrice;
    private String imageUrl;
    @OneToMany(mappedBy = "product")
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
}
