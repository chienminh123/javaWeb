package com.example.demo.controller;

import java.security.Principal; // Thêm import
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller; // Thêm import
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.model.Carts; // Thêm import
import com.example.demo.model.Coupon;
import com.example.demo.model.Orders;
import com.example.demo.service.CartService;
import com.example.demo.service.CouponService;
import com.example.demo.service.EmailService;
import com.example.demo.service.OrderService;
import com.example.demo.service.ProductService;
import com.example.demo.service.VNPayService;

import jakarta.servlet.http.HttpServletRequest; 

@Controller
public class CartController {

    @Autowired
    private CartService cartService; // Inject Service mới
    @Autowired
    private OrderService orderService; // Inject OrderService để xử lý đặt hàng
    @Autowired
    private EmailService emailService;
    @Autowired
    private ProductService productService;
    @Autowired
    private VNPayService vnpayService;
    @Autowired private CouponService couponService;

    private static final Logger logger = LoggerFactory.getLogger(CartController.class);
    
    @PostMapping("/cart/add")
    public String addToCart(
            @RequestParam("productId") Integer productId,
            @RequestParam(name = "sizeId", required = false) Integer sizeId,
            @RequestParam("quantity") Integer quantity,
            Principal principal, // Lấy user đang đăng nhập
            RedirectAttributes redirectAttributes) { // Dùng để gửi thông báo lỗi
        
        // Yêu cầu đăng nhập (Spring Security sẽ xử lý, nhưng check lại)
        if (principal == null) {
            return "redirect:/Auth/login";
        }
        if (sizeId == null) {
            redirectAttributes.addFlashAttribute("error", "Sản phẩm này chưa có size, không thể thêm");
            return "redirect:/product/" + productId;
        }

        try {
            // Gọi Service để xử lý logic
            cartService.addToCart(principal.getName(), productId, sizeId, quantity);
        } catch (Exception e) {
            // Gửi thông báo lỗi (ví dụ: Hết hàng) về trang chi tiết
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/product/" + productId;
        }

        return "redirect:/cart";
    }

    @GetMapping("/cart")
    public String showCart(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/Auth/login";
        }
        
        // Lấy giỏ hàng của user
        Carts cart = cartService.getCart(principal.getName());
        
        // Tính tổng tiền
        float total = cartService.calculateTotal(cart);
        
        model.addAttribute("cart", cart);
        model.addAttribute("totalPrice", total);
        
