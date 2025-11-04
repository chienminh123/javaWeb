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
public class InventoryDetail {
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer inventoryDetailId;
    @ManyToOne
    @JoinColumn(name = "inventoryCheckId", referencedColumnName = "inventoryCheckId")
    private InventoryCheck inventoryCheck;
    @ManyToOne
    @JoinColumn(name = "productId", referencedColumnName = "productId")
    private Product product;
    @ManyToOne
    @JoinColumn(name = "sizeId", referencedColumnName = "sizeId")
    private Sizes size;
    private Integer systemQuantity;
    private Integer actualQuantity;
    private Integer difference;
    private String note;
}
