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
import com.example.demo.model.Product;

@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetail, Integer> {


    @Query("SELECT new com.example.demo.dto.TopProductIdDTO(od.product.productId, SUM(od.quantity)) " +
           "FROM OrderDetail od JOIN od.orders o " +
           "WHERE o.orderDate BETWEEN :startDate AND :endDate " +
           "GROUP BY od.product.productId " + 
           "ORDER BY SUM(od.quantity) DESC")
    List<TopProductIdDTO> findTopSellingProducts(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

 
   @Query("SELECT FUNCTION('DATE', o.orderDate), SUM(COALESCE(o.finalTotal, 0)) " +
       "FROM Orders o " +
       "WHERE (o.orderDate BETWEEN :startDate AND :endDate) " +
       "AND o.status IN ('Giao hàng thành công') " + 
       "GROUP BY FUNCTION('DATE', o.orderDate) " +
       "ORDER BY FUNCTION('DATE', o.orderDate) ASC")
    List<Object[]> findRevenueByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

        boolean existsByProduct(Product product);
}