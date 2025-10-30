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
public class Sizes {
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer SizeId;

    @ManyToOne
    @JoinColumn(name = "productId", referencedColumnName = "productId") // Khóa ngoại tham chiếu Sản Phẩm
    private Product product;
    
    private String sizeName;
    private Integer qantity;

}
