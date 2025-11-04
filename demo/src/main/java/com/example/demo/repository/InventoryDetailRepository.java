package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.InventoryDetail;

public interface InventoryDetailRepository extends JpaRepository<InventoryDetail, Integer> {}
