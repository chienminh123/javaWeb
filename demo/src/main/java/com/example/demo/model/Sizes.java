package com.example.demo.model;
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
public class Sizes {
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer SizeId;
    
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "productId", referencedColumnName = "productId") // Khóa ngoại tham chiếu Sản Phẩm
    private Product product;
    
    private String sizeName;
    private Integer quantity;
    
    // public Integer getQuantity() { return quantity; }
    // public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
