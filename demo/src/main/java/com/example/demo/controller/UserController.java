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

    @Autowired
    private ReportService reportService; 

    @GetMapping("/User/index") 
    public String home(Model model) {
        
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

        return "User/index"; 
    }
}