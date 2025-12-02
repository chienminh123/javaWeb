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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
            return "redirect:Auth/login"; 
        }
        
        String phone = principal.getName(); 
        User currentUser = userService.findByPhone(phone);

        if (currentUser == null) {
             throw new RuntimeException("User không tồn tại");
        }

        model.addAttribute("user", currentUser);
        return "Auth/profile"; 
    }


    @PostMapping("Auth/updateProfile")
    public String updateProfile(
            @RequestParam String email,
            @RequestParam String address,
            @RequestParam String userName,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        
        try {
            userService.updateUserProfile(principal.getName(),userName, email, address);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Cập nhật thất bại: " + e.getMessage());
        }
        return "redirect:/Auth/profile";
    }

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
    
    
    @GetMapping("/User/order")
    public String userOrders(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/Auth/login";
        }
        
        String userPhone = principal.getName();
        
        List<Orders> orderList = ordersRepo.findByUserPhoneOrderByOrderDateDesc(userPhone);
        
        model.addAttribute("orders", orderList);
        return "User/order"; 
    }

    @GetMapping("/User/order-detail/{orderId}")
    public String orderDetail(@PathVariable("orderId") Integer orderId, Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/Auth/login";
        }

        Orders order = orderService.findOrderDetailsById(orderId) 
            .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng ID: " + orderId));
        
        String userPhone = principal.getName();

        if (!order.getUser().getPhone().equals(userPhone)) {
            throw new RuntimeException("Bạn không có quyền xem đơn hàng này."); 
        }
        double totalPrice;
        if (order.getFinalTotal() != null && order.getFinalTotal() > 0) {
            totalPrice = order.getFinalTotal();
        } else {
            totalPrice = order.getOrderDetails().stream()
                            .mapToDouble(d -> d.getPrice() * d.getQuantity())
                            .sum();
        }
                                            
        model.addAttribute("order", order);
        model.addAttribute("totalPrice", totalPrice);
        
        return "User/order-detail"; 
    }

    @PostMapping("/User/order/cancel")
    public String cancelOrder(
            @RequestParam("orderId") Integer orderId,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        if (principal == null) {
            return "redirect:/Auth/login";
        }

        try {
            Orders order = orderService.findOrderById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng."));

            if (!order.getUser().getPhone().equals(principal.getName())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: Bạn không có quyền thao tác trên đơn hàng này.");
                return "redirect:/User/order";
            }

            String currentStatus = order.getStatus();
            if ("Đang xử lý".equals(currentStatus) || "Chờ thanh toán".equals(currentStatus) ||"Đã thanh toán VNPay".equals(currentStatus)) {
                orderService.updateOrderStatus(orderId, "Đã hủy");
                
                redirectAttributes.addFlashAttribute("successMessage", "Đã hủy đơn hàng #" + orderId + " thành công.");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Không thể hủy đơn hàng đang ở trạng thái: " + currentStatus);
            }

        } catch (Exception e) {
            logger.error("Lỗi khi hủy đơn hàng: ", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi hệ thống: " + e.getMessage());
        }
        return "redirect:/User/order-detail/" + orderId;
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

            if (!order.getUser().getPhone().equals(principal.getName())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: Bạn không có quyền thực hiện thao tác này.");
                return "redirect:/User/orders";
            }

            if (!order.getStatus().equals("Đã giao hàng")) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không thể trả hàng cho đơn hàng đang ở trạng thái: " + order.getStatus());
                return "redirect:/User/order-detail/" + orderId;
            }
            
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
    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam String email, Model model, HttpServletRequest request) {
        try {
            String token = userService.generateResetToken(email); 
            
            String resetUrl = request.getRequestURL().toString().replace(request.getServletPath(), "") 
                            + "/reset-password?token=" + token;
            
            emailService.sendResetPasswordEmail(email, resetUrl);
            
            model.addAttribute("message", "Link đặt lại mật khẩu đã được gửi vào email của bạn.");
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }
        return "Auth/forgot-password";
    }

    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam String token, Model model) {
        model.addAttribute("token", token);
        return "Auth/reset-password";
    }

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

