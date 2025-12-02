package com.example.demo.model;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class AutoStockDiscountConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer configId;
    
    private Integer minStockQuantity;
    private Integer discountPercent;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean active;
    
    public AutoStockDiscountConfig() {
        this.minStockQuantity = 20;
        this.discountPercent = 10;
        this.active = true;
    }
    
    public boolean isActiveNow() {
        if (active == null || !active) {
            return false;
        }
        LocalDate now = LocalDate.now();
        return (startDate == null || !now.isBefore(startDate)) &&
               (endDate == null || !now.isAfter(endDate));
    }
}

