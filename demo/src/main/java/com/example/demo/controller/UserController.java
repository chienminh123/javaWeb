package com.example.demo.controller;

import java.security.Principal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.demo.dto.TopProductDTO;
import com.example.demo.model.Genre;
import com.example.demo.model.Product;
import com.example.demo.model.User;
import com.example.demo.service.AiService;
import com.example.demo.service.GenreService;
import com.example.demo.service.ReportService; // Dùng service này
import com.example.demo.service.UserService;

@Controller
public class UserController {

    @Autowired
    private GenreService genreService;

    @Autowired
    private ReportService reportService; 
    @Autowired
    private AiService aiService; 
    
    @Autowired
    private UserService userService;

    @GetMapping("/User/index") 
    public String home(Model model,Principal principal) {
        
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(90); 
        
        List<TopProductDTO> topProductDTOs = reportService.getTopSellingProducts(startDate, endDate);
        
        List<Product> products = topProductDTOs.stream()
                                .map(TopProductDTO::getProduct) 
                                .collect(Collectors.toList());
        
        String pageTitle = "Sản phẩm Bán chạy";

       
        List<Genre> genres = genreService.findAllGenres();
        
     
        model.addAttribute("products", products);
        model.addAttribute("genres", genres);
        model.addAttribute("pageTitle", pageTitle);
        
        List<Product> recommendations = new ArrayList<>();
        
        if (principal != null) {
            // Nếu người dùng đã đăng nhập
            String userPhone = principal.getName(); // Lấy số điện thoại (username)
            User currentUser = userService.findByPhone(userPhone); // Lấy thông tin User từ DB
            
            if (currentUser != null) {
                // Gọi sang Python để lấy danh sách gợi ý cho user này
                recommendations = aiService.getRecommendedProducts(currentUser.getUserId());
            }
        }
        
        // Đẩy danh sách gợi ý ra giao diện (dù rỗng hay có dữ liệu)
        model.addAttribute("recommendations", recommendations);
        return "User/index"; 
    }
}