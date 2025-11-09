package com.example.demo.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor 
@AllArgsConstructor
public class RevenueByDateDTO {
    private LocalDate date;
    private Double revenue;

    // public RevenueByDateDTO(Date sqlDate, Double revenue) {
    //     this.date = sqlDate != null ? sqlDate.toLocalDate() : null;
    //     this.revenue = revenue;
    // }

    
}