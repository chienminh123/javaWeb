package com.example.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.CartDetail;
import com.example.demo.model.Carts;
import com.example.demo.model.Product;
import com.example.demo.model.Sizes;

@Repository
public interface CartDetailRepository extends JpaRepository<CartDetail, Integer> {
    // Tìm một món hàng cụ thể (cùng 1 size, 1 sản phẩm) trong 1 giỏ hàng
    Optional<CartDetail> findByCartsAndProductAndSizes(Carts cart, Product product, Sizes size);
}