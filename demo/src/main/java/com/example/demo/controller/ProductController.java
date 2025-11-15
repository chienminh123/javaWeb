package com.example.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.model.Genre;
import com.example.demo.model.Product;
import com.example.demo.model.Provider;
import com.example.demo.service.GenreService;
import com.example.demo.service.ProductService;
import com.example.demo.service.ProviderService;

@Controller
public class ProductController {

    @Autowired
    private ProductService productService;
    @Autowired
    private ProviderService providerService;

    @Autowired
    private GenreService genreService;

    @GetMapping("/products") 
    public String showProductsByGenre(Model model, 
        @RequestParam(name = "genreId") Integer genreId,
        @RequestParam(name = "sort", required = false) String sort,
        @RequestParam(name = "priceRange", required = false) String priceRange, 
        @RequestParam(name = "brandId", required = false) Integer brandId 
    ) {
        
        // 1. Gọi Service với các tham số mới
        List<Product> products = productService.findProductsByGenre(
            genreId, sort, priceRange, brandId); 
        
        // 2. Lấy TẤT CẢ Thể loại và NCC
        List<Genre> genres = genreService.findAllGenres();
        List<Provider> providerList = providerService.findAll(); 
        
        // 3. Gửi dữ liệu vào Model
        model.addAttribute("products", products);
        model.addAttribute("genres", genres);
        model.addAttribute("providerList", providerList); // [GỬI LIST PROVIDER CHO BỘ LỌC]
        model.addAttribute("currentGenreId", genreId); // Giữ lại genreId để header hoạt động đúng

        return "User/products"; 
    }

    
    @GetMapping("/product/{id}")
    public String showProductDetail(@PathVariable("id") Integer id, Model model) {
        
        Product product = productService.findById(id) 
             .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm: " + id));

        model.addAttribute("product", product);
        return "User/product-detail"; 
    }
    @GetMapping("/search")
public String searchProducts(
    @RequestParam("keyword") String keyword,
    @RequestParam(name = "genreId", required = false) Integer genreId,
    Model model) {

    List<Product> products = productService.searchSuggestions(keyword, genreId);
    List<Genre> genres = genreService.findAllGenres();

    // 3. Gửi dữ liệu tới view products.html
    model.addAttribute("products", products);
    model.addAttribute("genres", genres);
    model.addAttribute("pageTitle", "Kết quả tìm kiếm cho: " + keyword);
    model.addAttribute("currentGenreId", genreId); // Giữ lại genreId để header hoạt động đúng

    // Dùng lại view hiển thị danh sách sản phẩm
    return "User/products"; //
}
}