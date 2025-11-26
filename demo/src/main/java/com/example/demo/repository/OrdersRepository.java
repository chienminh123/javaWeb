package com.example.demo.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Orders;
import com.example.demo.model.Product;

@Repository
public interface OrdersRepository extends JpaRepository<Orders, Integer> {
    List<Orders> findByUserPhoneOrderByOrderDateDesc(String userPhone);
    List<Orders> findFirst5ByUserPhoneOrderByOrderDateDesc(String userPhone);
    long countByStatus(String status);
    
    @Query("SELECT o FROM Orders o JOIN FETCH o.orderDetails od JOIN FETCH o.user u ORDER BY o.orderDate DESC")
    List<Orders> findAllWithDetailsOrderByOrderDateDesc();
    
    /**
     * Lấy đơn hàng theo Status, tải đồng thời orderDetails và user.
     */
    @Query("SELECT o FROM Orders o JOIN FETCH o.orderDetails od JOIN FETCH o.user u WHERE o.status = :status ORDER BY o.orderDate DESC")
    List<Orders> findByStatusWithDetailsOrderByOrderDateDesc(@Param("status") String status);
    
    @Query("SELECT o FROM Orders o JOIN FETCH o.orderDetails od JOIN FETCH o.user u JOIN FETCH od.sizes s JOIN FETCH od.product p WHERE o.orderId = :orderId")
    Optional<Orders> findByIdWithDetails(@Param("orderId") Integer orderId);

    @Query("SELECT o FROM Orders o JOIN FETCH o.orderDetails od JOIN FETCH o.user u " +
       "WHERE o.status = :status " +
       "AND o.orderDate BETWEEN :startDate AND :endDate " +
       "ORDER BY o.orderDate DESC")
    List<Orders> findByStatusAndDateRange(
        @Param("status") String status, 
        @Param("startDate") LocalDateTime startDate, 
        @Param("endDate") LocalDateTime endDate);

    
}
