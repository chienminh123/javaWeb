package com.example.demo.config;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.example.demo.model.Carts;
import com.example.demo.service.CartService;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class GlobalControllerAdvice {

    @Autowired
    private CartService cartService;

    @Autowired
    private HttpServletRequest request;

    @ModelAttribute("cartItemCount")
    public Integer getCartItemCount() {
        var principal = request.getUserPrincipal();
        if (principal != null) {
            String userPhone = principal.getName();
            Carts cart = cartService.getCart(userPhone);
            if (cart != null && cart.getCartDetails() != null) {
                return cart.getCartDetails().size();
            }
        }
        return 0;
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
}