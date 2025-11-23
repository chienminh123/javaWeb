package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.model.Product;
import com.example.demo.model.Review;
import com.example.demo.model.User;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.ReviewRepository;
import com.example.demo.repository.UserRepository;

@Service
public class ReviewService {

    @Autowired private ReviewRepository reviewRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private ProductRepository productRepo;

    // Lấy danh sách đánh giá (Có lọc hoặc không)
    public List<Review> getReviews(Integer productId, Integer ratingFilter) {
        if (ratingFilter != null && ratingFilter > 0) {
            return reviewRepo.findByProductProductIdAndRatingOrderByReviewDateDesc(productId, ratingFilter);
        }
        return reviewRepo.findByProductProductIdOrderByReviewDateDesc(productId);
    }

    // Thêm đánh giá mới
    public void addReview(String userPhone, Integer productId, Integer rating, String comment) throws Exception {
        User user = userRepo.findByPhone(userPhone);
        if (user == null) throw new Exception("Vui lòng đăng nhập.");

        // Check quyền: Phải mua hàng thành công
        if (!reviewRepo.hasUserBoughtProduct(user.getUserId(), productId)) {
            throw new Exception("Bạn phải mua sản phẩm này mới được đánh giá.");
        }

        Product product = productRepo.findById(productId).orElseThrow();

        Review review = new Review();
        review.setUser(user);
        review.setProduct(product);
        review.setRating(rating);
        review.setComment(comment);
        review.setReviewDate(LocalDateTime.now());
        
        reviewRepo.save(review);
    }
    
    // Tính thống kê (Số lượng từng sao, TB sao)
    public Map<String, Object> getReviewStats(Integer productId) {
        List<Review> all = reviewRepo.findByProductProductIdOrderByReviewDateDesc(productId);
        Map<String, Object> stats = new HashMap<>();
        
        int total = all.size();
        if (total == 0) {
            stats.put("average", 0.0);
            stats.put("total", 0);
            stats.put("count5", 0); stats.put("count4", 0); stats.put("count3", 0); stats.put("count2", 0); stats.put("count1", 0);
            return stats;
        }

        int sum = 0;
        int c5=0, c4=0, c3=0, c2=0, c1=0;

        for (Review r : all) {
            sum += r.getRating();
            switch (r.getRating()) {
                case 5 -> c5++;
                case 4 -> c4++;
                case 3 -> c3++;
                case 2 -> c2++;
                case 1 -> c1++;
            }
        }

        stats.put("total", total);
        stats.put("average", String.format("%.1f", (double)sum / total));
        stats.put("count5", c5);
        stats.put("count4", c4);
        stats.put("count3", c3);
        stats.put("count2", c2);
        stats.put("count1", c1);
        
        return stats;
    }
}