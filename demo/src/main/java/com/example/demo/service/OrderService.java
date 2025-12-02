package com.example.demo.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
    

    private static final int[][] QUANTITY_DISCOUNT_RULES = {
        {20, 15},
        {15, 12},
        {10, 10},
        {5, 5}
    };
    
    private static final double[][] SOFT_DISCOUNT_RULES = {
        {20, 15, 0.3},
        {15, 12, 0.25},
        {10, 10, 0.2},
        {5, 5, 0.15}
    };

    @Transactional
    public Orders createOrderFromCart(
        String userPhone, String address, String phone, 
        String paymentMethod, String couponCode,
        List<Integer> selectedItems
    ) throws Exception {
        
        User user = userRepo.findByPhone(userPhone);
        if (user == null) throw new Exception("User không tồn tại.");
        
        Carts cart = cartRepo.findByUser(user).orElseThrow(() -> new Exception("Giỏ hàng không tồn tại."));
        List<CartDetail> cartDetails = cart.getCartDetails();

        if (cartDetails == null || cartDetails.isEmpty()) throw new Exception("Giỏ hàng rỗng.");

        List<CartDetail> itemsToOrder;
        if (selectedItems != null && !selectedItems.isEmpty()) {
            itemsToOrder = cartDetails.stream()
                .filter(item -> selectedItems.contains(item.getCartDetailId()))
                .collect(Collectors.toList());
            
            if (itemsToOrder.isEmpty()) throw new Exception("Không có sản phẩm nào được chọn.");
        } else {
             throw new Exception("Vui lòng chọn sản phẩm để thanh toán.");
        }
    
        float tempTotal = 0;
        int totalQuantity = 0;
        for (CartDetail item : itemsToOrder) {
            tempTotal += item.getPrice() * item.getQuantity();
            totalQuantity += item.getQuantity();
        }

        double quantityDiscountAmount = calculateQuantityDiscount(totalQuantity, tempTotal);
        double subtotalAfterQuantityDiscount = tempTotal - quantityDiscountAmount;

        double couponDiscountAmount = 0;
        double finalTotal = subtotalAfterQuantityDiscount;
        if (couponCode != null && !couponCode.isEmpty()) {
            Coupon coupon = couponService.checkCoupon(couponCode);
            couponDiscountAmount = couponService.calculateDiscount(coupon, subtotalAfterQuantityDiscount);
            finalTotal = subtotalAfterQuantityDiscount - couponDiscountAmount;
            coupon.setQuantity(coupon.getQuantity() - 1);
            couponService.save(coupon);
        }
        
        double totalDiscountAmount = quantityDiscountAmount + couponDiscountAmount;

        Orders newOrder = new Orders();
        newOrder.setUser(user);
        newOrder.setOrderDate(LocalDateTime.now());
        newOrder.setStatus("Mới tạo"); 
        newOrder.setAddress(address);
        newOrder.setPhone(phone); 
        newOrder.setPaymentMethod(paymentMethod);
        newOrder.setCouponCode(couponCode);
        newOrder.setDiscountAmount(couponDiscountAmount);
        newOrder.setQuantityDiscountAmount(quantityDiscountAmount);
        newOrder.setFinalTotal(finalTotal);

        newOrder = ordersRepo.save(newOrder);

        List<OrderDetail> orderDetails = new java.util.ArrayList<>();
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
            
            cartDetailRepo.delete(item);
        }
        
        orderDetailRepo.saveAll(orderDetails);
        newOrder.setOrderDetails(orderDetails);
        
        return newOrder;
    }

   @Transactional
    public Orders updateOrderStatus(Integer orderId, String newStatus) {
        logger.info("Bắt đầu cập nhật trạng thái đơn hàng ID: {} sang '{}'", orderId, newStatus);

        Orders order = ordersRepo.findByIdWithDetails(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng ID: " + orderId));

        String oldStatus = order.getStatus();
        logger.info("Trạng thái hiện tại của đơn hàng {}: '{}'", orderId, oldStatus);

        if (oldStatus != null && oldStatus.equals(newStatus)) {
            logger.info("Trạng thái không thay đổi, bỏ qua cập nhật cho đơn hàng {}", orderId);
            return order;
        }

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

        if ("Giao hàng thành công".equals(newStatus) && (oldStatus == null || !"Giao hàng thành công".equals(oldStatus))) {
            logger.info("Xử lý cập nhật trạng thái 'Giao hàng thành công' cho đơn hàng {}", orderId);
            if (order.getUser() == null) {
                logger.error("Đơn hàng {} không có thông tin người dùng", orderId);
                throw new RuntimeException("Đơn hàng không có thông tin người dùng");
            }
            User user = userRepo.findById(order.getUser().getUserId())
                    .orElseThrow(() -> {
                        logger.error("Không tìm thấy User ID: {} cho đơn hàng {}", order.getUser().getUserId(), orderId);
                        return new RuntimeException("User không tồn tại");
                    });

            double orderValue;
            if (order.getFinalTotal() != null && order.getFinalTotal() > 0) {
                orderValue = order.getFinalTotal();
            } else {
                orderValue = getTotalPrice(order);
            }

            long pointsEarned = (long) (orderValue * 0.1); 
            
            logger.info("Tính điểm cho đơn hàng {}: giá trị đơn hàng = {}, điểm sẽ được cộng = {}", orderId, orderValue, pointsEarned);
            
            if (pointsEarned > 0) {
                Long currentPointsObj = user.getPoints();
                long currentPoints = (currentPointsObj != null) ? currentPointsObj : 0L;
                long newPoints = currentPoints + pointsEarned;
                
                user.setPoints(newPoints);
                logger.info("Cập nhật điểm cho User ID: {} từ {} lên {}", user.getUserId(), currentPoints, newPoints);
                
                checkAndUpgradeRank(user);
                userRepo.save(user);
                
                logger.info("Đã cộng {} điểm cho User ID: {} (từ {} điểm lên {} điểm)", 
                    pointsEarned, user.getUserId(), currentPoints, newPoints);
            } else {
                logger.warn("Không cộng điểm cho đơn hàng {} vì giá trị đơn hàng quá thấp: {}", orderId, orderValue);
            }
        }

        order.setStatus(newStatus);
        logger.info("Đang lưu đơn hàng {} với trạng thái mới: '{}'", orderId, newStatus);
        Orders updatedOrder = ordersRepo.save(order);
        logger.info("Đã cập nhật thành công trạng thái đơn hàng {} từ '{}' sang '{}'", orderId, oldStatus, newStatus);
    
        try {
            if(newStatus.equals("Đã trả hàng")) emailService.sendUserReturnNotification(updatedOrder);
            emailService.sendOrderStatusUpdate(updatedOrder, newStatus);
        } catch (MessagingException e) { logger.error("Lỗi email", e); }
        
        return updatedOrder;
    }

    private void checkAndUpgradeRank(User user) {
        String oldRank = user.getRank();
        String newRank = null;
        Coupon rewardCoupon = null;

        Long pointsObj = user.getPoints();
        long pts = (pointsObj != null) ? pointsObj : 0L;

        if (pts >= RANK_DIAMOND) {
            newRank = "DIAMOND";
        } else if (pts >= RANK_GOLD) {
            newRank = "GOLD";
        } else if (pts >= RANK_SILVER) {
            newRank = "SILVER";
        } else {
            newRank = oldRank; // Giữ nguyên rank cũ nếu chưa đủ điểm
        }

        // So sánh an toàn với null
        boolean rankChanged = (oldRank == null && newRank != null) || 
                            (oldRank != null && !oldRank.equals(newRank));

        if (rankChanged && newRank != null) {
            user.setRank(newRank);
            logger.info("User ID: {} đã được nâng cấp rank từ '{}' sang '{}'", user.getUserId(), oldRank, newRank);
            try {
                if ("SILVER".equals(newRank)) {
                    rewardCoupon = createRankUpCoupon(user, "SILVER", 50_000.0);
                } else if ("GOLD".equals(newRank)) {
                    rewardCoupon = createRankUpCoupon(user, "GOLD", 100_000.0);
                } else if ("DIAMOND".equals(newRank)) {
                    rewardCoupon = createRankUpCoupon(user, "DIAMOND", 200_000.0);
                }
                
                if (rewardCoupon != null) {
                    emailService.sendRankUpEmail(user, newRank, rewardCoupon);
                }
            } catch (Exception e) { 
                logger.error("Lỗi tạo quà tặng rank cho User ID: " + user.getUserId(), e); 
            }
        }
    }

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
    public double calculateQuantityDiscount(int totalQuantity, double orderTotal) {
        if (totalQuantity <= 0 || orderTotal <= 0) {
            return 0.0;
        }
        
        double bestDiscount = 0.0;
        double bestDiscountPercent = 0.0;
        double[] bestRule = null;
        
        for (double[] softRule : SOFT_DISCOUNT_RULES) {
            double baseQuantity = softRule[0];
            double baseDiscountPercent = softRule[1];
            double additionalDiscountPerItem = softRule[2];
            
            if (totalQuantity >= baseQuantity) {
                double extraItems = totalQuantity - baseQuantity;
                double totalDiscountPercent = baseDiscountPercent + (extraItems * additionalDiscountPerItem);
                
                totalDiscountPercent = Math.min(totalDiscountPercent, 30.0);
                
                double discount = orderTotal * (totalDiscountPercent / 100.0);
                
                if (discount > bestDiscount) {
                    bestDiscount = discount;
                    bestDiscountPercent = totalDiscountPercent;
                    bestRule = softRule;
                }
            }
        }
        
        if (bestDiscount == 0.0) {
            for (int[] rule : QUANTITY_DISCOUNT_RULES) {
                int minQuantity = rule[0];
                int discountPercent = rule[1];
                
                if (totalQuantity >= minQuantity) {
                    bestDiscount = orderTotal * (discountPercent / 100.0);
                    bestDiscountPercent = discountPercent;
                    logger.info("Áp dụng giảm giá số lượng (cố định): {} sản phẩm -> giảm {}% = {} VNĐ", 
                        totalQuantity, discountPercent, bestDiscount);
                    return bestDiscount;
                }
            }
        } else {
            double baseQuantity = bestRule[0];
            double extraItems = totalQuantity - baseQuantity;
            logger.info("Áp dụng giảm giá mềm: {} sản phẩm (cơ sở: {}, vượt: {}) -> giảm {}% = {} VNĐ", 
                totalQuantity, (int)baseQuantity, (int)extraItems, 
                String.format("%.2f", bestDiscountPercent), String.format("%.0f", bestDiscount));
        }
        
        return bestDiscount;
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