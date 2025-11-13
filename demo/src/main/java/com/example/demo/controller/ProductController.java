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
import com.example.demo.service.GenreService;
import com.example.demo.service.ProductService;

@Controller
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private GenreService genreService; // [cite: GenreService.java]

    @GetMapping("/products") 
    public String showProductsByGenre(Model model, 
    @RequestParam(name = "genreId") Integer genreId) {
        
        // 1. Lấy sản phẩm theo thể loại
        List<Product> products = productService.findProductsByGenre(genreId); // [cite: ProductService.java, line 386]
        
        // 2. Lấy TẤT CẢ thể loại để hiển thị menu
        List<Genre> genres = genreService.findAllGenres();
        
        // 3. Lấy tên thể loại (SỬA LỖI Ở ĐÂY)
        // (Giả định bạn đã có hàm findById trả về Optional<Genre> trong GenreService)
        Genre currentGenre = genreService.getById(genreId).orElse(null); 
        String pageTitle = (currentGenre != null) ? currentGenre.getGenreName() : "Sản Phẩm";

        // 4. Gửi dữ liệu
        model.addAttribute("products", products);
        model.addAttribute("genres", genres);
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("currentGenreId", genreId); // Gửi ID để biết đang ở mục nào

        return "User/products"; // Trả về file products.html
    }

    
    @GetMapping("/product/{id}")
    public String showProductDetail(@PathVariable("id") Integer id, Model model) {
        
        // (Giả định ProductService đã có findProductDetailsById)
        // (Hoặc dùng hàm findById có sẵn của JpaRepository)
        Product product = productService.findById(id) // (Bạn cần tạo hàm này)
             .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm: " + id));

        model.addAttribute("product", product);
        return "User/product-detail"; // Trả về file HTML mới
    }
    @GetMapping("/search")
public String searchProducts(
    @RequestParam("keyword") String keyword,
    @RequestParam(name = "genreId", required = false) Integer genreId,
    Model model) {

    // 1. Lấy sản phẩm bằng hàm Service mới
    List<Product> products = productService.searchSuggestions(keyword, genreId);

    // 2. Lấy TẤT CẢ thể loại để hiển thị menu/sidebar (đã có logic này)
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