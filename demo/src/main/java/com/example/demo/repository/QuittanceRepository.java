package com.example.demo.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Quittance;

@Repository
public interface QuittanceRepository extends JpaRepository<Quittance, Integer> {
    // Tìm các biên lai theo loại và khoảng thời gian
    List<Quittance> findByQuittanceTypeAndDateBetween(
            String quittanceType, 
            LocalDateTime startDate, 
            LocalDateTime endDate);
}