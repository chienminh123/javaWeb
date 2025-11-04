package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.InventoryCheck;

public interface InventoryCheckRepository extends JpaRepository<InventoryCheck, Integer> {}