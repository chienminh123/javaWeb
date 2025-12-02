package com.example.demo.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.model.Orders;
import com.example.demo.service.OrderService;
import com.example.demo.service.VNPayService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class PaymentController {
    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);
    @Autowired private VNPayService vnpayService;
    @Autowired private OrderService orderService;
    


    @GetMapping("/vnpay_return")
    public String vnPayReturnHandler(
        HttpServletRequest request,
        RedirectAttributes redirectAttributes) {

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

        if (vnpayService.processVnPayReturn(request)) {
            
            if ("00".equals(responseCode)) {
                order.setStatus("Đã thanh toán VNPay");
                orderService.save(order);
                
                orderService.notifyOrderSuccess(order);

                redirectAttributes.addFlashAttribute("successMessage", "Thanh toán thành công! Đơn hàng #" + orderId);
            } else {
                order.setStatus("Thanh toán thất bại");
                orderService.save(order);
                redirectAttributes.addFlashAttribute("errorMessage", "Thanh toán thất bại qua VNPay (Mã lỗi: " + responseCode + ")");
            }
        } else {
            logger.warn("Cảnh báo: Chữ ký VNPay không hợp lệ cho đơn hàng #{}", orderId);
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi xác thực thanh toán: Chữ ký không hợp lệ.");
        }

        return "redirect:/User/order";
    }
    
}