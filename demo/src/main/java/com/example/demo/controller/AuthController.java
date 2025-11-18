package com.example.demo.controller;

import java.security.Principal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.model.Carts;
import com.example.demo.model.Orders;
import com.example.demo.model.User;
import com.example.demo.repository.OrdersRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.CartService;
import com.example.demo.service.EmailService;
import com.example.demo.service.OrderService;
import com.example.demo.service.UserService;

import jakarta.servlet.http.HttpServletRequest;




@Controller
public class AuthController {

    @Autowired
    private UserService userService;
    @Autowired
    private OrderService orderService;

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private OrdersRepository ordersRepo;
    @Autowired
    private CartService cartService;
    @Autowired
    private EmailService emailService;
    
    
    @GetMapping("/Auth/login")
    public String loginForm(Model model) {
        return "Auth/login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("user", new User());
        return "Auth/register";
    }
    
    @PostMapping("/Auth/register")
    public String registerUser(User user, Model model) {
        if (userRepository.findByPhone(user.getPhone()) != null) {
            model.addAttribute("error", "Số điện thoại người dùng đã tồn tại!");
            return "Auth/register";
        }
        if (user.getRole() == null || user.getRole().isEmpty()) {
            long userCount = userRepository.count();
            if (userCount == 0) {
                user.setRole("ADMIN"); // Người dùng đầu tiên là ADMIN
            } else {
                user.setRole("USER"); // Các người dùng khác là USER
            }
        }
        // Mã hóa mật khẩu trước khi lưu
        user.setPassWord(passwordEncoder.encode(user.getPassWord()));
        try {
            User saved = userRepository.save(user);
            logger.info("User saved: id={}, phone={}", saved.getUserId(), saved.getPhone());
        } catch (Exception e) {
            logger.error("Error saving user", e);
            model.addAttribute("error", "Lỗi khi lưu người dùng: " + e.getMessage());
            return "Auth/register";
        }
        return "redirect:/Auth/login";
    }

    @GetMapping("Auth/profile")
    public String userProfile(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:Auth/login"; // (Hoặc trang đăng nhập của bạn)
        }
        
        // principal.getName() sẽ là SĐT (vì bạn dùng SĐT để đăng nhập)
        String phone = principal.getName(); 
        User currentUser = userService.findByPhone(phone);

        if (currentUser == null) {
             throw new RuntimeException("User không tồn tại");
        }

        model.addAttribute("user", currentUser);
        return "Auth/profile"; // Trả về file HTML mới
    }

    /**
     * POST: Xử lý Sửa Thông Tin
     */
    @PostMapping("Auth/updateProfile")
    public String updateProfile(
            @RequestParam String email,
            @RequestParam String address,
            @RequestParam String userName,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        
        try {
            // Sửa SĐT và địa chỉ, SĐT lấy từ user đang đăng nhập
            userService.updateUserProfile(principal.getName(),userName, email, address);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Cập nhật thất bại: " + e.getMessage());
        }
        return "redirect:/Auth/profile";
    }

    /**
     * POST: Xử lý Đổi Mật Khẩu
     */
    @PostMapping("Auth/changePassword")
    public String changePassword(
            @RequestParam String oldPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("passwordError", "Mật khẩu mới không khớp!");
            return "redirect:/Auth/profile";
        }

        try {
            userService.changeUserPassword(principal.getName(), oldPassword, newPassword);
            redirectAttributes.addFlashAttribute("passwordSuccess", "Đổi mật khẩu thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("passwordError", "Lỗi: " + e.getMessage());
        }
        return "redirect:/Auth/profile";
    }
    
    @ModelAttribute
