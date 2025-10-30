package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;



@Controller

public class AuthController {

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
    

    
    
}
