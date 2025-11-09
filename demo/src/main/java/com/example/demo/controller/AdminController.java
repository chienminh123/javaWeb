package com.example.demo.controller;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.TopProductDTO;
import com.example.demo.model.Genre;
import com.example.demo.model.Provider;
import com.example.demo.service.GenreService;
import com.example.demo.service.ProductService;
import com.example.demo.service.ProviderService;
import com.example.demo.service.ReportService;

@Controller
@RequestMapping("/Admin")
public class AdminController {

    @Autowired
    private ProviderService providerService;

    @Autowired
    private GenreService genreService;

    @Autowired
    private ProductService productService;

    @Autowired
    private ReportService reportService;



    // === CÁC MAPPING ===
    @GetMapping("/home")
    public String home(Model model) {
        double totalInventoryValue = productService.calculateTotalInventoryValue();
        long outOfStockCount = productService.countOutOfStockProducts();

        // 2. Đưa dữ liệu vào Model
        model.addAttribute("totalInventoryValue", totalInventoryValue);
        model.addAttribute("outOfStockCount", outOfStockCount);
        
        return "Admin/home";
    }

    @GetMapping("/addproduct")
    public String addProduct(Model model) {
        model.addAttribute("providers", providerService.findAll());
        model.addAttribute("genres", genreService.findAllGenres()); // List<Genre>
        model.addAttribute("productSuggestions", productService.getAllProductSuggestionsMap());
        return "Admin/addproduct";
    }

    @PostMapping("/saveMultipleProducts")
    public String saveMultipleProducts(
        @RequestParam String[] productName,
        @RequestParam Integer[] providerId,
        @RequestParam Integer[] genreId,
        @RequestParam Float[] basisPrice,
        @RequestParam String[] description,
        @RequestParam(required = false) MultipartFile[][] images,
       
        @RequestParam(required = false) String[][] sizeName,
        @RequestParam(required = false) Integer[][] quantity
        
    ) {
        productService.saveMultipleProducts(
            productName, providerId, genreId, basisPrice, description, images,
            sizeName, quantity
        );
        return "redirect:/Admin/tonkho";
    }

    @PostMapping("/addProvider")
    @ResponseBody
    @Transactional  // Đảm bảo lưu DB
    public Provider addProvider(@RequestBody Provider provider) {
        if (provider.getProviderName() == null || provider.getProviderName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên nhà cung cấp không được để trống");
        }
        return providerService.save(provider); // Trả về đối tượng có ID
    }

    @PostMapping("/addGenre")
    @ResponseBody
    @Transactional  
    public Genre addGenre(@RequestBody Map<String, String> body) {
        String genreName = body.get("genre");
        return genreService.saveGenre(genreName);
    }

    @GetMapping("/fixproduct")
    public String fixProduct(Model model) {
        // === SỬA HÀM NÀY ===
        // Nạp tất cả sản phẩm, NCC, và Thể loại
        model.addAttribute("products", productService.getAllProductsWithInventory());
        model.addAttribute("providers", providerService.findAll());
        model.addAttribute("genres", genreService.findAllGenres());
        return "Admin/fixproduct"; // Trả về file HTML mới
    }
    
    @PostMapping("/updateSingleProduct")
    @ResponseBody // Rất quan trọng, để trả về JSON
    public Map<String, Object> updateSingleProduct(
            @RequestParam("productId") Integer productId,
            @RequestParam("providerId") Integer providerId,
            @RequestParam("genreId") Integer genreId,
            @RequestParam("productName") String productName,
            @RequestParam("basisPrice") Float basisPrice,
            @RequestParam(value = "markupPercent", required = false) Float markupPercent,
            @RequestParam("description") String description,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile
    ) {
        try {
            // Gọi Service để xử lý
            String newImageUrl = productService.updateSingleProduct( // Gọi hàm service MỚI
                productId, providerId, genreId, productName,
                basisPrice, markupPercent, description, imageFile
            );

            // Trả về kết quả thành công
            return Map.of(
                "success", true,
                "message", "Cập nhật thành công!",
                "newImageUrl", (newImageUrl != null ? newImageUrl : "")
            );
        } catch (Exception e) {
            // Trả về lỗi
            return Map.of(
                "success", false,
                "message", e.getMessage() // Gửi thông báo lỗi về cho alert()
            );
        }
    }
    


    
    @GetMapping("/report")
    public String reports(Model model,
            // Nhận 2 tham số ngày tháng, nếu không có thì dùng 30 ngày qua
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        
        // === LOGIC MỚI CHO BÁO CÁO ===
        
        // 1. Đặt ngày mặc định (nếu user không chọn)
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        if (startDate == null) {
            startDate = endDate.minusDays(30); // 30 ngày gần nhất
        }
        
        List<com.example.demo.dto.RevenueByDateDTO> revenueData = reportService.getRevenueReport(startDate, endDate);
        List<TopProductDTO> topProducts = reportService.getTopSellingProducts(startDate, endDate);
        List<com.example.demo.model.Quittance> importQuittances = reportService.getQuittances("IMPORT", startDate, endDate);
        List<com.example.demo.model.Quittance> exportQuittances = reportService.getQuittances("EXPORT", startDate, endDate);

        model.addAttribute("revenueData", revenueData);
        model.addAttribute("topProducts", topProducts);
        model.addAttribute("importQuittances", importQuittances);
        model.addAttribute("exportQuittances", exportQuittances);

        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        
        return "Admin/report";
    }