        return "User/cart";
    }
    @PostMapping("/cart/update")
    @ResponseBody // Yêu cầu Spring trả về dữ liệu (String) thay vì tên view
    public String updateCartItemQuantity(
            @RequestParam Integer cartDetailId,
            @RequestParam Integer quantity) {
        
        try {
            cartService.updateQuantity(cartDetailId, quantity);
            return "SUCCESS";
        } catch (Exception e) {
            // Trả về thông báo lỗi cho JavaScript xử lý
            return "ERROR: " + e.getMessage();
        }
    }

    @PostMapping("/cart/delete")
    @ResponseBody 
    public String deleteCartItem(@RequestParam Integer cartDetailId) {
        try {
            cartService.deleteItem(cartDetailId);
            return "SUCCESS";
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    @GetMapping("/checkout")
    public String showCheckout(Model model, Principal principal) {
        // 1. Kiểm tra đăng nhập
        if (principal == null) {
            return "redirect:/Auth/login";
        }
        
        // 2. Lấy giỏ hàng của user
        // Giả định CartService đã có các hàm này
        Carts cart = cartService.getCart(principal.getName());
        
        // Kiểm tra giỏ hàng rỗng
        if (cart == null || cart.getCartDetails() == null || cart.getCartDetails().isEmpty()) {
            // Có thể chuyển hướng về trang giỏ hàng hoặc báo lỗi
            return "redirect:/cart?error=Giỏ hàng rỗng";
        }
        
        // 3. Tính tổng tiền và chuẩn bị dữ liệu (ví dụ: thông tin User, địa chỉ mặc định)
        float total = cartService.calculateTotal(cart);
        // (Bạn cần thêm logic lấy thông tin User như tên, địa chỉ, SĐT để điền vào form)
        
        model.addAttribute("cart", cart);
        model.addAttribute("totalPrice", total);
        
        return "User/checkout"; 
    }

    
    @PostMapping("/checkout/confirm")
    public String confirmCheckout(
        @RequestParam("address") String address,
        @RequestParam("phone") String phone, 
        @RequestParam("paymentMethod") String paymentMethod,
        Principal principal, 
        RedirectAttributes redirectAttributes,
        HttpServletRequest request,
        @RequestParam(required = false) String couponCode
    ) {

        if (principal == null) {
            return "redirect:/Auth/login"; 
        }

        Orders newOrder = null;
        try {
            String userPhone = principal.getName();

            // 1. TẠO ĐƠN HÀNG (Trừ kho, tạo Order)
            newOrder = orderService.createOrderFromCart(userPhone, address, phone, paymentMethod, couponCode);

            // 2. XỬ LÝ LUỒNG THANH TOÁN
            if ("VNPay".equals(paymentMethod)) {
                
                newOrder.setStatus("Chờ thanh toán VNPay");
                orderService.save(newOrder); // Lưu trạng thái
                
                String orderInfo = "Thanh toan don hang #" + newOrder.getOrderId();
                // Gọi hàm createPaymentUrl thật (cần request để lấy IP)
                String vnpayUrl = vnpayService.createPaymentUrl(newOrder, request); 
                
                // Chuyển hướng người dùng đến Cổng VNPay Sandbox
                return "redirect:" + vnpayUrl; 

            } else if ("Chuyển khoản".equals(paymentMethod)) {
                newOrder.setStatus("Chờ thanh toán"); 
                orderService.save(newOrder); 

            } else { // COD
                newOrder.setStatus("Đang xử lý"); 
                orderService.save(newOrder); 
            }
            
            // 3. GỬI EMAIL (Chỉ gửi cho COD và Chuyển khoản thủ công)
            // sendOrderEmails(newOrder);
            orderService.notifyOrderSuccess(newOrder);
                    
            redirectAttributes.addFlashAttribute("successMessage", "Đặt hàng thành công! Mã đơn hàng: #" + newOrder.getOrderId());
            return "redirect:/User/order"; 

        } catch (Exception e) {
            logger.error("LỖI XỬ LÝ ĐẶT HÀNG:", e);
            String errorMessage = "Lỗi đặt hàng: " + (e.getMessage() != null ? e.getMessage() : "Lỗi không xác định.");
            redirectAttributes.addFlashAttribute("errorMessage", errorMessage);
            
            return "redirect:/cart"; 
        }
    }

    // private void sendOrderEmails(Orders order) {
    //     try {
    //          emailService.sendOrderConfirmation(order); 
    //          emailService.sendNewOrderNotification(order); 
    //          List<Sizes> lowStockItems = productService.checkLowStockAfterOrder(order, 5); 
    //          if (!lowStockItems.isEmpty()) {
    //              emailService.sendLowStockNotification(lowStockItems);
    //          }
    //     } catch (MessagingException e) {
    //         logger.warn("LỖI GỬI EMAIL XÁC NHẬN:", e);
    //     }
    // }

    @GetMapping("/api/coupon/check")
    @ResponseBody
    public Map<String, Object> checkCoupon(@RequestParam String code, @RequestParam Double total) {
        Map<String, Object> response = new HashMap<>();
        try {
            Coupon coupon = couponService.checkCoupon(code);
            double discount = couponService.calculateDiscount(coupon, total);
            response.put("valid", true);
            response.put("discountAmount", discount);
            response.put("newTotal", total - discount);
            if ("FIXED".equals(coupon.getDiscountType())) 
            {
                response.put("messageText", "Giảm trực tiếp " + String.format("%,.0f", coupon.getDiscountValue()) + "đ");
            }
            else 
            {
                response.put("messageText", "Giảm " + coupon.getDiscountValue() + "%");
            }
        } catch (Exception e) {
            response.put("valid", false);
            response.put("message", e.getMessage());
        }
        return response;
    }
}
