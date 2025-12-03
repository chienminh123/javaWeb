package com.example.demo.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.dto.ChatBotResponse;
import com.example.demo.model.Genre;
import com.example.demo.model.Product;

@Service
public class ChatBotService {

    @Autowired
    private ProductService productService;
    
    @Autowired
    private GenreService genreService;

    public ChatBotResponse processMessage(String userMessage) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return createTextResponse("Xin chào! Tôi có thể giúp gì cho bạn?");
        }

        String message = userMessage.toLowerCase().trim();

        // Chào hỏi
        if (matchesKeywords(message, Arrays.asList("xin chào", "chào", "hello", "hi", "hey"))) {
            return createGreetingResponse();
        }

        // Tạm biệt
        if (matchesKeywords(message, Arrays.asList("tạm biệt", "bye", "goodbye", "cảm ơn", "thanks"))) {
            return createTextResponse("Cảm ơn bạn đã liên hệ! Chúc bạn một ngày tốt lành! 😊");
        }

        // Hỏi về giờ làm việc
        if (matchesKeywords(message, Arrays.asList("giờ làm việc", "mở cửa", "đóng cửa", "thời gian"))) {
            return createTextResponse("Cửa hàng chúng tôi mở cửa từ 8:00 - 22:00 hàng ngày. Bạn có thể đặt hàng online 24/7!");
        }

        // Hỏi về vận chuyển
        if (matchesKeywords(message, Arrays.asList("vận chuyển", "ship", "giao hàng", "phí ship", "phí vận chuyển"))) {
            return createTextResponse("Chúng tôi giao hàng toàn quốc.Đang có chương trình trợ giá miễn phí vận chuyển nha các mom!");
        }

        // Hỏi về thanh toán
        if (matchesKeywords(message, Arrays.asList("thanh toán", "payment", "cách thanh toán", "trả tiền"))) {
            return createTextResponse("Chúng tôi hỗ trợ thanh toán khi nhận hàng (COD), chuyển khoản ngân hàng, và thanh toán online qua VNPay.");
        }

        // Hỏi về đổi trả
        if (matchesKeywords(message, Arrays.asList("đổi trả", "hoàn tiền", "return", "đổi hàng","trả hàng"))) {
            return createTextResponse("Chúng tôi hỗ trợ đổi trả trong vòng 7 ngày kể từ ngày nhận hàng. Sản phẩm phải còn nguyên vẹn, chưa sử dụng và có hóa đơn. Vui lòng liên hệ số Hotline 1900 6868 để được hỗ trợ thêm.");
        }

        // Tìm kiếm theo danh mục
        String category = extractCategory(message);
        if (category != null) {
            return searchByCategory(category);
        }

        // Tìm kiếm sản phẩm theo tên
        List<String> productKeywords = extractProductKeywords(message);
        if (!productKeywords.isEmpty()) {
            return searchProducts(productKeywords);
        }

        // Câu hỏi về giá
        if (matchesKeywords(message, Arrays.asList("giá", "bao nhiêu", "cost", "price"))) {
            return createTextResponse("Bạn muốn tìm sản phẩm nào? Vui lòng cho tôi biết tên sản phẩm hoặc danh mục bạn quan tâm.");
        }

        // Câu hỏi về khuyến mãi
        if (matchesKeywords(message, Arrays.asList("khuyến mãi", "giảm giá", "sale", "discount", "promotion"))) {
            return createTextResponse("Hiện tại chúng tôi có nhiều chương trình khuyến mãi hấp dẫn! Bạn có thể xem các sản phẩm đang giảm giá trên trang chủ hoặc tìm kiếm sản phẩm cụ thể.");
        }

        // Câu hỏi về độ tuổi
        //phần thể loại chia nhỏ hơn nữa để tư vấn độ tuổi
        if (matchesKeywords(message, Arrays.asList("tuổi", "tháng", "tháng tuổi", "cho bé", "dành cho"))) {
            return createTextResponse("Chúng tôi có sản phẩm cho mọi lứa tuổi: sơ sinh (0-6 tháng), nhũ nhi (6-12 tháng), trẻ nhỏ (1-3 tuổi), và trẻ lớn (3+ tuổi). Bạn muốn tìm sản phẩm cho bé bao nhiêu tháng/tuổi?");
        }

        // Câu hỏi không hiểu
        return createDefaultResponse();
    }

    private boolean matchesKeywords(String message, List<String> keywords) {
        return keywords.stream().anyMatch(message::contains);
    }

    private String extractCategory(String message) {
        List<String> categories = Arrays.asList(
            "bình sữa","sữa", "tã", "bỉm", "quần áo", "đồ chơi", "xe", "nôi", 
            "ghế ăn",  "máy hút sữa", "đồ dùng", "thực phẩm",
            "vitamin", "dinh dưỡng", "chăm sóc", "vệ sinh","Bình sữa","Túi","Áo","Quần","Tất",
            "Đồ chơi","Xe đẩy","Nôi","Ghế ăn","Máy hút sữa","Tã giấy","Tã vải"
        );
        
        for (String category : categories) {
            if (message.contains(category)) {
                return category;
            }
        }
        return null;
    }

    private List<String> extractProductKeywords(String message) {
        // Loại bỏ các từ không cần thiết
        List<String> stopWords = Arrays.asList(
            "tôi", "muốn", "mua", "cần", "tìm", "có", "bán", "giá", "bao nhiêu",
            "cho", "bé", "em bé", "của", "và", "hoặc", "là", "gì", "nào"
        );
        
        String[] words = message.split("\\s+");
        List<String> keywords = new ArrayList<>();
        
        for (String word : words) {
            word = word.trim();
            if (!word.isEmpty() && word.length() > 2 && !stopWords.contains(word)) {
                keywords.add(word);
            }
        }
        
        return keywords;
    }

    private ChatBotResponse searchByCategory(String category) {
        List<Genre> allGenres = genreService.findAllGenres();
        Genre matchedGenre = null;
        
        // Map category to genre
        String categoryLower = category.toLowerCase();
        for (Genre genre : allGenres) {
            String genreName = genre.getGenreName().toLowerCase();
            if (genreName.contains(categoryLower) || categoryLower.contains(genreName)) {
                matchedGenre = genre;
                break;
            }
        }
        
        if (matchedGenre != null) {
            List<Product> products = productService.findProductsByGenre(
                matchedGenre.getGenreId(), null, null, null
            );
            
            if (!products.isEmpty()) {
                List<Product> topProducts = products.stream()
                    .limit(5)
                    .collect(Collectors.toList());
                
                return createProductsResponse(
                    "Tôi tìm thấy các sản phẩm trong danh mục " + matchedGenre.getGenreName() + ":",
                    topProducts
                );
            }
        }
        
        //  tìm kiếm theo keyword
        List<Product> products = productService.searchSuggestions(category, null);
        if (!products.isEmpty()) {
            List<Product> topProducts = products.stream().limit(5).collect(Collectors.toList());
            return createProductsResponse(
                "Tôi tìm thấy các sản phẩm liên quan đến \"" + category + "\":",
                topProducts
            );
        }
        
        return createTextResponse("Xin lỗi, tôi không tìm thấy sản phẩm nào trong danh mục \"" + category + "\". Bạn có thể thử tìm kiếm với từ khóa khác hoặc xem tất cả sản phẩm trên website.");
    }

    private ChatBotResponse searchProducts(List<String> keywords) {
        // Thử tìm với từng keyword
        for (String keyword : keywords) {
            List<Product> products = productService.searchSuggestions(keyword, null);
            if (!products.isEmpty()) {
                List<Product> topProducts = products.stream().limit(5).collect(Collectors.toList());
                return createProductsResponse(
                    "Tôi tìm thấy các sản phẩm liên quan đến \"" + keyword + "\":",
                    topProducts
                );
            }
        }
        
        // Nếu không tìm thấy, gợi ý tìm kiếm
        return createTextResponse("Xin lỗi, tôi không tìm thấy sản phẩm nào với từ khóa \"" + 
            String.join(" ", keywords) + "\". Bạn có thể:\n" +
            "• Thử tìm kiếm với từ khóa khác\n" +
            "• Xem các danh mục sản phẩm\n" +
            "• Liên hệ với chúng tôi để được tư vấn");
    }

    private ChatBotResponse createGreetingResponse() {
        ChatBotResponse response = new ChatBotResponse();
        response.setType("quick_replies");
        response.setMessage("Xin chào! 👋 Tôi là trợ lý tư vấn của cửa hàng Mẹ & Bé. Tôi có thể giúp bạn:\n\n" +
            "• Tìm kiếm sản phẩm\n" +
            "• Tư vấn theo danh mục\n" +
            "• Hỏi về chính sách mua hàng\n" +
            "• Hỗ trợ đặt hàng");
        
        List<ChatBotResponse.QuickReply> quickReplies = Arrays.asList(
            new ChatBotResponse.QuickReply("Tìm sản phẩm", "Tìm sản phẩm"),
            new ChatBotResponse.QuickReply("Xem danh mục", "Danh mục"),
            new ChatBotResponse.QuickReply("Vận chuyển", "Vận chuyển"),
            new ChatBotResponse.QuickReply("Thanh toán", "Thanh toán")
        );
        response.setQuickReplies(quickReplies);
        return response;
    }

    private ChatBotResponse createTextResponse(String message) {
        ChatBotResponse response = new ChatBotResponse();
        response.setType("text");
        response.setMessage(message);
        return response;
    }

    private ChatBotResponse createProductsResponse(String message, List<Product> products) {
        ChatBotResponse response = new ChatBotResponse();
        response.setType("products");
        response.setMessage(message);
        
        List<ChatBotResponse.ProductSuggestion> productSuggestions = products.stream()
            .map(p -> {
                float finalPrice = p.getSellPrice();
                if (p.getDiscount() != null && p.getDiscount() > 0) {
                    finalPrice = p.getSellPrice() * (1 - p.getDiscount() / 100f);
                }
                
                return new ChatBotResponse.ProductSuggestion(
                    p.getProductId(),
                    p.getProductName(),
                    p.getImageUrl(),
                    finalPrice,
                    p.getDiscount(),
                    p.getGenreName(),
                    "/product/" + p.getProductId()
                );
            })
            .collect(Collectors.toList());
        
        response.setProducts(productSuggestions);
        return response;
    }

    private ChatBotResponse createDefaultResponse() {
        ChatBotResponse response = new ChatBotResponse();
        response.setType("quick_replies");
        response.setMessage("Xin lỗi, tôi chưa hiểu rõ câu hỏi của bạn. Bạn có thể:\n\n" +
            "• Tìm kiếm sản phẩm bằng cách nhập tên sản phẩm\n" +
            "• Hỏi về danh mục (sữa, tã, đồ chơi, quần áo...)\n" +
            "• Hỏi về chính sách vận chuyển, thanh toán\n" +
            "• Hoặc liên hệ trực tiếp với chúng tôi");
        
        List<ChatBotResponse.QuickReply> quickReplies = Arrays.asList(
            new ChatBotResponse.QuickReply("Tìm sản phẩm", "Tìm sản phẩm"),
            new ChatBotResponse.QuickReply("Danh mục sản phẩm", "Danh mục"),
            new ChatBotResponse.QuickReply("Hỗ trợ", "Hỗ trợ")
        );
        response.setQuickReplies(quickReplies);
        return response;
    }
}

