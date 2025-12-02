// package com.example.demo.config;

// import java.security.Principal;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.ui.Model;
// import org.springframework.web.bind.annotation.ControllerAdvice;
// import org.springframework.web.bind.annotation.ModelAttribute;

// import com.example.demo.model.Carts;
// import com.example.demo.service.CartService;

// import jakarta.servlet.http.HttpServletRequest;

// @ControllerAdvice
// public class GlobalControllerAdvice {

//     @Autowired
//     private CartService cartService;

//     @Autowired
//     private HttpServletRequest request;

//     @ModelAttribute("cartItemCount")
//     public Integer getCartItemCount() {
//         var principal = request.getUserPrincipal();
//         if (principal != null) {
//             String userPhone = principal.getName();
//             Carts cart = cartService.getCart(userPhone);
//             if (cart != null && cart.getCartDetails() != null) {
//                 return cart.getCartDetails().size();
//             }
//         }
//         return 0;
//     }
//        @ModelAttribute
// public void addGlobalAttributes(Model model, Principal principal) {
//     if (principal != null) {
//         String userPhone = principal.getName();
//         // 1. Gửi SĐT
//         System.out.println("DEBUG CHECKOUT: Giá trị principal.getName() (SĐT) là: " + userPhone);
//         model.addAttribute("currentUserPhone", userPhone);
//         Carts userCart = cartService.getCart(userPhone);
//         int itemCount = cartService.calculateItemCount(userCart);
//         model.addAttribute("cartItemCount", itemCount);
//     } else {
//         // Nếu chưa đăng nhập, luôn gửi itemCount = 0
//         model.addAttribute("cartItemCount", 0); 
//     }
//     if (principal != null) {
//     String userPhone = principal.getName();
//     System.out.println("DEBUG: Principal Name (SĐT) là: " + userPhone);
//     }
// }
// }
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
        model.addAttribute("currentUserName", "Khách");
        
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