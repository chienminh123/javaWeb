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
    @Autowired private com.example.demo.service.AutoStockDiscountConfigService autoStockDiscountConfigService;

    @GetMapping("/addproduct")
    public String addProduct(Model model) {
        model.addAttribute("providers", providerService.findAll());
        model.addAttribute("genres", genreService.findAllGenres());
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
                productService.deleteProduct(id);
                return org.springframework.http.ResponseEntity.ok("Deleted");
            } catch (Exception e) {
                return org.springframework.http.ResponseEntity.badRequest().body(e.getMessage());
            }
        }
    @PostMapping("/addProvider")
    @ResponseBody
    @Transactional
    public Provider addProvider(@RequestBody Provider provider) {
        if (provider.getProviderName() == null || provider.getProviderName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên nhà cung cấp không được để trống");
        }
        return providerService.save(provider);
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
        model.addAttribute("products", productService.getAllProductsWithInventory());
        model.addAttribute("providers", providerService.findAll());
        model.addAttribute("genres", genreService.findAllGenres());
        
        var activeConfig = autoStockDiscountConfigService.getActiveConfig();
        model.addAttribute("autoStockDiscountConfig", activeConfig.orElse(null));
        model.addAttribute("allConfigs", autoStockDiscountConfigService.getAllConfigs());
        
        return "Admin/fixproduct";
    }
    
    @PostMapping("/saveAutoStockDiscountConfig")
    public String saveAutoStockDiscountConfig(
            @RequestParam Integer minStockQuantity,
            @RequestParam Integer discountPercent,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            RedirectAttributes redirectAttributes) {
        try {
            autoStockDiscountConfigService.createConfig(minStockQuantity, discountPercent, startDate, endDate);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cấu hình giảm giá tự động thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/Admin/coupons";
    }
    
    @GetMapping("/deleteAutoStockDiscountConfig")
    public String deleteAutoStockDiscountConfig(@RequestParam Integer id, RedirectAttributes redirectAttributes) {
        try {
            autoStockDiscountConfigService.deleteConfig(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa cấu hình giảm giá tự động thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/Admin/coupons";
    }
    
    @PostMapping("/updateSingleProduct")
    @ResponseBody
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
            String newImageUrl = productService.updateSingleProduct( 
                productId, providerId, genreId, productName,
                basisPrice, markupPercent, discount,description, imageFile
            );

            return Map.of(
                "success", true,
                "message", "Cập nhật thành công!",
                "newImageUrl", (newImageUrl != null ? newImageUrl : "")
            );
        } catch (Exception e) {
            return Map.of(
                "success", false,
                "message", e.getMessage()
            );
        }
    }
    


    
    @GetMapping("/report")
    public String reports(Model model,
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        if (startDate == null) {
            startDate = endDate.minusDays(30);
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
        model.addAttribute("providers", providerService.findAll());
        model.addAttribute("productSuggestions", productService.getAllProductSuggestionsMap());
        return "Admin/export";
    }
    @PostMapping("/processExport")
    public String processExport(
        @RequestParam(required = false) Integer[] providerId,
        @RequestParam(required = false) Integer[] productId,
        @RequestParam(required = false) String[][] sizeName,
        @RequestParam(required = false) Integer[][] quantity,
        Model model
    ) {
        if (providerId == null || productId == null) {
            model.addAttribute("errorMessage", "Bạn chưa thêm sản phẩm nào để xuất kho.");
            model.addAttribute("providers", providerService.findAll());
            model.addAttribute("productSuggestions", productService.getAllProductSuggestionsMap());
            return "Admin/export";
        }

        try {
            productService.exportMultipleProducts(providerId, productId, sizeName, quantity);
            return "redirect:/Admin/tonkho"; 

        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("providers", providerService.findAll());
            model.addAttribute("productSuggestions", productService.getAllProductSuggestionsMap());
            return "Admin/export";
        }
    }
    
    @GetMapping("/inventory")
    public String inventory(Model model) {
        model.addAttribute("productsWithSizes", productService.getAllProductsWithInventory());
        return "Admin/inventory";
    }
    @PostMapping("/saveInventoryCheck")
    public String saveInventoryCheck(
        @RequestParam Integer[] productId,
        @RequestParam String[] sizeName,
        @RequestParam Integer[] systemQty,
        @RequestParam Integer[] actualQty,
        @RequestParam String[] note
    ) {
        productService.saveInventoryCheck(
            productId,
            sizeName,
            systemQty,
            actualQty,
            note
        );
        return "redirect:/Admin/tonkho";
    }

   
   
    @GetMapping("/home")
    public String home(Model model) {
        double totalInventoryValue = productService.calculateTotalInventoryValue();
        long outOfStockCount = productService.countOutOfStockProducts();

        long adminCancelledCount = orderService.countOrdersByStatus("Đã hủy");
        long userReturnedCount = orderService.countOrdersByStatus("Đã trả hàng"); 
        long totalCancelledAndReturned = adminCancelledCount + userReturnedCount;

        long pendingOrdersCount = orderService.countOrdersByStatus("Đang xử lý"); 
        long vnpOrdersCount = orderService.countOrdersByStatus("Đã thanh toán VNPay");
        long totalPendingAndVnp = pendingOrdersCount + vnpOrdersCount;
        
        model.addAttribute("totalInventoryValue", totalInventoryValue);
        model.addAttribute("outOfStockCount", outOfStockCount);
        model.addAttribute("cancelledOrdersCount", totalCancelledAndReturned);
        model.addAttribute("pendingOrdersCount", totalPendingAndVnp );
        
        return "Admin/home";
    }

    @GetMapping("/order-detail")
public String showOrderDetail(@RequestParam("orderId") Integer orderId, Model model) {
    Orders order = orderService.findOrderDetailsById(orderId)
        .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng ID: " + orderId));

    List<String> statusList = List.of(
        "Chờ thanh toán",
        "Đang xử lý",
        "Đã xác nhận",
        "Đang giao hàng",
        "Giao hàng thành công",
        "Đã hủy"
    );

    model.addAttribute("order", order);
    model.addAttribute("statusList", statusList);

    return "Admin/order-detail";
}

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
                emailService.sendOrderStatusUpdate(updatedOrder, newStatus); 
            } catch (Exception e) {
                System.err.println("LỖI GỬI EMAIL CẬP NHẬT TRẠNG THÁI: " + e.getMessage());
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
            e.printStackTrace();
        }
        
        return "redirect:/Admin/order-detail?orderId=" + orderId;
    }

