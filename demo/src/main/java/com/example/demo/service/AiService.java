package com.example.demo.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.demo.dto.AiResponse;
import com.example.demo.model.Product;
import com.example.demo.repository.ProductRepository;

@Service
public class AiService {
    @Autowired
    private ProductRepository productRepository;

    // Địa chỉ server Python (đang chạy ở port 8000)
    private final String PYTHON_API_URL = "http://localhost:8000/recommend/";

    public List<Product> getRecommendedProducts(Integer userId) {
        // 1. Nếu chưa đăng nhập thì trả về rỗng
        if (userId == null) {
            return new ArrayList<>();
        }

        RestTemplate restTemplate = new RestTemplate();
        List<Integer> productIds = new ArrayList<>();

        try {
            // 2. Gọi sang Python lấy danh sách ID
            AiResponse response = restTemplate.getForObject(
                PYTHON_API_URL + userId, 
                AiResponse.class
            );

            if (response != null && response.getRecommendations() != null) {
                productIds = response.getRecommendations();
            }
        } catch (Exception e) {
            // Nếu Python chưa bật, web vẫn chạy bình thường (chỉ không có gợi ý)
            System.err.println("⚠ Không gọi được AI Service: " + e.getMessage());
            return new ArrayList<>();
        }

        // 3. Nếu không có gợi ý nào thì trả về rỗng
        if (productIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 4. Lấy thông tin chi tiết sản phẩm từ Database dựa trên ID
        return productRepository.findAllById(productIds);
    }
}
