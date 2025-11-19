package com.example.demo.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.model.Coupon;
import com.example.demo.repository.CouponRepository;

@Service
public class CouponService {
    @Autowired private CouponRepository couponRepo;

    public List<Coupon> findAll() { return couponRepo.findAll(); }

    public Coupon save(Coupon coupon) { return couponRepo.save(coupon); }
    
    public void delete(Integer id) { couponRepo.deleteById(id); }

    // Validate mã giảm giá
    public Coupon checkCoupon(String code) throws Exception {
        Coupon coupon = couponRepo.findByCode(code)
            .orElseThrow(() -> new Exception("Mã giảm giá không tồn tại!"));

        if (!coupon.isActive()) throw new Exception("Mã này đang bị khóa!");
        if (coupon.getQuantity() <= 0) throw new Exception("Mã này đã hết lượt sử dụng!");
        
        LocalDate now = LocalDate.now();
        if (now.isBefore(coupon.getStartDate()) || now.isAfter(coupon.getEndDate())) {
            throw new Exception("Mã giảm giá chưa bắt đầu hoặc đã hết hạn!");
        }
        return coupon;
    }
    
    public double calculateDiscount(Coupon coupon, double orderTotal) {
        double discount = 0.0;

        if ("FIXED".equals(coupon.getDiscountType())) {
            // 1. GIẢM TIỀN MẶT (VD: 50.000)
            discount = coupon.getDiscountValue();
        } else {
            // 2. GIẢM PHẦN TRĂM (VD: 10%)
            discount = orderTotal * (coupon.getDiscountValue() / 100.0);
            
            // Kiểm tra giảm tối đa (nếu có set giới hạn)
            if (coupon.getMaxDiscountAmount() != null && coupon.getMaxDiscountAmount() > 0) {
                if (discount > coupon.getMaxDiscountAmount()) {
                    discount = coupon.getMaxDiscountAmount();
                }
            }
        }
        return Math.min(discount, orderTotal);
    }
}