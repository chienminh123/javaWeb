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
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
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
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
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
       
        @RequestParam(required = false) String[] sizeName,
        @RequestParam(required = false) String[] quantity  
    ) {
        productService.saveMultipleProducts(
            productName, providerId, genreId, basisPrice, description, images,
            sizeName, quantity
        );
        return "redirect:/Admin/tonkho";
    }
    @org.springframework.web.bind.annotation.DeleteMapping("/deleteProduct/{id}")
        @ResponseBody
        public org.springframework.http.ResponseEntity<String> deleteProduct(@PathVariable Integer id) {
            try {
                // Gọi Service để xóa
                productService.deleteProduct(id);
                return org.springframework.http.ResponseEntity.ok("Deleted");
            } catch (Exception e) {
                // Trả về lỗi nếu không xóa được (ví dụ: ràng buộc khóa ngoại)
                return org.springframework.http.ResponseEntity.badRequest().body(e.getMessage());
            }
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
        long userReturnedCount = orderService.countOrdersByStatus("Đã trả hàng"); 
        // 3. Gộp tổng số lượng
        long totalCancelledAndReturned = adminCancelledCount + userReturnedCount;

        long pendingOrdersCount = orderService.countOrdersByStatus("Đang xử lý"); 
        long vnpOrdersCount = orderService.countOrdersByStatus("Đã thanh toán VNPay");
        long totalPendingAndVnp = pendingOrdersCount + vnpOrdersCount;
        // 2. Đưa dữ liệu vào Model
        model.addAttribute("totalInventoryValue", totalInventoryValue);
        model.addAttribute("outOfStockCount", outOfStockCount);
        
        // === CODE MỚI: Đưa dữ liệu đếm vào Model ===
        model.addAttribute("cancelledOrdersCount", totalCancelledAndReturned);
        model.addAttribute("pendingOrdersCount", totalPendingAndVnp );
        
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
        "Giao hàng thành công",
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
    
    // Trong file: src/main/java/com/example/demo/controller/AdminController.java

@GetMapping("/orders")
public String showOrdersByStatus(
        @RequestParam(required = false) String status, 
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, // Thêm tham số này
        Model model) {
    
    String title = "Danh sách Đơn hàng";
    List<Orders> orders;
    
    // 1. Ưu tiên lọc theo Ngày + Trạng thái (Logic click từ biểu đồ)
    if (date != null) {
        // Mặc định biểu đồ doanh thu tính cho "Giao hàng thành công"
        String targetStatus = (status != null && !status.isEmpty()) ? status : "Giao hàng thành công";
        
        orders = orderService.findOrdersByStatusAndDate(targetStatus, date);
        title = "Đơn hàng " + targetStatus + " ngày " + date.toString();
    } 
    // 2. Logic cũ: Lọc theo Trạng thái hủy/trả
    else if ("Đã hủy".equals(status)) {
        List<Orders> cancelled = orderService.findOrdersByStatus("Đã hủy");
        List<Orders> returned = orderService.findOrdersByStatus("Đã trả hàng");
        orders = new java.util.ArrayList<>(cancelled);
        orders.addAll(returned);
        orders.sort((o1, o2) -> o2.getOrderDate().compareTo(o1.getOrderDate()));
        title = "Đơn hàng Hủy & Trả hàng";
    } 
    else if ("Cho_Xu_Ly".equals(status)) {
        List<Orders> processing = orderService.findOrdersByStatus("Đang xử lý");
        List<Orders> vnpay = orderService.findOrdersByStatus("Đã thanh toán VNPay");
        
        orders = new java.util.ArrayList<>(processing);
        orders.addAll(vnpay);

        orders.sort((o1, o2) -> o2.getOrderDate().compareTo(o1.getOrderDate()));
        
        title = "Đơn hàng Chờ xác nhận (Gồm VNPay)";
    } 
    else {
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
    // 1. API lấy danh sách NCC (Trả về JSON)
    @GetMapping("/api/providers")
    @ResponseBody
    public List<Provider> getProvidersApi() {
        return providerService.findAll();
    }

    // 2. API Cập nhật NCC
    @org.springframework.web.bind.annotation.PutMapping("/updateProvider")
    @ResponseBody
    @Transactional
    public Provider updateProvider(@RequestBody Provider provider) {
        Provider exist = providerService.findAll().stream()
            .filter(p -> p.getProviderId() == provider.getProviderId())
            .findFirst()
            .orElseThrow(() -> new RuntimeException("NCC không tồn tại"));
            
        exist.setProviderName(provider.getProviderName());
        exist.setProviderEmail(provider.getProviderEmail());
        exist.setProviderPhone(provider.getProviderPhone());
        exist.setProviderAddress(provider.getProviderAddress());
        
        return providerService.save(exist);
    }

    // 3. API Xóa NCC
    @org.springframework.web.bind.annotation.DeleteMapping("/deleteProvider/{id}")
    @ResponseBody
    @Transactional
    public String deleteProvider(@PathVariable Integer id) {
        // Lưu ý: Nếu DB có khóa ngoại, bạn cần xử lý try-catch ở đây
        // hoặc xóa các sản phẩm liên quan trước.
        // Ở đây giả định xóa cơ bản:
        providerService.findAll().stream()
            .filter(p -> p.getProviderId() == id)
            .findFirst()
            .ifPresent(p -> {
                
            });
        
        return "Deleted";
    }
  

  @Autowired
    private UserRepository userRepository;

    // --- 1. HIỂN THỊ DANH SÁCH + LỌC ---
  @GetMapping("/customers")
    public String listCustomers(Model model, 
                                @RequestParam(value = "min", required = false) Long min,
                                @RequestParam(value = "max", required = false) Long max) {
        List<User> list;
        if (min == null && max == null) {
            list = userRepository.findByRole("USER"); 
        } else {
            long minVal = (min != null) ? min : 0; 
            long maxVal = (max != null) ? max : Long.MAX_VALUE; 
            list = userRepository.findByRoleAndPointsBetweenOrderByPointsDesc("USER", minVal, maxVal);
        }
        model.addAttribute("customers", list); 
        model.addAttribute("minPoints", min);
        model.addAttribute("maxPoints", max);
        return "Admin/customers";
    }

 
    @GetMapping("/user/delete/{id}") 
    public String deleteUser(@PathVariable("id") int userId, RedirectAttributes redirectAttributes) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Người dùng không tồn tại!");
                return "redirect:/Admin/customers";
            }
            if (user.getOrders() != null && !user.getOrders().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa khách vì họ đã có đơn hàng. Hãy dùng chức năng KHÓA.");
                return "redirect:/Admin/customers";
            }
            userRepository.delete(user);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa khách hàng thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xóa: " + e.getMessage());
        }
        return "redirect:/Admin/customers";
    }

    @GetMapping("/user/lock/{id}") 
    public String lockUser(@PathVariable("id") int userId, RedirectAttributes redirectAttributes) {
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            user.setEnabled(false); 
            userRepository.save(user);
            redirectAttributes.addFlashAttribute("successMessage", "Đã khóa tài khoản: " + user.getUserName());
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy user!");
        }
        return "redirect:/Admin/customers";
    }

    @GetMapping("/user/unlock/{id}")
    public String unlockUser(@PathVariable("id") int userId, RedirectAttributes redirectAttributes) {
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            user.setEnabled(true);
            userRepository.save(user);
            redirectAttributes.addFlashAttribute("successMessage", "Đã mở khóa tài khoản.");
        }
        return "redirect:/Admin/customers";
    }


    
    // a. Hiển thị form sửa
    @GetMapping("/user-detail/{id}")
    public String showUserDetail(@PathVariable("id") Integer id, Model model) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Invalid user Id:" + id));
        model.addAttribute("user", user);
        return "Admin/user-detail"; // Bạn cần tạo file này (xem bước 2)
    }

    // b. Xử lý lưu sau khi sửa
    @PostMapping("/updateUser")
    public String updateUser(@ModelAttribute("user") User user, RedirectAttributes redirectAttributes) {
        User existingUser = userRepository.findById(user.getUserId()).orElse(null);
        if (existingUser != null) {
            // Chỉ cho phép cập nhật thông tin cá nhân, KHÔNG cập nhật mật khẩu/điểm ở đây nếu không cần thiết
            existingUser.setUserName(user.getUserName());
            existingUser.setPhone(user.getPhone());
            existingUser.setAddress(user.getAddress());
            existingUser.setEmail(user.getEmail());
            // existingUser.setRank(user.getRank()); // Nếu muốn sửa hạng thủ công
            
            userRepository.save(existingUser);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin khách hàng thành công!");
        }
        return "redirect:/Admin/customers";
    }
}
    
