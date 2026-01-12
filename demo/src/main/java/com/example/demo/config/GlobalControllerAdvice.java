
package com.example.demo.config;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.example.demo.model.Carts;
import com.example.demo.model.User;
import com.example.demo.service.CartService;
import com.example.demo.service.UserService;

@ControllerAdvice
public class GlobalControllerAdvice {

    @Autowired
    private CartService cartService;

    @Autowired
    private UserService userService;
    
    @Autowired
    private com.example.demo.service.AutoStockDiscountConfigService autoStockDiscountConfigService;

    @ModelAttribute
    public void addGlobalAttributes(Model model, Principal principal) {
        model.addAttribute("cartItemCount", 0);
        model.addAttribute("currentUserName", null);
        
        if (principal == null) {
            return;
        }

        try {
            String userPhone = principal.getName();
            model.addAttribute("currentUserPhone", userPhone);

            try {
                User user = userService.findByPhone(userPhone);
                if (user != null) {
                    model.addAttribute("currentUserName", user.getUserName());
                }
            } catch (Exception e) {
                System.err.println("Lỗi lấy User: " + e.getMessage());
            }

            try {
                Carts userCart = cartService.getCart(userPhone);
                if (userCart != null && userCart.getCartDetails() != null) {
                    int itemCount = userCart.getCartDetails().size();
                    model.addAttribute("cartItemCount", itemCount);
                }
            } catch (Exception e) {
                System.err.println("Lỗi lấy Giỏ hàng: " + e.getMessage());
            }

        } catch (Exception e) {
            System.err.println("Lỗi Global Controller: " + e.getMessage());
        }
        
        try {
            var configOpt = autoStockDiscountConfigService.getActiveConfig();
            if (configOpt.isPresent() && configOpt.get().isActiveNow()) {
                var config = configOpt.get();
                model.addAttribute("autoStockDiscountConfig", config);
                com.example.demo.model.Product.setCurrentConfig(config);
            } else {
                com.example.demo.model.Product.setCurrentConfig(null);
            }
        } catch (Exception e) {
            System.err.println("Lỗi lấy AutoStockDiscountConfig: " + e.getMessage());
            com.example.demo.model.Product.setCurrentConfig(null);
        }
    }
}