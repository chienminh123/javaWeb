package com.example.demo.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;


@Entity
@Data
public class Provider {
@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int providerId;
    private String providerName;
    private String providerEmail;
    private String providerPhone;
    private String providerAddress;
    
}
