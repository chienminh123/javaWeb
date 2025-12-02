package com.example.demo.model;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

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
    private String couponCode;      
    private Double discountAmount;  
    private Double quantityDiscountAmount;  
    private Double finalTotal;
    
    @OneToMany(mappedBy="orders" ,cascade = CascadeType.ALL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<OrderDetail> orderDetails;
    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "userId")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;
}
