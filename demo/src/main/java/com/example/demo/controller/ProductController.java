
package com.example.demo.controller;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.dto.TopProductDTO;
import com.example.demo.model.Genre;
import com.example.demo.model.Product;
import com.example.demo.model.Provider;
import com.example.demo.model.Review;
import com.example.demo.service.GenreService;
import com.example.demo.service.ProductService;
import com.example.demo.service.ProviderService;
import com.example.demo.service.ReportService;
import com.example.demo.service.ReviewService;

@Controller
public class ProductController {

    @Autowired
    private ProductService productService;
    @Autowired
    private ProviderService providerService;
    @Autowired
    private GenreService genreService;
    @Autowired
    private ReportService reportService;
    @Autowired
    private ReviewService reviewService;



    @GetMapping("/products") 
    public String showProductsByGenre(Model model, 
        @RequestParam(name = "genreId", required = false) Integer genreId,
        @RequestParam(name = "sort", required = false) String sort,
        @RequestParam(name = "priceRange", required = false) String priceRange, 
        @RequestParam(name = "brandId", required = false) Integer brandId,
        @RequestParam(name = "page", defaultValue = "1") int page 
    ) {
        int pageSize = 8; 

        Page<Product> productPage = productService.findProductsByGenreWithPagination(
            genreId, sort, priceRange, brandId, page, pageSize); 
        
        List<Product> products = productPage.getContent();
        
        // 2. Lấy dữ liệu bổ trợ
        List<Genre> genres = genreService.findAllGenres();
        List<Provider> providerList;
        if (genreId != null) {
            providerList = providerService.findByGenreId(genreId);
            Genre currentGenre = genreService.getById(genreId).orElse(null);
            if (currentGenre != null) {
                model.addAttribute("pageTitle", currentGenre.getGenreName());
            } else {
                model.addAttribute("pageTitle", "Sản phẩm");
            }
        } else {
            providerList = providerService.findAll(); 
        }
        
        // 3. Gửi dữ liệu vào Model
        model.addAttribute("products", products);
        model.addAttribute("genres", genres);
        model.addAttribute("providerList", providerList); 
        model.addAttribute("currentGenreId", genreId);
        
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("totalItems", productPage.getTotalElements());

        return "User/products"; 
    }
    
    @GetMapping("/product/{id}")
    public String showProductDetail(
        @PathVariable("id") Integer id, 
        @RequestParam(required = false) Integer rating, // Nhận tham số lọc sao (nếu có)
        Model model,
        Principal principal
    ) {
        Product product = productService.findById(id) 
             .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm: " + id));

        // 1. Lấy danh sách đánh giá (Có lọc theo rating nếu user chọn)
        List<Review> reviews = reviewService.getReviews(id, rating);
        List<Genre> genres = genreService.findAllGenres();
        // 2. Lấy thống kê (để vẽ biểu đồ 5 sao, 4 sao...)
        Map<String, Object> stats = reviewService.getReviewStats(id);

        // 3. Kiểm tra quyền hiển thị form (Đã đăng nhập là hiện form, Service sẽ chặn nếu chưa mua)
        boolean canReview = (principal != null);

        model.addAttribute("product", product);
        model.addAttribute("reviews", reviews);
        model.addAttribute("stats", stats);
        model.addAttribute("genres", genres);
        model.addAttribute("currentFilter", rating); // Để highlight nút lọc đang chọn
        model.addAttribute("canReview", canReview);

        return "User/product-detail"; 
    }
@GetMapping("/products/top-selling")
    public String showTopSellingProducts(Model model) {
        
        // Lấy top bán chạy trong 30 ngày qua
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);
        
        List<TopProductDTO> topSellingDTOs = reportService.getTopSellingProducts(startDate, endDate);
        
        // Chuyển đổi từ DTO sang List<Product> để tái sử dụng template
        List<Product> products = topSellingDTOs.stream()
                                .map(TopProductDTO::getProduct)
                                .collect(Collectors.toList());

        // Lấy dữ liệu phụ trợ cho Sidebar (để không bị lỗi giao diện)
        List<Genre> genres = genreService.findAllGenres();
        List<Provider> providerList = providerService.findAll(); 

        // Gửi dữ liệu sang view
        model.addAttribute("products", products);
        model.addAttribute("genres", genres);
        model.addAttribute("providerList", providerList);
        
        model.addAttribute("pageTitle", "Top 10 Sản Phẩm Bán Chạy Nhất");
        model.addAttribute("currentGenreId", null); // Không highlight menu nào

        return "User/products"; 
    }

    @PostMapping("/product/review")
    public String submitReview(
        @RequestParam Integer productId,
        @RequestParam Integer rating,
        @RequestParam String comment,
        Principal principal,
        RedirectAttributes redirectAttributes
    ) {
        try {
            if (principal == null) return "redirect:/Auth/login";
            
            // Gọi Service thêm đánh giá
            reviewService.addReview(principal.getName(), productId, rating, comment);
            
            redirectAttributes.addFlashAttribute("successMessage", "Cảm ơn bạn đã đánh giá sản phẩm!");
        } catch (Exception e) {
            // Nếu chưa mua hàng hoặc lỗi khác, thông báo lỗi
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        // Quay lại trang chi tiết sản phẩm
        return "redirect:/product/" + productId;
    }

    @GetMapping("/search")
    public String searchProducts(
        @RequestParam("keyword") String keyword,
        @RequestParam(name = "genreId", required = false) Integer genreId,
        Model model) {

        List<Product> products = productService.searchSuggestions(keyword, genreId);
        List<Genre> genres = genreService.findAllGenres();
        List<Provider> providerList = providerService.findAll();

        model.addAttribute("products", products);
        model.addAttribute("genres", genres);
        model.addAttribute("providerList", providerList);
        model.addAttribute("pageTitle", "Kết quả tìm kiếm cho: " + keyword);
        model.addAttribute("currentGenreId", genreId); 

        return "User/products"; 
    }
}