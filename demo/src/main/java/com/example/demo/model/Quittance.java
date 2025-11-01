package com.example.demo.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;

@Entity
@Data
public class Quittance {
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer quittanceId;
    private String quittanceName;
    private LocalDateTime date;
    private String quittanceType;
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "productId", referencedColumnName = "productId") // Khóa ngoại tham chiếu Sản Phẩm
    private Product product;
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "providerId", referencedColumnName = "providerId")
    private Provider provider;
    private String note;
}