    @GetMapping("/tonkho")
    public String tonkho(Model model) {
        model.addAttribute("products", productService.getAllProductsWithInventory());
        model.addAttribute("providers", providerService.findAll());
        model.addAttribute("genres", genreService.findAllGenres());
    return "Admin/tonkho";
    }
    @GetMapping("/export")
    public String export(Model model) {
        // Tái sử dụng các service để nạp dữ liệu cho form
        model.addAttribute("providers", providerService.findAll());
        model.addAttribute("productSuggestions", productService.getAllProductSuggestionsMap());
        return "Admin/export"; // Trả về trang export.html mới
    }
    @PostMapping("/processExport")
    public String processExport(
        @RequestParam(required = false) Integer[] providerId,
        @RequestParam(required = false) Integer[] productId,
        @RequestParam(required = false) String[][] sizeName,
        @RequestParam(required = false) Integer[][] quantity,
        Model model
    ) {
        // Kiểm tra xem người dùng có nhập gì không
        if (providerId == null || productId == null) {
            model.addAttribute("errorMessage", "Bạn chưa thêm sản phẩm nào để xuất kho.");
            // Nạp lại dữ liệu cho form
            model.addAttribute("providers", providerService.findAll());
            model.addAttribute("productSuggestions", productService.getAllProductSuggestionsMap());
            return "Admin/export";
        }

        try {
            // Gọi service mới để xử lý logic
            productService.exportMultipleProducts(providerId, productId, sizeName, quantity);
            
            // Nếu thành công, quay về trang tồn kho
            return "redirect:/Admin/tonkho"; 

        } catch (IllegalArgumentException | IllegalStateException e) {
            // Nếu có lỗi (ví dụ: không đủ hàng), quay lại trang
            // export và hiển thị thông báo lỗi
            model.addAttribute("errorMessage", e.getMessage());
            
            // Nạp lại dữ liệu cho form
            model.addAttribute("providers", providerService.findAll());
            model.addAttribute("productSuggestions", productService.getAllProductSuggestionsMap());
            return "Admin/export";
        }
    }
    
    @GetMapping("/inventory")
    public String inventory(Model model) {
        // 1. Lấy tất cả sản phẩm và size
        model.addAttribute("productsWithSizes", productService.getAllProductsWithInventory());
        return "Admin/inventory";
    }
    @PostMapping("/saveInventoryCheck")
    public String saveInventoryCheck(
        // Các mảng dữ liệu từ form
        @RequestParam Integer[] productId,
        @RequestParam String[] sizeName,
        @RequestParam Integer[] systemQty,
        @RequestParam Integer[] actualQty,
        @RequestParam String[] note
        
    ) {
        // 2. Gọi service để lưu
        productService.saveInventoryCheck(
            
            productId,
            sizeName,
            systemQty,
            actualQty,
            note
        );
        
        // 3. Xong thì quay về trang tồn kho
        return "redirect:/Admin/tonkho";
    }

    @ModelAttribute
    public void addGlobalAttributes(Model model, Principal principal) {
        if (principal != null) {
            // principal.getName() chính là SĐT (vì bạn đăng nhập bằng SĐT)
            model.addAttribute("currentUserPhone", principal.getName());
        }
    }
   
}