public void addGlobalAttributes(Model model, Principal principal) {
    if (principal != null) {
        String userPhone = principal.getName();
        // 1. Gửi SĐT
        System.out.println("DEBUG CHECKOUT: Giá trị principal.getName() (SĐT) là: " + userPhone);
        model.addAttribute("currentUserPhone", userPhone);
        Carts userCart = cartService.getCart(userPhone);
        int itemCount = cartService.calculateItemCount(userCart);
        model.addAttribute("cartItemCount", itemCount);
    } else {
        // Nếu chưa đăng nhập, luôn gửi itemCount = 0
        model.addAttribute("cartItemCount", 0); 
    }
    if (principal != null) {
    String userPhone = principal.getName();
    System.out.println("DEBUG: Principal Name (SĐT) là: " + userPhone);
    }
}
    
    @GetMapping("/User/order")
    public String userOrders(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/Auth/login";
        }
        
        String userPhone = principal.getName();
        
        // (Dùng hàm cũ để lấy TẤT CẢ)
        List<Orders> orderList = ordersRepo.findByUserPhoneOrderByOrderDateDesc(userPhone);
        
        model.addAttribute("orders", orderList);
        return "User/order"; 
    }

    @GetMapping("/User/order-detail/{orderId}")
    public String orderDetail(@PathVariable("orderId") Integer orderId, Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/Auth/login";
        }
        
        // 1. Lấy đơn hàng và chi tiết
        Orders order = orderService.findOrderDetailsById(orderId) 
            .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng ID: " + orderId));
        
        String userPhone = principal.getName();
        
        // 2. Kiểm tra quyền sở hữu
        if (!order.getUser().getPhone().equals(userPhone)) {
            // Ngăn người dùng xem đơn hàng của người khác
            throw new RuntimeException("Bạn không có quyền xem đơn hàng này."); 
        }
        double totalPrice = order.getOrderDetails().stream()
                                            .mapToDouble(d -> d.getPrice() * d.getQuantity())
                                            .sum();
                                            
        model.addAttribute("order", order);
        model.addAttribute("totalPrice", totalPrice);
        
        return "User/order-detail"; 
    }

    @PostMapping("/User/order/return")
    public String returnOrder(
            @RequestParam("orderId") Integer orderId,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        if (principal == null) {
            return "redirect:/Auth/login";
        }

        try {
            Orders order = orderService.findOrderById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng."));

            // 1. Kiểm tra bảo mật: Đơn hàng này có phải của user đang đăng nhập không?
            if (!order.getUser().getPhone().equals(principal.getName())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: Bạn không có quyền thực hiện thao tác này.");
                return "redirect:/User/orders";
            }

            // 2. Kiểm tra nghiệp vụ: Chỉ cho phép trả hàng khi "Đã giao hàng"
            if (!order.getStatus().equals("Đã giao hàng")) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không thể trả hàng cho đơn hàng đang ở trạng thái: " + order.getStatus());
                return "redirect:/User/order-detail/" + orderId;
            }
            
            // 3. Thực hiện cập nhật trạng thái (Logic hoàn kho sẽ chạy trong service này)
            orderService.updateOrderStatus(orderId, "Đã trả hàng");

            redirectAttributes.addFlashAttribute("successMessage", "Yêu cầu trả hàng cho đơn hàng #" + orderId + " đã được gửi. Kho đã được cập nhật.");

        } catch (Exception e) {
            logger.error("Lỗi khi xử lý trả hàng: ", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }

        return "redirect:/User/order-detail/" + orderId;
    }

    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        return "Auth/forgot-password";
    }
    // 2. Xử lý gửi email
    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam String email, Model model, HttpServletRequest request) {
        try {
            // Tạo token
            String token = userService.generateResetToken(email); // Cần thêm hàm này vào interface UserService nếu bạn dùng Interface
            
            // Tạo URL reset (ví dụ: http://localhost:8080/reset-password?token=xyz...)
            String resetUrl = request.getRequestURL().toString().replace(request.getServletPath(), "") 
                            + "/reset-password?token=" + token;
            
            // Gửi email
            emailService.sendResetPasswordEmail(email, resetUrl);
            
            model.addAttribute("message", "Link đặt lại mật khẩu đã được gửi vào email của bạn.");
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }
        return "Auth/forgot-password";
    }

    // 3. Hiển thị form nhập mật khẩu mới (từ link email)
    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam String token, Model model) {
        model.addAttribute("token", token);
        return "Auth/reset-password";
    }

    // 4. Xử lý đổi mật khẩu
    @PostMapping("/reset-password")
    public String processResetPassword(
            @RequestParam String token, 
            @RequestParam String password, 
            Model model,
            RedirectAttributes redirectAttributes) {
        
        try {
            userService.resetPassword(token, password);
            redirectAttributes.addFlashAttribute("successMessage", "Đặt lại mật khẩu thành công! Vui lòng đăng nhập.");
            return "redirect:/Auth/login";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", e.getMessage());
            return "Auth/reset-password";
        }
    }
}

