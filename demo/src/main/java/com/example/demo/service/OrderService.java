package com.example.demo.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors; // Thêm import này

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.model.CartDetail;
import com.example.demo.model.Carts;
import com.example.demo.model.Coupon;
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
    
    @Autowired private EmailService emailService; 
    @Autowired private OrdersRepository ordersRepo;
    @Autowired private OrderDetailRepository orderDetailRepo;
    @Autowired private CartRepository cartRepo;
    @Autowired private CartDetailRepository cartDetailRepo;
    @Autowired private SizesRepository sizesRepo;
    @Autowired private UserRepository userRepo; 
    @Autowired private ProductService productService;
    @Autowired private CouponService couponService;

    private static final long RANK_SILVER = 100_000;  
    private static final long RANK_GOLD = 500_000;    
    private static final long RANK_DIAMOND = 1_000_000;

    @Transactional
    public Orders createOrderFromCart(
        String userPhone, String address, String phone, 
        String paymentMethod, String couponCode,
        List<Integer> selectedItems // Nhận list item
    ) throws Exception {
        
        User user = userRepo.findByPhone(userPhone);
        if (user == null) throw new Exception("User không tồn tại.");
        
        Carts cart = cartRepo.findByUser(user).orElseThrow(() -> new Exception("Giỏ hàng không tồn tại."));
        List<CartDetail> cartDetails = cart.getCartDetails();

        if (cartDetails == null || cartDetails.isEmpty()) throw new Exception("Giỏ hàng rỗng.");

        // === [LỌC SẢN PHẨM] ===
        List<CartDetail> itemsToOrder;
        if (selectedItems != null && !selectedItems.isEmpty()) {
            itemsToOrder = cartDetails.stream()
                .filter(item -> selectedItems.contains(item.getCartDetailId()))
                .collect(Collectors.toList());
            
            if (itemsToOrder.isEmpty()) throw new Exception("Không có sản phẩm nào được chọn.");
        } else {
             // Nếu không chọn gì, chặn lại
             throw new Exception("Vui lòng chọn sản phẩm để thanh toán.");
        }
    
        // Tính tổng tiền trên danh sách đã lọc
        float tempTotal = 0;
        for (CartDetail item : itemsToOrder) {
            tempTotal += item.getPrice() * item.getQuantity();
        }

        double discountAmount = 0;
        double finalTotal = tempTotal;
        if (couponCode != null && !couponCode.isEmpty()) {
            Coupon coupon = couponService.checkCoupon(couponCode);
            discountAmount = couponService.calculateDiscount(coupon, tempTotal);
            finalTotal = tempTotal - discountAmount;
            coupon.setQuantity(coupon.getQuantity() - 1);
            couponService.save(coupon);
        }

        Orders newOrder = new Orders();
        newOrder.setUser(user);
        newOrder.setOrderDate(LocalDateTime.now());
        newOrder.setStatus("Mới tạo"); 
        newOrder.setAddress(address);
        newOrder.setPhone(phone); 
        newOrder.setPaymentMethod(paymentMethod);
        newOrder.setCouponCode(couponCode);
        newOrder.setDiscountAmount(discountAmount);
        newOrder.setFinalTotal(finalTotal);

        newOrder = ordersRepo.save(newOrder);

        List<OrderDetail> orderDetails = new java.util.ArrayList<>();
        // Duyệt qua danh sách ĐÃ LỌC
        for (CartDetail item : itemsToOrder) {
            Sizes size = item.getSizes();
            Integer orderedQuantity = item.getQuantity();
            
            if (size.getQuantity() == null || size.getQuantity() < orderedQuantity) {
                throw new Exception("Hết hàng: " + item.getProduct().getProductName());
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
            
            cartDetailRepo.delete(item); // Xóa khỏi giỏ
        }
        
        orderDetailRepo.saveAll(orderDetails);
        newOrder.setOrderDetails(orderDetails);
        
        return newOrder;
    }

   @Transactional
    public Orders updateOrderStatus(Integer orderId, String newStatus) {

        Orders order = ordersRepo.findByIdWithDetails(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng ID: " + orderId));

        String oldStatus = order.getStatus();

        if (oldStatus.equals(newStatus)) return order;

        List<String> stockOutStates = List.of(
            "Mới tạo", 
            "Chờ thanh toán", 
            "Đang xử lý", 
            "Đã xác nhận", 
            "Đang giao hàng", 
            "Giao hàng thành công", 
            "Đã thanh toán VNPay", 
            "Chờ thanh toán VNPay"
        );
        
        List<String> restoreTriggerStates = List.of("Đã hủy", "Đã trả hàng");

        if (restoreTriggerStates.contains(newStatus) && stockOutStates.contains(oldStatus)) {
            try { 
                restoreStockForOrder(order); 
            } catch (Exception e) { 
                logger.error("Lỗi hoàn kho cho đơn hàng " + orderId, e); 
            }
        }

        if ("Giao hàng thành công".equals(newStatus) && !"Giao hàng thành công".equals(oldStatus)) {
            User user = userRepo.findById(order.getUser().getUserId())
                    .orElseThrow(() -> new RuntimeException("User không tồn tại"));

            // Tính giá trị đơn hàng
            double orderValue;
            if (order.getFinalTotal() != null && order.getFinalTotal() > 0) {
                orderValue = order.getFinalTotal();
            } else {
                orderValue = getTotalPrice(order);
            }

            long pointsEarned = (long) (orderValue * 0.1); 
            
            if (pointsEarned > 0) {
                long currentPoints = user.getPoints();
                user.setPoints(currentPoints + pointsEarned);
                checkAndUpgradeRank(user);
                userRepo.save(user);
                
                logger.info("Đã cộng " + pointsEarned + " điểm cho User ID: " + user.getUserId());
            }
        }

        order.setStatus(newStatus);
        Orders updatedOrder = ordersRepo.save(order);
    
        try {
            if(newStatus.equals("Đã trả hàng")) emailService.sendUserReturnNotification(updatedOrder);
            emailService.sendOrderStatusUpdate(updatedOrder, newStatus);
        } catch (MessagingException e) { logger.error("Lỗi email", e); }
        
        return updatedOrder;
    }

    private void checkAndUpgradeRank(User user) {
        String oldRank = user.getRank();
        String newRank = oldRank;
        Coupon rewardCoupon = null;

        // `points` is a primitive long; no null check needed
        long pts = user.getPoints();

        if (pts >= RANK_DIAMOND) newRank = "DIAMOND";
        else if (pts >= RANK_GOLD) newRank = "GOLD";
        else if (pts >= RANK_SILVER) newRank = "SILVER";

        if (!newRank.equals(oldRank)) {
            user.setRank(newRank);
            try {
                if ("SILVER".equals(newRank)) rewardCoupon = createRankUpCoupon(user, "SILVER", 50_000.0);
                else if ("GOLD".equals(newRank)) rewardCoupon = createRankUpCoupon(user, "GOLD", 100_000.0);
                else if ("DIAMOND".equals(newRank)) rewardCoupon = createRankUpCoupon(user, "DIAMOND", 200_000.0);
                
                if (rewardCoupon != null) emailService.sendRankUpEmail(user, newRank, rewardCoupon);
            } catch (Exception e) { logger.error("Lỗi quà tặng", e); }
        }
    }

    // Các hàm phụ trợ khác giữ nguyên
    private Coupon createRankUpCoupon(User user, String rankName, Double discountAmount) {
        Coupon c = new Coupon();
        c.setCode(rankName + "-" + user.getPhone() + "-" + (System.currentTimeMillis() % 10000));
        c.setDiscountType("FIXED");
        c.setDiscountValue(discountAmount);
        c.setQuantity(1);
        c.setStartDate(java.time.LocalDate.now());
        c.setEndDate(java.time.LocalDate.now().plusMonths(1));
        c.setActive(true);
        return couponService.save(c);
    }
    private double getTotalPrice(Orders order) {
        return order.getOrderDetails().stream().mapToDouble(d -> d.getPrice() * d.getQuantity()).sum();
    }
    @Transactional
    protected void restoreStockForOrder(Orders order) throws Exception {
        Orders orderWithDetails = ordersRepo.findByIdWithDetails(order.getOrderId()).orElseThrow();
        for (OrderDetail detail : orderWithDetails.getOrderDetails()) {
            Sizes size = detail.getSizes();
            if (size != null) {
                size.setQuantity((size.getQuantity() != null ? size.getQuantity() : 0) + detail.getQuantity());
                sizesRepo.save(size);
            }
        }
    }
    public Optional<Orders> findOrderById(Integer orderId) { return ordersRepo.findById(orderId); }
    public Optional<Orders> findOrderDetailsById(Integer orderId) { return ordersRepo.findByIdWithDetails(orderId); }
    @Transactional public Orders save(Orders order) { return ordersRepo.save(order); }
    public void notifyOrderSuccess(Orders order) {
        try {
            emailService.sendOrderConfirmation(order);
            emailService.sendNewOrderNotification(order);
            List<Sizes> lowStockItems = productService.checkLowStockAfterOrder(order, 5);
            if (!lowStockItems.isEmpty()) emailService.sendLowStockNotification(lowStockItems);
        } catch (MessagingException e) { logger.warn("Lỗi email", e); }
    }

    public List<Orders> findOrdersByStatusAndDate(String status, LocalDate date) {
    if (date == null) return List.of();
    LocalDateTime start = date.atStartOfDay();
    LocalDateTime end = date.atTime(java.time.LocalTime.MAX);
    
    return ordersRepo.findByStatusAndDateRange(status, start, end);
}

    
    public long countOrdersByStatus(String status) { return ordersRepo.countByStatus(status); }
    public List<Orders> findAllOrderByOrderDateDesc() { return ordersRepo.findAllWithDetailsOrderByOrderDateDesc(); }
    public List<Orders> findOrdersByStatus(String status) { return ordersRepo.findByStatusWithDetailsOrderByOrderDateDesc(status); }
}