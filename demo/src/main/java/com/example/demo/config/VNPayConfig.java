package com.example.demo.config;

import com.example.demo.service.VNPayService; // Sẽ tạo ở Bước 2
import jakarta.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class VNPayConfig {
    
    // === [THÔNG TIN CẤU HÌNH CỦA BẠN] ===
    /**
     * Mã website (Terminal ID)
     */
    public static String vnp_TmnCode = "L4N84ZDD";
    
    /**
     * Chuỗi bí mật (Secret Key)
     */
    public static String vnp_HashSecret = "PFCZO5ML29ME0NV3NUM0OYBG09I1JAF8";
    // ======================================

    // URL Môi trường Sandbox
    public static String vnp_PayUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    public static String vnp_ApiUrl = "https://sandbox.vnpayment.vn/merchant_webapi/api/transaction";
    
    /**
     * URL trả về (Return URL) - Nơi VNPay chuyển hướng sau khi thanh toán.
     * Cần khớp với @GetMapping trong PaymentController.java
     */
    public static String vnp_ReturnUrl = "http://localhost:8080/vnpay_return"; 
    
    public static String vnp_Version = "2.1.0";
    public static String vnp_Command = "pay";

    // --- CÁC HÀM TIỆN ÍCH (Giữ nguyên) ---
    
    public static String getIpAddress(HttpServletRequest request) {
        String ipAddr = request.getHeader("X-Forwarded-For");
        if (ipAddr == null || ipAddr.isEmpty()) {
            ipAddr = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddr == null || ipAddr.isEmpty()) {
            ipAddr = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddr == null || ipAddr.isEmpty()) {
            ipAddr = request.getRemoteAddr();
        }
        return ipAddr.equals("0:0:0:0:0:0:0:1") ? "127.0.0.1" : ipAddr;
    }
    
    public static String hashAllFields(Map<String, String> fields, String hashSecret) {
        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = fields.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                hashData.append(fieldName);
                hashData.append('=');
                try {
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                if (itr.hasNext()) {
                    hashData.append('&');
                }
            }
        }
        // Gọi hàm hmacSHA512 từ VNPayService
        return VNPayService.hmacSHA512(hashSecret, hashData.toString());
    }
}