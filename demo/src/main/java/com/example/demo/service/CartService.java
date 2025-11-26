package com.example.demo.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.model.CartDetail;
import com.example.demo.model.Carts;
import com.example.demo.model.Product;
import com.example.demo.model.Sizes;
import com.example.demo.model.User;
import com.example.demo.repository.CartDetailRepository;
import com.example.demo.repository.CartRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.SizesRepository;
import com.example.demo.repository.UserRepository;

@Service
public class CartService {

    @Autowired private CartRepository cartsRepo;
    @Autowired private CartDetailRepository cartDetailRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private ProductRepository productRepo;
    @Autowired private SizesRepository sizesRepo;

    /**
     * Lấy giỏ hàng của user (hoặc tạo mới nếu chưa có)
     */
    @Transactional
    public Carts findOrCreateCart(User user) {
        return cartsRepo.findByUser(user).orElseGet(() -> {
            Carts newCart = new Carts();
            newCart.setUser(user);
            return cartsRepo.save(newCart);
        });
    }

    /**
     * Thêm sản phẩm vào giỏ hàng
     */
    @Transactional
    public void addToCart(String userPhone, Integer productId, Integer sizeId, Integer quantity) {
        // 1. Lấy thông tin
        User user = userRepo.findByPhone(userPhone); //
        if (user == null) {
            throw new RuntimeException("Không tìm thấy user");
        }
        Carts cart = findOrCreateCart(user);
        Product product = productRepo.findById(productId)
            .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));
        Sizes size = sizesRepo.findById(sizeId)
            .orElseThrow(() -> new RuntimeException("Size không tồn tại"));

    
        if (quantity > size.getQuantity()) {
            throw new IllegalStateException("Số lượng tồn kho không đủ (Chỉ còn " + size.getQuantity() + ")");
        }

        Optional<CartDetail> existingItem = cartDetailRepo.findByCartsAndProductAndSizes(cart, product, size);

        if (existingItem.isPresent()) {
            // Nếu đã có: Cộng dồn số lượng
            CartDetail item = existingItem.get();
            int newQuantity = item.getQuantity() + quantity;
            // Kiểm tra lại tồn kho khi cộng dồn
            if (newQuantity > size.getQuantity()) {
                newQuantity = size.getQuantity(); // Chỉ cho phép thêm tối đa
            }
            item.setQuantity(newQuantity);
            cartDetailRepo.save(item);
        } else {
            // Nếu chưa có: Tạo mới
            CartDetail newItem = new CartDetail();
            newItem.setCarts(cart);
            newItem.setProduct(product);
            newItem.setSizes(size);
            newItem.setQuantity(quantity);
            newItem.setPrice((float) product.getDiscountedPrice());// Lưu giá ĐÃ GIẢM
            cartDetailRepo.save(newItem);
        }
    }

    /**
     * Lấy giỏ hàng để hiển thị
     */
    public Carts getCart(String userPhone) {
        User user = userRepo.findByPhone(userPhone);
        if (user == null) return null;
        
        // Trả về giỏ hàng (có thể là null nếu user chưa có giỏ)
        return cartsRepo.findByUser(user).orElse(null);
    }

    /**
     * Tính tổng tiền (Constraint 1)
     */
    public float calculateTotal(Carts cart) {
        if (cart == null || cart.getCartDetails() == null) {
            return 0;
        }
        float total = 0;
        for (CartDetail item : cart.getCartDetails()) {
            total += (item.getPrice() * item.getQuantity());
        }
        return total;
    }
    @Transactional
    public void updateQuantity(Integer cartDetailId, Integer newQuantity) {
        if (newQuantity < 1) {
             throw new IllegalArgumentException("Số lượng không thể nhỏ hơn 1.");
        }
        
        CartDetail item = cartDetailRepo.findById(cartDetailId)
            .orElseThrow(() -> new RuntimeException("Chi tiết giỏ hàng không tồn tại."));
        
        Sizes size = item.getSizes();
        
        // KIỂM TRA TỒN KHO
        if (newQuantity > size.getQuantity()) {
            throw new IllegalStateException("Số lượng tồn kho không đủ (Chỉ còn " + size.getQuantity() + ")");
        }
        
        // Lưu số lượng mới
        item.setQuantity(newQuantity);
        cartDetailRepo.save(item);
    }

    @Transactional
    public void deleteItem(Integer cartDetailId) {
        // Chỉ cần xóa item khỏi repository
        cartDetailRepo.deleteById(cartDetailId);
    }

    public int countCartItems(String userPhone) {
        User user = userRepo.findByPhone(userPhone);
        if (user == null) {
            return 0;
        }
        
        Optional<Carts> cartOptional = cartsRepo.findByUser(user);
        
        if (cartOptional.isPresent() && cartOptional.get().getCartDetails() != null) {
            // Trả về số lượng các dòng sản phẩm (CartDetail)
            return cartOptional.get().getCartDetails().size();
        }
        
        return 0;
    }
    public int calculateItemCount(Carts cart) {
        if (cart == null || cart.getCartDetails() == null) {
            return 0;
        }
        // Dùng stream để tính tổng trường quantity trong CartDetail
        return cart.getCartDetails().stream()
                .mapToInt(CartDetail::getQuantity)
                .sum();
    }
}