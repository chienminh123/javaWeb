package com.example.demo.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.Product;
import com.example.demo.service.ProductService;

@RestController
@RequestMapping("/api/products")
public class ProductApiController {

    @Autowired
    private ProductService productService;

    /**
     * API này trả về danh sách tên sản phẩm
     * để làm gợi ý tìm kiếm (autocomplete)
     */
    @GetMapping("/suggest")
public List<String> getProductSuggestions(
        @RequestParam("keyword") String keyword,
        @RequestParam(name = "genreId", required = false) Integer genreId) { 
    
    return productService.searchSuggestions(keyword, genreId) 
            .stream()
            .map(Product::getProductName) // Chỉ lấy tên
            .collect(Collectors.toList());
}
}