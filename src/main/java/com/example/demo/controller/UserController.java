package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class UserController {
@GetMapping("/User/home")
 public String home(java.util.Map<String, Object> model) {
    model.put("message", "Chào mừng bạn đến với trang ban hang!");
    return "User/home"; // Trả về tên của template Thymeleaf (home.html)

}
}
