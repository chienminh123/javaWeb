package com.example.demo.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;

@Entity
@Data
public class CartDetail {
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer cartDetailId;
    @ManyToOne
    @JoinColumn(name = "cartId", referencedColumnName = "cartId") 
    private Carts carts; // Quan hệ nhiều-một vớiCart
    private Integer productId;
    private Integer sizeId;
    private Integer quantity;
    private Double price;

}
