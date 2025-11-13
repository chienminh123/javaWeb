package com.example.demo.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.demo.dto.TopProductDTO;
import com.example.demo.model.Genre;
import com.example.demo.model.Product;
import com.example.demo.service.GenreService;
import com.example.demo.service.ReportService; // Dùng service này

@Controller
public class UserController {

    @Autowired
    private GenreService genreService;

    // === DÙNG REPORTSERVICE THAY VÌ PRODUCTSERVICE ===
    @Autowired
    private ReportService reportService; 

    /**
     * Trang chủ, LUÔN HIỂN THỊ TOP BÁN CHẠY
     */
    @GetMapping("/User/index") 
    public String home(Model model) {
        
        // 1. Luôn lấy Top Bán Chạy (ví dụ 90 ngày qua)
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(90); 
        
        List<TopProductDTO> topProductDTOs = reportService.getTopSellingProducts(startDate, endDate);
        
        // Chuyển đổi từ DTO sang List<Product>
        List<Product> products = topProductDTOs.stream()
                                .map(TopProductDTO::getProduct) // [cite: TopProductDTO.java]
                                .collect(Collectors.toList());
        
        String pageTitle = "Sản phẩm Bán chạy";

        // 2. Lấy TẤT CẢ thể loại để hiển thị menu
        List<Genre> genres = genreService.findAllGenres();
        
        // 3. Gửi dữ liệu sang index.html
        model.addAttribute("products", products);
        model.addAttribute("genres", genres);
        model.addAttribute("pageTitle", pageTitle); // Gửi tiêu đề

        return "User/index"; // [cite: index.html]
    }
}