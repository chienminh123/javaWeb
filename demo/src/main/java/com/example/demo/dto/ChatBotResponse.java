package com.example.demo.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatBotResponse {
    private String message;
    private String type; // "text", "products", "quick_replies"
    private List<ProductSuggestion> products;
    private List<QuickReply> quickReplies;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductSuggestion {
        private Integer productId;
        private String productName;
        private String imageUrl;
        private Float price;
        private Integer discount;
        private String genreName;
        private String productUrl;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuickReply {
        private String text;
        private String payload;
    }
}

