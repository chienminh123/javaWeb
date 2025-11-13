package com.example.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Carts;
import com.example.demo.model.User;

@Repository
public interface CartRepository extends JpaRepository<Carts, Integer> {
    // Tìm giỏ hàng của một user
    Optional<Carts> findByUser(User user);
}