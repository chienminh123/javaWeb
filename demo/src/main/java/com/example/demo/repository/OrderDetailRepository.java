package com.example.demo.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable; 
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.dto.TopProductIdDTO;
import com.example.demo.model.OrderDetail;

@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetail, Integer> {

    /**
     * Truy vấn Top Sản phẩm bán chạy (theo ID)
     * Dùng od.product.productId để truy cập đúng
     */
    @Query("SELECT new com.example.demo.dto.TopProductIdDTO(od.product.productId, SUM(od.quantity)) " +
           "FROM OrderDetail od JOIN od.orders o " +
           "WHERE o.orderDate BETWEEN :startDate AND :endDate " +
           "GROUP BY od.product.productId " + // Sửa cả GROUP BY
           "ORDER BY SUM(od.quantity) DESC")
    List<TopProductIdDTO> findTopSellingProducts(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    /**
     * Truy vấn Doanh thu theo ngày
     */
   @Query("SELECT FUNCTION('DATE', o.orderDate), SUM(od.quantity * od.price) " +
       "FROM OrderDetail od JOIN od.orders o " +
       "WHERE (o.orderDate BETWEEN :startDate AND :endDate) " +
       // [ĐIỀU KIỆN LỌC] Chỉ tính doanh thu cho các đơn hàng GIAO THÀNH CÔNG
       "AND o.status IN ('Hoàn thành', 'Đã giao hàng') " + 
       "GROUP BY FUNCTION('DATE', o.orderDate) " +
       "ORDER BY FUNCTION('DATE', o.orderDate) ASC")
    List<Object[]> findRevenueByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

}