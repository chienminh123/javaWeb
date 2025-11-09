package com.example.demo.dto;

import lombok.Data;

@Data
public class TopProductIdDTO {
    private Integer productId;
    private Long totalQuantity;

    public TopProductIdDTO(Integer productId, Long totalQuantity) {
        this.productId = productId;
        this.totalQuantity = totalQuantity;
    }
}