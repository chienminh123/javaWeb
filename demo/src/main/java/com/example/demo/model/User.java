package com.example.demo.model;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import lombok.Data;

@Entity
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
    private float point;
    @OneToOne(mappedBy = "user")
    private Carts carts;
    @OneToMany(mappedBy = "user")
    private List<Orders> orders;
}
