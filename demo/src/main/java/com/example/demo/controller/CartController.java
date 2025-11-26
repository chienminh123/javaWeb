package com.example.demo.controller;

import java.security.Principal; // Thêm import
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

import com.example.demo.model.CartDetail;
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
            Principal principal, 
            RedirectAttributes redirectAttributes) { 
    
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
    public String showCheckout(Model model, Principal principal,
                               @RequestParam(name = "selectedItems", required = false) List<Integer> selectedItems) {
        // 1. Kiểm tra đăng nhập
        if (principal == null) {
            return "redirect:/Auth/login";
        }
        
        // 2. Lấy giỏ hàng gốc
        Carts cart = cartService.getCart(principal.getName());
        
        if (cart == null || cart.getCartDetails() == null || cart.getCartDetails().isEmpty()) {
            return "redirect:/cart?error=Giỏ hàng rỗng";
        }

        // 3. LỌC SẢN PHẨM ĐƯỢC CHỌN (QUAN TRỌNG: Fix lỗi chọn 1 ra tất cả)
        // Nếu có danh sách ID được gửi lên, chỉ giữ lại các item đó
        if (selectedItems != null && !selectedItems.isEmpty()) {
            List<CartDetail> filteredDetails = cart.getCartDetails().stream()
                .filter(item -> selectedItems.contains(item.getCartDetailId()))
                .collect(Collectors.toList());
            
            // Set tạm vào object cart để view hiển thị (không lưu DB)
            cart.setCartDetails(filteredDetails); 
        }

        // 4. Tính lại tổng tiền dựa trên danh sách (đã lọc)
        float total = cartService.calculateTotal(cart);
        
        // 5. Lấy danh sách Coupon hợp lệ
        List<Coupon> allCoupons = couponService.findAll();
        List<Coupon> validCoupons = allCoupons.stream()
            .filter(c -> c.isActive())
            .filter(c -> c.getQuantity() > 0)
            .filter(c -> {
                LocalDate now = LocalDate.now();
                return (c.getStartDate() == null || !now.isBefore(c.getStartDate())) &&
                       (c.getEndDate() == null || !now.isAfter(c.getEndDate()));
            })
            .collect(Collectors.toList());

        // 6. Gửi dữ liệu sang View
        model.addAttribute("cart", cart);
        model.addAttribute("totalPrice", total); 
        model.addAttribute("userCoupons", validCoupons); 
        model.addAttribute("currentUser", cart.getUser());
        
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
        @RequestParam(required = false) String couponCode,
        // THÊM THAM SỐ NÀY ĐỂ NHẬN LIST ID SẢN PHẨM
        @RequestParam(name = "selectedItems", required = false) List<Integer> selectedItems 
    ) {
        if (principal == null) return "redirect:/Auth/login"; 

        Orders newOrder = null;
        try {
            String userPhone = principal.getName();

            // Gọi hàm Service với danh sách selectedItems
            newOrder = orderService.createOrderFromCart(userPhone, address, phone, paymentMethod, couponCode, selectedItems);

            if ("VNPay".equals(paymentMethod)) {
                newOrder.setStatus("Chờ thanh toán VNPay");
                orderService.save(newOrder); 
                String vnpayUrl = vnpayService.createPaymentUrl(newOrder, request); 
                return "redirect:" + vnpayUrl; 

            } else if ("Chuyển khoản".equals(paymentMethod)) {
                newOrder.setStatus("Chờ thanh toán"); 
                orderService.save(newOrder); 
            } else { 
                newOrder.setStatus("Đang xử lý"); 
                orderService.save(newOrder); 
            }
            
            orderService.notifyOrderSuccess(newOrder);
            redirectAttributes.addFlashAttribute("successMessage", "Đặt hàng thành công! Mã đơn: #" + newOrder.getOrderId());
            return "redirect:/User/order"; 

        } catch (Exception e) {
            logger.error("LỖI ĐẶT HÀNG:", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
            return "redirect:/cart"; 
        }
    }

   

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
