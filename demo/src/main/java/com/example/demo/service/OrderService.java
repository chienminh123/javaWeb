
package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.model.CartDetail;
import com.example.demo.model.Carts;
import com.example.demo.model.OrderDetail;
import com.example.demo.model.Orders;
import com.example.demo.model.Sizes;
import com.example.demo.model.User;
import com.example.demo.repository.CartDetailRepository;
import com.example.demo.repository.CartRepository;
import com.example.demo.repository.OrderDetailRepository;
import com.example.demo.repository.OrdersRepository;
import com.example.demo.repository.SizesRepository;
import com.example.demo.repository.UserRepository;

import jakarta.mail.MessagingException;

@Service
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
    
    @Autowired private EmailService emailService; // Để gửi email thông báo
    @Autowired private OrdersRepository ordersRepo;
    @Autowired private OrderDetailRepository orderDetailRepo;
    @Autowired private CartRepository cartRepo;
    @Autowired private CartDetailRepository cartDetailRepo;
    @Autowired private SizesRepository sizesRepo;
    @Autowired private UserRepository userRepo; 
    
   @Transactional
    public Orders createOrderFromCart(String userPhone, String address, String phone, String paymentMethod) throws Exception {
        
        User user = userRepo.findByPhone(userPhone);
        if (user == null) {
            throw new Exception("Không tìm thấy thông tin người dùng.");
        }
        
        Carts cart = cartRepo.findByUser(user)
                          .orElseThrow(() -> new Exception("Giỏ hàng không tồn tại."));
                          
        List<CartDetail> cartDetails = cart.getCartDetails();

        if (cartDetails == null || cartDetails.isEmpty()) {
            throw new Exception("Giỏ hàng rỗng, không thể tạo đơn hàng.");
        }

        // 2. TẠO VÀ LƯU ORDERS
        Orders newOrder = new Orders();
        newOrder.setUser(user);
        newOrder.setOrderDate(LocalDateTime.now());
        
        // [SỬA ĐỔI] Gán trạng thái mặc định. Controller sẽ quyết định trạng thái cuối cùng.
        newOrder.setStatus("Mới tạo"); 
        
        newOrder.setAddress(address);
        newOrder.setPhone(phone); 
        newOrder.setPaymentMethod(paymentMethod);
        
        newOrder = ordersRepo.save(newOrder);
        List<OrderDetail> orderDetails = new java.util.ArrayList<>();
        for (CartDetail item : cartDetails) {
            Sizes size = item.getSizes();
            Integer orderedQuantity = item.getQuantity();
            
            if (size.getQuantity() == null || size.getQuantity() < orderedQuantity) {
                String errorMsg = "Sản phẩm " + item.getProduct().getProductName() + " (Size: " + size.getSizeName() + ") chỉ còn " + (size.getQuantity() != null ? size.getQuantity() : 0) + " sản phẩm trong kho.";
                throw new Exception(errorMsg + ". Đặt hàng thất bại.");
            }
            
            OrderDetail detail = new OrderDetail();
            detail.setOrders(newOrder);
            detail.setProduct(item.getProduct());
            detail.setSizes(size);
            detail.setQuantity(orderedQuantity);
            detail.setPrice((double)item.getPrice()); 
            orderDetails.add(detail);
            
            size.setQuantity(size.getQuantity() - orderedQuantity);
            sizesRepo.save(size); 
            
            cartDetailRepo.delete(item);
        }
        
        orderDetailRepo.saveAll(orderDetails);
        newOrder.setOrderDetails(orderDetails);
        
        // ... (Logic xóa Cart giữ nguyên)
        user.setCarts(null); 
        userRepo.save(user); 
        cartRepo.delete(cart); 
        
        return newOrder;
    }

    /**
     * [USER & ADMIN] Tìm đơn hàng theo ID
     */
    public Optional<Orders> findOrderById(Integer orderId) {
        return ordersRepo.findById(orderId);
    }
    

    /**
     * [ADMIN] Cập nhật trạng thái (Gửi thông báo ngược lại User)
     */
   @Transactional
    public Orders updateOrderStatus(Integer orderId, String newStatus) {
        Orders order = ordersRepo.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng ID: " + orderId));
        
        String oldStatus = order.getStatus();

        if (oldStatus.equals(newStatus)) {
            return order; // Không thay đổi gì, không chạy logic bên dưới
        }
        
        // === [LOGIC HOÀN KHO MỚI] ===
        
        // 1. Các trạng thái đã bị trừ kho (cần hoàn lại nếu hủy)
        List<String> stockOutStates = List.of(
            "Đang xử lý", 
            "Đã xác nhận", 
            "Đang giao hàng", 
            "Đã giao hàng",
            "Đã thanh toán (VNPay)",
            "Chờ thanh toán VNPay"
        );

        // 2. Các trạng thái kích hoạt việc hoàn kho
        List<String> restoreTriggerStates = List.of("Đã hủy", "Đã trả hàng");

        // 3. Kiểm tra sự thay đổi (transition)
        boolean isTriggeringRestore = restoreTriggerStates.contains(newStatus);
        boolean wasStockOut = stockOutStates.contains(oldStatus);

        // CHỈ hoàn kho nếu:
        // Trạng thái MỚI là Hủy/Trả HÀNG VÀ trạng thái CŨ là một trạng thái ĐÃ TRỪ KHO
        if (isTriggeringRestore && wasStockOut) {
            try {
                restoreStockForOrder(order);
                logger.info("Đã hoàn kho thành công cho Đơn hàng #{}", orderId);
            } catch (Exception e) {
                logger.error("LỖI nghiêm trọng: Không thể hoàn kho cho Đơn hàng #{}. Lỗi: {}", orderId, e.getMessage());
                // (Nếu muốn, bạn có thể ném lỗi ở đây để ngăn việc đổi trạng thái nếu hoàn kho thất bại)
            }
        }
        // ============================

        // Cập nhật trạng thái mới
        order.setStatus(newStatus);
        Orders updatedOrder = ordersRepo.save(order);
        
        try {
            if (newStatus.equals("Đã trả hàng")) {
                // Nếu là USER TRẢ HÀNG, gửi thông báo đặc biệt cho Admin
                emailService.sendUserReturnNotification(updatedOrder); 
            }
            
            // Gửi thông báo cập nhật trạng thái chung cho User (kể cả khi trả hàng)
            emailService.sendOrderStatusUpdate(updatedOrder, newStatus);
            
        } catch (MessagingException e) {
            logger.error("Lỗi gửi email cập nhật trạng thái đơn hàng #{}: {}", orderId, e.getMessage());
        }
        
        return updatedOrder;
    }

    @Transactional
    protected void restoreStockForOrder(Orders order) throws Exception {
        // Tải lại đơn hàng và chi tiết của nó (vì 'order' có thể chưa load)
        Orders orderWithDetails = ordersRepo.findByIdWithDetails(order.getOrderId())
            .orElseThrow(() -> new Exception("Không tìm thấy chi tiết đơn hàng # " + order.getOrderId()));

        if (orderWithDetails.getOrderDetails() == null || orderWithDetails.getOrderDetails().isEmpty()) {
            throw new Exception("Đơn hàng không có chi tiết sản phẩm để hoàn kho.");
        }

        for (OrderDetail detail : orderWithDetails.getOrderDetails()) {
            Sizes size = detail.getSizes();
            Integer returnedQuantity = detail.getQuantity();
            
            if (size == null) {
                logger.warn("Bỏ qua hoàn kho: Không tìm thấy 'Sizes' cho OrderDetail ID: {}", detail.getOrderDetailId());
                continue;
            }
            
            int currentStock = size.getQuantity() != null ? size.getQuantity() : 0;
            size.setQuantity(currentStock + returnedQuantity);
            
            sizesRepo.save(size);
        }
    }
    /**
     * [ADMIN] Đếm số đơn hàng theo trạng thái
     */
    public long countOrdersByStatus(String status) {
        return ordersRepo.countByStatus(status);
    }
    
    //[ADMIN] Lấy danh sách đơn hàng theo trạng thái

    public List<Orders> findAllOrderByOrderDateDesc() {
        return ordersRepo.findAllWithDetailsOrderByOrderDateDesc();
    }
    
    /**
     * [ADMIN] Lấy danh sách đơn hàng theo trạng thái 
     */
    public List<Orders> findOrdersByStatus(String status) {
        return ordersRepo.findByStatusWithDetailsOrderByOrderDateDesc(status); 
    }
    public Optional<Orders> findOrderDetailsById(Integer orderId) {
        return ordersRepo.findByIdWithDetails(orderId); 
    }
    @Transactional
    public Orders save(Orders order) {
        return ordersRepo.save(order);
    }
}