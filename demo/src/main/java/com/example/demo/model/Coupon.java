package com.example.demo.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class Coupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer couponId;

    @Column(unique = true, nullable = false)
    private String code; // Mã (VD: TET2025)
    private String discountType = "PERCENT";
    private Double discountValue;
    private Double maxDiscountAmount;
    private Integer quantity; // Số lượng mã
    private LocalDate startDate;
    private LocalDate endDate;
    
    private boolean isActive = true; // Trạng thái
}