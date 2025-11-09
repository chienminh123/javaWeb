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
    private ProductRepository productRepo; // Giả định bạn đã có từ các bước trước

    // // Lấy dữ liệu cho biểu đồ Doanh thu
    public List<RevenueByDateDTO> getRevenueReport(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        
        // 1. Lấy về List<Object[]>
        List<Object[]> results = orderDetailRepo.findRevenueByDateRange(startDateTime, endDateTime);
        
        // 2. Chuyển đổi thủ công từ Object[] sang DTO
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

    // Lấy Top 10 sản phẩm bán chạy
    public List<TopProductDTO> getTopSellingProducts(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        
        // 1. Lấy danh sách ID và số lượng từ query
        List<TopProductIdDTO> topIds = orderDetailRepo.findTopSellingProducts(
                startDateTime, endDateTime, PageRequest.of(0, 10));
        // 2. Chuyển đổi ID thành đối tượng Product (để hiển thị tên, ảnh, v.v.)
        return topIds.stream().map(dto -> {
            Product product = productRepo.findById(dto.getProductId())
                .orElse(new Product()); // Tạo 1 Product tạm nếu bị xóa
            return new TopProductDTO(product, dto.getTotalQuantity());
        }).collect(Collectors.toList());
    }

    // Lấy các biên lai Nhập/Xuất
    public List<Quittance> getQuittances(String type, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        return quittanceRepo.findByQuittanceTypeAndDateBetween(type, startDateTime, endDateTime);
    }
}