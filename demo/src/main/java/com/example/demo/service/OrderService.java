// package com.example.demo.service;

// import java.time.LocalDateTime;
// import java.util.List;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional;

// import com.example.demo.model.CartDetail;
// import com.example.demo.model.Carts;
// import com.example.demo.model.OrderDetail;
// import com.example.demo.model.Orders;
// import com.example.demo.model.Sizes;
// import com.example.demo.model.User;
// import com.example.demo.repository.CartDetailRepository;
// import com.example.demo.repository.CartRepository;
// import com.example.demo.repository.OrderDetailRepository;
// import com.example.demo.repository.OrdersRepository;
// import com.example.demo.repository.SizesRepository;
// import com.example.demo.repository.UserRepository;
// @Service
// public class OrderService {

//     @Autowired private OrdersRepository ordersRepo;
//     @Autowired private OrderDetailRepository orderDetailRepo;
//     @Autowired private CartRepository cartRepo;
//     @Autowired private CartDetailRepository cartDetailRepo;
//     @Autowired private SizesRepository sizesRepo;
//     @Autowired private UserRepository userRepo; // Cần để lấy đối tượng User

//     /**
//      * Tạo đơn hàng mới từ giỏ hàng: Lưu Orders, OrderDetail, Giảm tồn kho Sizes, Xóa CartDetail và Carts.
//      */
//     @Transactional
//     public Orders createOrderFromCart(String userPhone, String address, String phone, String paymentMethod) throws Exception {
        
//         // 1. Lấy User và Cart
//         User user = userRepo.findByPhone(userPhone);
//         if (user == null) {
//             throw new Exception("Không tìm thấy thông tin người dùng.");
//         }
        
//         Carts cart = cartRepo.findByUser(user)
//                           .orElseThrow(() -> new Exception("Giỏ hàng không tồn tại."));
                          
//         List<CartDetail> cartDetails = cart.getCartDetails();

//         if (cartDetails == null || cartDetails.isEmpty()) {
//             throw new Exception("Giỏ hàng rỗng, không thể tạo đơn hàng.");
//         }

//         // 2. Tạo đối tượng Orders
//         Orders newOrder = new Orders();
//         newOrder.setUser(user);
//         newOrder.setOrderDate(LocalDateTime.now());
//         newOrder.setStatus("Đang xử lý"); // Trạng thái ban đầu
//         newOrder.setAddress(address);
//         newOrder.setPhone(phone); 
//         newOrder.setPaymentMethod(paymentMethod);
        
//         // Lưu Orders trước để lấy orderId
//         newOrder = ordersRepo.save(newOrder);

//         // Chuẩn bị lưu OrderDetail
//         List<OrderDetail> orderDetails = new java.util.ArrayList<>();
        
//         // 3. Xử lý từng sản phẩm trong giỏ
//         for (CartDetail item : cartDetails) {
//             Sizes size = item.getSizes();
//             Integer orderedQuantity = item.getQuantity();
            
//             // 3a. Kiểm tra tồn kho và NÉM LỖI nếu không đủ
//             if (size.getQuantity() == null || size.getQuantity() < orderedQuantity) {
//                 // Transaction sẽ được rollback (hủy) ngay lập tức
//                 throw new Exception("Sản phẩm " + item.getProduct().getProductName() + " (Size: " + size.getSizeName() + ") chỉ còn " + (size.getQuantity() != null ? size.getQuantity() : 0) + " sản phẩm trong kho. Đặt hàng thất bại.");
//             }
            
//             // 3b. Tạo OrderDetail và thêm vào danh sách
//             OrderDetail detail = new OrderDetail();
//             detail.setOrders(newOrder);
//             detail.setProduct(item.getProduct());
//             detail.setSizes(size);
//             detail.setQuantity(orderedQuantity);
//             detail.setPrice((double)item.getPrice()); 
//             orderDetails.add(detail);
            
//             // 3c. Giảm tồn kho Sizes và lưu lại
//             size.setQuantity(size.getQuantity() - orderedQuantity);
//             sizesRepo.save(size); 
            
//             // 3d. Xóa CartDetail đã được đặt hàng
//             cartDetailRepo.delete(item);
//         }
        
//         // 4. Lưu tất cả OrderDetail
//         orderDetailRepo.saveAll(orderDetails);
        
//         // 5. Xóa đối tượng Carts chính (vì tất cả CartDetail đã bị xóa)
//         cartRepo.delete(cart); 
        
//         return newOrder;
//     }
// }
package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

@Service
public class OrderService {

    @Autowired private OrdersRepository ordersRepo;
    @Autowired private OrderDetailRepository orderDetailRepo;
    @Autowired private CartRepository cartRepo;
    @Autowired private CartDetailRepository cartDetailRepo;
    @Autowired private SizesRepository sizesRepo;
    @Autowired private UserRepository userRepo; // Cần để lấy đối tượng User và lưu User đã sửa đổi

