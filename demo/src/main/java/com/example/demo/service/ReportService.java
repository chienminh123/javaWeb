package com.example.demo.service;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.example.demo.dto.RevenueByDateDTO;
import com.example.demo.dto.TopProductDTO;
import com.example.demo.dto.TopProductIdDTO;
import com.example.demo.model.Product;
import com.example.demo.model.Quittance;
import com.example.demo.repository.OrderDetailRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.QuittanceRepository;

@Service
public class ReportService {

    @Autowired
    private OrderDetailRepository orderDetailRepo;

    @Autowired
    private QuittanceRepository quittanceRepo;
    
    @Autowired
    private ProductRepository productRepo; 

    public List<RevenueByDateDTO> getRevenueReport(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        
        List<Object[]> results = orderDetailRepo.findRevenueByDateRange(startDateTime, endDateTime);
        
        List<RevenueByDateDTO> dtoList = new ArrayList<>();
        for (Object[] row : results) {
            Date sqlDate = (Date) row[0];
            Double revenue = (Double) row[1];
            
            RevenueByDateDTO dto = new RevenueByDateDTO();
            dto.setDate(sqlDate != null ? sqlDate.toLocalDate() : null);
            dto.setRevenue(revenue);
            dtoList.add(dto);
        }
        
        return dtoList;
    }

    public List<TopProductDTO> getTopSellingProducts(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        
        List<TopProductIdDTO> topIds = orderDetailRepo.findTopSellingProducts(
                startDateTime, endDateTime, PageRequest.of(0, 10));
   
        return topIds.stream().map(dto -> {
            Product product = productRepo.findById(dto.getProductId())
                .orElse(new Product());
            return new TopProductDTO(product, dto.getTotalQuantity());
        }).collect(Collectors.toList());
    }

    public List<Quittance> getQuittances(String type, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        return quittanceRepo.findByQuittanceTypeAndDateBetween(type, startDateTime, endDateTime);
    }
}