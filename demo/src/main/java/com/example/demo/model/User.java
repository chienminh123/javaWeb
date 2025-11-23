package com.example.demo.model;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(name = "user") 
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer userId;
    
    private String userName;
    private String passWord;
    private String role;
    private String phone;
    private String email;    
    private String address;  
    
    @Column(columnDefinition = "bigint default 0")
    private long points = 0L;
    
    private String resetToken;
    private LocalDateTime resetTokenExpiry;

    // SỬA Ở ĐÂY: Ánh xạ tên cột khác để tránh lỗi SQL
    @Column(name = "member_rank") 
    private String rank = "MEMBER";
    @Column(columnDefinition = "boolean default true")
    private boolean enabled = true; // true = hoạt động, false = bị khóa
    
    @OneToOne(mappedBy = "user")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Carts carts;
    
    @OneToMany(mappedBy = "user")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Orders> orders;
}