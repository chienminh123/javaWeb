package com.example.demo.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
@Entity
@Data
public class CartDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer cartDetailId;
    
    @ManyToOne
    @JoinColumn(name = "cartId", referencedColumnName = "cartId") 
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Carts carts; // Quan hệ nhiều-một với Cart
    @ManyToOne
    @JoinColumn(name = "productId", referencedColumnName = "productId")
    private Product product; // Thay vì Integer productId

    @ManyToOne
    @JoinColumn(name = "sizeId", referencedColumnName = "sizeId")
    private Sizes sizes; // Thay vì Integer sizeId

    private Integer quantity;
    private float price; // (Lưu giá tại thời điểm thêm vào)
}