@GetMapping("/orders")
public String showOrdersByStatus(
        @RequestParam(required = false) String status, 
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        Model model) {
    
    String title = "Danh sách Đơn hàng";
    List<Orders> orders;
    
    if (date != null) {
        String targetStatus = (status != null && !status.isEmpty()) ? status : "Giao hàng thành công";
        
        orders = orderService.findOrdersByStatusAndDate(targetStatus, date);
        title = "Đơn hàng " + targetStatus + " ngày " + date.toString();
    } 
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

    @GetMapping("/coupons")
    public String coupons(Model model) {
        model.addAttribute("coupons", couponService.findAll());
        
        var activeConfig = autoStockDiscountConfigService.getActiveConfig();
        model.addAttribute("autoStockDiscountConfig", activeConfig.orElse(null));
        model.addAttribute("allConfigs", autoStockDiscountConfigService.getAllConfigs());
        
        return "Admin/coupons";
    }

    @PostMapping("/addCoupon")
    public String addCoupon(
            @RequestParam String code,
            @RequestParam String discountType,
            @RequestParam Double discountValue,
            @RequestParam(required = false) Double maxDiscountAmount,
            @RequestParam Integer quantity,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "false") boolean isActive) {
        Coupon coupon = new Coupon();
        coupon.setCode(code);
        coupon.setDiscountType(discountType);
        coupon.setDiscountValue(discountValue);
        coupon.setMaxDiscountAmount(maxDiscountAmount);
        coupon.setQuantity(quantity);
        coupon.setStartDate(startDate);
        coupon.setEndDate(endDate);
        coupon.setActive(isActive);
        couponService.save(coupon);
        return "redirect:/Admin/coupons";
    }

    @GetMapping("/deleteCoupon")
    public String deleteCoupon(@RequestParam Integer id) {
        couponService.delete(id);
        return "redirect:/Admin/coupons";
    }
    @GetMapping("/login")
    public String adminLogin(Model model) {
        return "Admin/login";
    }

    @GetMapping("/api/providers")
    @ResponseBody
    public List<Provider> getProvidersApi() {
        return providerService.findAll();
    }

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

    @org.springframework.web.bind.annotation.DeleteMapping("/deleteProvider/{id}")
    @ResponseBody
    @Transactional
    public String deleteProvider(@PathVariable Integer id) {
        providerService.findAll().stream()
            .filter(p -> p.getProviderId() == id)
            .findFirst()
            .ifPresent(p -> {
                
            });
        
        return "Deleted";
    }
  

  @Autowired
    private UserRepository userRepository;

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


    @GetMapping("/user-detail/{id}")
    public String showUserDetail(@PathVariable("id") Integer id, Model model) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Invalid user Id:" + id));
        model.addAttribute("user", user);
        return "Admin/user-detail";
    }

    @PostMapping("/updateUser")
    public String updateUser(@ModelAttribute("user") User user, RedirectAttributes redirectAttributes) {
        User existingUser = userRepository.findById(user.getUserId()).orElse(null);
        if (existingUser != null) {
            existingUser.setUserName(user.getUserName());
            existingUser.setPhone(user.getPhone());
            existingUser.setAddress(user.getAddress());
            existingUser.setEmail(user.getEmail());
            
            userRepository.save(existingUser);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin khách hàng thành công!");
        }
        return "redirect:/Admin/customers";
    }
}
    
