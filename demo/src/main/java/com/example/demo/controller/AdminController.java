package com.example.demo.controller;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.dto.TopProductDTO;
import com.example.demo.model.Coupon;
import com.example.demo.model.Genre;
import com.example.demo.model.Orders;
import com.example.demo.model.Provider;
import com.example.demo.service.CouponService;
import com.example.demo.service.EmailService;
import com.example.demo.service.ExcelExportService;
import com.example.demo.service.GenreService;
import com.example.demo.service.OrderService;
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
    @Autowired
    private OrderService orderService;
    @Autowired
    private EmailService emailService;
    @Autowired
    private ExcelExportService excelExportService;
    @Autowired private CouponService couponService;

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
        model.addAttribute("coupons", couponService.findAll());
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
            @RequestParam(value = "discount", required = false) Integer discount,
            @RequestParam(value = "markupPercent", required = false) Float markupPercent,
            @RequestParam("description") String description,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile
    ) {
        try {
            // Gọi Service để xử lý
            String newImageUrl = productService.updateSingleProduct( 
                productId, providerId, genreId, productName,
                basisPrice, markupPercent, discount,description, imageFile
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

   
   
    @GetMapping("/home")
    public String home(Model model) {
        double totalInventoryValue = productService.calculateTotalInventoryValue();
        long outOfStockCount = productService.countOutOfStockProducts();

        long adminCancelledCount = orderService.countOrdersByStatus("Đã hủy");
        // 2. Đếm số đơn User Trả
        long userReturnedCount = orderService.countOrdersByStatus("Đã trả hàng");
        
        // 3. Gộp tổng số lượng
        long totalCancelledAndReturned = adminCancelledCount + userReturnedCount;
        long pendingOrdersCount = orderService.countOrdersByStatus("Đang xử lý"); // Hoặc "Chờ thanh toán" tùy vào logic bạn muốn
        
        // 2. Đưa dữ liệu vào Model
        model.addAttribute("totalInventoryValue", totalInventoryValue);
        model.addAttribute("outOfStockCount", outOfStockCount);
        
        // === CODE MỚI: Đưa dữ liệu đếm vào Model ===
        model.addAttribute("cancelledOrdersCount", totalCancelledAndReturned);
        model.addAttribute("pendingOrdersCount", pendingOrdersCount);
        
        return "Admin/home";
    }

    @GetMapping("/order-detail")
public String showOrderDetail(@RequestParam("orderId") Integer orderId, Model model) {
    // 1. Lấy đơn hàng theo ID
    Orders order = orderService.findOrderById(orderId)
        .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng ID: " + orderId));

    // 2. Định nghĩa danh sách các trạng thái có thể có
    // Đây là danh sách các trạng thái cố định (bạn có thể tùy chỉnh)
    List<String> statusList = List.of(
        "Chờ thanh toán",
        "Đang xử lý",
        "Đã xác nhận",
        "Đang giao hàng",
        "Đã giao hàng",
        "Đã hủy"
    );

    // 3. Truyền dữ liệu sang view
    model.addAttribute("order", order);
    model.addAttribute("statusList", statusList);

    return "Admin/order-detail";
}

    // === MAPPING MỚI: Xử lý cập nhật trạng thái đơn hàng (POST) ===
    @PostMapping("/updateOrderStatus")
    public String updateOrderStatus(
            @RequestParam Integer orderId,
            @RequestParam String newStatus,
            RedirectAttributes redirectAttributes) {

        try {
            Orders updatedOrder = orderService.updateOrderStatus(orderId, newStatus);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Cập nhật trạng thái đơn hàng #" + orderId + " thành công sang: " + updatedOrder.getStatus());
                try {
                // newStatus sẽ được truyền vào template
                emailService.sendOrderStatusUpdate(updatedOrder, newStatus); 
            } catch (Exception e) {
                // Log lỗi gửi email nhưng vẫn tiếp tục
                System.err.println("LỖI GỬI EMAIL CẬP NHẬT TRẠNG THÁI: " + e.getMessage());
            }
        } catch (RuntimeException e) {
            // Xử lý lỗi (ví dụ: không tìm thấy đơn hàng)
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        
        // Chuyển hướng về trang chi tiết đơn hàng để thấy kết quả
        return "redirect:/Admin/order-detail?orderId=" + orderId;
    }
    
    // Đảm bảo hàm showOrdersByStatus đã có (đã tạo ở bước trước)
    // Nếu bạn muốn hiển thị tất cả đơn hàng cho /Admin/orders, bạn cần dùng hàm sau:
    @GetMapping("/orders")
    public String showOrdersByStatus(@RequestParam(required = false) String status, Model model) {
        String title = "Danh sách Đơn hàng";
        List<Orders> orders;
        
        if ("Đã hủy".equals(status)) {
        // Nếu admin bấm xem "Đã hủy", gộp cả 2 danh sách
        List<Orders> cancelled = orderService.findOrdersByStatus("Đã hủy");
        List<Orders> returned = orderService.findOrdersByStatus("Đã trả hàng");
        
        // Gộp 2 danh sách
        orders = new java.util.ArrayList<>(cancelled);
        orders.addAll(returned);
        
        // Sắp xếp lại theo ngày (nếu cần)
        orders.sort((o1, o2) -> o2.getOrderDate().compareTo(o1.getOrderDate()));
        
        title = "Đơn hàng Hủy & Trả hàng";
        
    } else if (status != null && !status.isEmpty()) {
        // Logic cũ cho các trạng thái khác (Đang xử lý, Đã trả hàng)
        orders = orderService.findOrdersByStatus(status);
        title = "Đơn hàng (" + status + ")";
    } else {
        // Lấy tất cả
        orders = orderService.findAllOrderByOrderDateDesc();
        title = "Danh sách Đơn hàng";
    }
    
    model.addAttribute("orders", orders);
    model.addAttribute("pageTitle", title);
    
    return "Admin/orders";
    }
    @GetMapping("/export/inventory")
    public ResponseEntity<InputStreamResource> exportInventory() {
        List<com.example.demo.model.Product> products = productService.getAllProductsWithInventory();
        ByteArrayInputStream in = excelExportService.exportInventory(products);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=Bao_Cao_Ton_Kho.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new InputStreamResource(in));
    }

    // 2. API Xuất Excel Phiếu Kiểm Kê
    @GetMapping("/export/stocktake")
    public ResponseEntity<InputStreamResource> exportStocktake() {
        List<com.example.demo.model.Product> products = productService.getAllProductsWithInventory();
        ByteArrayInputStream in = excelExportService.exportStocktakeTemplate(products);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=Phieu_Kiem_Ke.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new InputStreamResource(in));
    }

    @PostMapping("/addCoupon")
    public String addCoupon(Coupon coupon) {
        couponService.save(coupon);
        return "redirect:/Admin/fixproduct";
    }

    @GetMapping("/deleteCoupon")
    public String deleteCoupon(@RequestParam Integer id) {
        couponService.delete(id);
        return "redirect:/Admin/fixproduct";
    }
}