    /**
     * Tạo đơn hàng mới từ giỏ hàng.
     * FIX: Xử lý TransientObjectException bằng cách ngắt liên kết User->Carts trước khi xóa.
     */
    @Transactional
    public Orders createOrderFromCart(String userPhone, String address, String phone, String paymentMethod) throws Exception {
        
        // 1. Lấy User và Cart
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
        // Trạng thái: Chờ thanh toán nếu là Chuyển khoản, ngược lại là Đang xử lý (COD)
        newOrder.setStatus("Chuyển khoản".equals(paymentMethod) ? "Chờ thanh toán" : "Đang xử lý");
        newOrder.setAddress(address);
        newOrder.setPhone(phone); 
        newOrder.setPaymentMethod(paymentMethod);
        
        newOrder = ordersRepo.save(newOrder);

        // Chuẩn bị lưu OrderDetail
        List<OrderDetail> orderDetails = new java.util.ArrayList<>();
        
        // 3. Xử lý từng sản phẩm trong giỏ (tạo OrderDetail, giảm tồn kho, xóa CartDetail)
        for (CartDetail item : cartDetails) {
            Sizes size = item.getSizes();
            Integer orderedQuantity = item.getQuantity();
            
            // 3a. Kiểm tra tồn kho và NÉM LỖI
            if (size.getQuantity() == null || size.getQuantity() < orderedQuantity) {
                String errorMsg = "Sản phẩm " + item.getProduct().getProductName() + " (Size: " + size.getSizeName() + ") chỉ còn " + (size.getQuantity() != null ? size.getQuantity() : 0) + " sản phẩm trong kho.";
                
                // === BỔ SUNG: GỬI EMAIL/THÔNG BÁO HẾT HÀNG CHO ADMIN ===
                // Bạn cần inject EmailService hoặc NotificationService vào OrderService
                // emailService.sendOutOfStockNotification(item.getProduct(), size, orderedQuantity); 
                // =======================================================
                
                throw new Exception(errorMsg + ". Đặt hàng thất bại.");
            }
            
            // 3b. Tạo OrderDetail
            OrderDetail detail = new OrderDetail();
            detail.setOrders(newOrder);
            detail.setProduct(item.getProduct());
            detail.setSizes(size);
            detail.setQuantity(orderedQuantity);
            detail.setPrice((double)item.getPrice()); 
            orderDetails.add(detail);
            
            // 3c. Giảm tồn kho Sizes và lưu lại
            size.setQuantity(size.getQuantity() - orderedQuantity);
            sizesRepo.save(size); 
            
            // 3d. Xóa CartDetail đã được đặt hàng
            cartDetailRepo.delete(item);
        }
        
        // 4. LƯU TẤT CẢ ORDER DETAIL
        orderDetailRepo.saveAll(orderDetails);
        newOrder.setOrderDetails(orderDetails);
        user.setCarts(null); 
        userRepo.save(user); 
        
        // Xóa đối tượng Carts chính (vì tất cả CartDetail đã bị xóa)
        cartRepo.delete(cart); 
        
        // 6. Hoàn tất và trả về
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
        
        if (!order.getStatus().equals(newStatus)) {
        order.setStatus(newStatus);
        Orders updatedOrder = ordersRepo.save(order);
        
        // === BỔ SUNG: GỬI EMAIL THÔNG BÁO CHO USER ===
        // Bạn cần tạo hàm sendStatusUpdate(updatedOrder) trong EmailService.java
        // try {
        //     emailService.sendStatusUpdate(updatedOrder); 
        // } catch (MessagingException e) {
        //     logger.error("Lỗi gửi email cập nhật trạng thái", e);
        // }
        // ===========================================
        
        return updatedOrder;
    }
    return order;
    }
    /**
     * [ADMIN] Đếm số đơn hàng theo trạng thái
     */
    public long countOrdersByStatus(String status) {
        return ordersRepo.countByStatus(status);
    }
    
    // /**
    //  * [ADMIN] Lấy danh sách đơn hàng theo trạng thái
    //  */
    // public List<Orders> findOrdersByStatus(String status) {
    //     return ordersRepo.findByStatusOrderByOrderDateDesc(status);
    // }
    public List<Orders> findAllOrderByOrderDateDesc() {
        // THAY THẾ: return ordersRepo.findAll(Sort.by(Sort.Direction.DESC, "orderDate"));
        return ordersRepo.findAllWithDetailsOrderByOrderDateDesc(); // <--- ĐÃ SỬA DÙNG JOIN FETCH
    }
    
    /**
     * [ADMIN] Lấy danh sách đơn hàng theo trạng thái (Hàm này có thể thiếu, nên thêm vào)
     */
    public List<Orders> findOrdersByStatus(String status) {
        // THAY THẾ: return ordersRepo.findByStatusOrderByOrderDateDesc(status); (Nếu bạn có hàm này)
        return ordersRepo.findByStatusWithDetailsOrderByOrderDateDesc(status); // <--- ĐÃ SỬA DÙNG JOIN FETCH
    }
}