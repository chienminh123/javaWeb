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
public class OrderDetail {
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer orderDetailId;
    @ManyToOne
    @JoinColumn(name = "orderId", referencedColumnName = "orderId") 
    private Orders orders; // Quan hệ nhiều-một với Đơn hàng
    private Integer productId;
    private Integer sizeId;
    private Integer quantity;
    private Double price;
}
