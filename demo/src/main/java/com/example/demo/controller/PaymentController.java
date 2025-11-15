package com.example.demo.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.model.Orders;
import com.example.demo.model.Sizes;
import com.example.demo.service.EmailService;
import com.example.demo.service.OrderService;
import com.example.demo.service.ProductService;
import com.example.demo.service.VNPayService;

import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;

@Controller
public class PaymentController {
    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);
    @Autowired private VNPayService vnpayService;
    @Autowired private OrderService orderService;
    @Autowired private EmailService emailService;
    @Autowired private ProductService productService;

    /**
     * SỬA ĐỔI: Nhận HttpServletRequest để xác thực chữ ký thật
     */
    @GetMapping("/vnpay_return")
    public String vnPayReturnHandler(
        HttpServletRequest request,
        RedirectAttributes redirectAttributes) {

        // Lấy tham số từ VNPay
        String orderIdStr = request.getParameter("vnp_TxnRef");
        String responseCode = request.getParameter("vnp_ResponseCode");

        if (orderIdStr == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy mã đơn hàng.");
            return "redirect:/User/order";
        }

        Integer orderId = Integer.parseInt(orderIdStr);
        Orders order = orderService.findOrderById(orderId)
                                .orElse(null);
        if (order == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy đơn hàng #" + orderId);
            return "redirect:/User/order";
        }

        // 1. [SỬA ĐỔI] Xác thực chữ ký bằng service thật
        if (vnpayService.processVnPayReturn(request)) {
            // Chữ ký hợp lệ
            
            if ("00".equals(responseCode)) {
                // Giao dịch thành công
                order.setStatus("Đã thanh toán (VNPay)");
                orderService.save(order);
                
                // Gửi email xác nhận
                sendOrderEmails(order);

                redirectAttributes.addFlashAttribute("successMessage", "Thanh toán thành công! Đơn hàng #" + orderId);
            } else {
                // Giao dịch thất bại (ví dụ: hủy, thiếu tiền)
                order.setStatus("Thanh toán thất bại");
                orderService.save(order);
                redirectAttributes.addFlashAttribute("errorMessage", "Thanh toán thất bại qua VNPay (Mã lỗi: " + responseCode + ")");
            }
        } else {
            // Chữ ký KHÔNG hợp lệ (Cảnh báo gian lận)
            logger.warn("Cảnh báo: Chữ ký VNPay không hợp lệ cho đơn hàng #{}", orderId);
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi xác thực thanh toán: Chữ ký không hợp lệ.");
        }

        return "redirect:/User/order";
    }
    
    // Tách hàm gửi email (Giống trong CartController)
    private void sendOrderEmails(Orders order) {
        try {
             emailService.sendOrderConfirmation(order);
             emailService.sendNewOrderNotification(order);
             List<Sizes> lowStockItems = productService.checkLowStockAfterOrder(order, 5); 
             if (!lowStockItems.isEmpty()) {
                 emailService.sendLowStockNotification(lowStockItems);
             }
        } catch (MessagingException e) {
             logger.warn("LỖI GỬI EMAIL XÁC NHẬN (PaymentController):", e);
        }
    }
}