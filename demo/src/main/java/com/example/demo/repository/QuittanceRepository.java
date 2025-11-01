package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Quittance;

@Repository
public interface QuittanceRepository extends JpaRepository<Quittance, Integer> {}