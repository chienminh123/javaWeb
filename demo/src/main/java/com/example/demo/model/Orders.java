package com.example.demo.model;

import java.time.LocalDateTime;
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
public class Orders {
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer orderId;
    private LocalDateTime orderDate;
    private String status;
    private String Address;
    private String Phone;
    private String paymentMethod;
    
    @OneToMany(mappedBy="orders")
    private List<OrderDetail> orderDetails;
    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "userId")
    private User user; // Quan hệ nhiều-một với User
}
