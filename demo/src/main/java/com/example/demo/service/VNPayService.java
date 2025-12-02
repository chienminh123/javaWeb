package com.example.demo.service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;

import com.example.demo.config.VNPayConfig;
import com.example.demo.model.Orders;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class VNPayService {

    private final String vnp_PayUrl = VNPayConfig.vnp_PayUrl;
    private final String vnp_TmnCode = VNPayConfig.vnp_TmnCode;
    private final String vnp_HashSecret = VNPayConfig.vnp_HashSecret;
    private final String vnp_ReturnUrl = VNPayConfig.vnp_ReturnUrl;


    public String createPaymentUrl(Orders order, HttpServletRequest req) {
        
        long amount;
        if (order.getFinalTotal() != null && order.getFinalTotal() > 0) {
            amount = (long) (order.getFinalTotal() * 100);
        } else {
  
            double totalPrice = order.getOrderDetails().stream().mapToDouble(d -> d.getPrice() * d.getQuantity()).sum();
            amount = (long) (totalPrice * 100);
        }

        String vnp_TxnRef = String.valueOf(order.getOrderId());
        String vnp_IpAddr = VNPayConfig.getIpAddress(req);
        String vnp_OrderInfo = "Thanh toan don hang " + vnp_TxnRef;

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", VNPayConfig.vnp_Version);
        vnp_Params.put("vnp_Command", VNPayConfig.vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", vnp_OrderInfo);
        vnp_Params.put("vnp_OrderType", "other"); // Loại hàng hóa
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", vnp_ReturnUrl);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        Calendar cld = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = sdf.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                hashData.append(fieldName);
                hashData.append('=');
                try {
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                    query.append('=');
                    query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }
        
        String queryUrl = query.toString();
        String vnp_SecureHash = hmacSHA512(vnp_HashSecret, hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
        
        return vnp_PayUrl + "?" + queryUrl;
    }


    public boolean processVnPayReturn(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Enumeration<String> paramsEnum = request.getParameterNames();
        while (paramsEnum.hasMoreElements()) {
            String fieldName = paramsEnum.nextElement();
            String fieldValue = request.getParameter(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                params.put(fieldName, fieldValue);
            }
        }

        String vnp_SecureHash = request.getParameter("vnp_SecureHash");
        if (params.containsKey("vnp_SecureHashType")) {
            params.remove("vnp_SecureHashType");
        }
        if (params.containsKey("vnp_SecureHash")) {
            params.remove("vnp_SecureHash");
        }

        String signValue = VNPayConfig.hashAllFields(params, vnp_HashSecret);
        

        return signValue.equals(vnp_SecureHash);
    }

    public static String hmacSHA512(final String key, final String data) {
        try {
            Mac sha512Hmac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), "HmacSHA512");
            sha512Hmac.init(secretKey);
            byte[] hash = sha512Hmac.doFinal(data.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}