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

    private String genre;

    @ManyToOne
    @JoinColumn(name = "providerId", referencedColumnName = "providerId") 
    private Provider provider; // Quan hệ nhiều-một với Nhà Cung Cấp
    
    private String productName;
    private String description;
    private float basisPrice;
    private float sellPrice;

    @OneToMany(mappedBy = "product")
    private List<Sizes> sizes;
    @OneToMany(mappedBy = "product")
    private List<Images> images;
}
