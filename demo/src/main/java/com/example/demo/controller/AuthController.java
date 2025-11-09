package com.example.demo.controller;

import java.security.Principal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.UserService;



@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    
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
            Principal principal,
            RedirectAttributes redirectAttributes) {
        
        try {
            // Sửa SĐT và địa chỉ, SĐT lấy từ user đang đăng nhập
            userService.updateUserProfile(principal.getName(), email, address);
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
            // principal.getName() chính là SĐT (vì bạn đăng nhập bằng SĐT)
            model.addAttribute("currentUserPhone", principal.getName());
        }
    }
}
