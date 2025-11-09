package com.example.demo.dto;

import com.example.demo.model.Product;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TopProductDTO {
    private Product product;
    private Long totalQuantity;
}