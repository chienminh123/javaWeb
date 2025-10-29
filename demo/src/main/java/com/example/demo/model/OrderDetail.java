package com.example.demo.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class OrderDetail {
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer orderDetailId;
    private Integer orderId;
    private Integer productId;
    private Integer sizeId;
    private Integer quantity;
    private Double price;
}
