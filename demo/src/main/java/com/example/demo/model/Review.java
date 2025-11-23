package com.example.demo.model;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Data
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer reviewId;

    private Integer rating; // 1-5 sao
    
    @Column(length = 2000)
    private String comment;
    
    private LocalDateTime reviewDate;

    @ManyToOne
    @JoinColumn(name = "userId")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;

    @ManyToOne
    @JoinColumn(name = "productId")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Product product;
}