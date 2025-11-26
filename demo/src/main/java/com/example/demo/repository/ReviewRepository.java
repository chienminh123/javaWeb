package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.model.Review;

public interface ReviewRepository extends JpaRepository<Review, Integer> {
    // 1. Lấy tất cả theo sản phẩm (Mới nhất trước)
    List<Review> findByProductProductIdOrderByReviewDateDesc(Integer productId);

    // 2. Lọc theo số sao (Ví dụ: chỉ lấy 5 sao)
    List<Review> findByProductProductIdAndRatingOrderByReviewDateDesc(Integer productId, Integer rating);

    // 3. Kiểm tra quyền đánh giá (User đã mua và đơn hàng thành công)
    @Query("SELECT COUNT(od) > 0 FROM OrderDetail od " +
           "JOIN od.orders o " +
           "WHERE od.product.productId = :productId " +
           "AND o.user.userId = :userId " +
           "AND o.status = 'Giao hàng thành công'") 
    boolean hasUserBoughtProduct(@Param("userId") Integer userId, @Param("productId") Integer productId);
}