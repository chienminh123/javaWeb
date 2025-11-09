package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public User findByPhone(String phone) {
        // Sử dụng hàm có sẵn của bạn
        return userRepo.findByPhone(phone); 
    }

    // Chức năng 1: Sửa thông tin
    @Transactional
    public User updateUserProfile(String phone, String email, String address) {
        User user = userRepo.findByPhone(phone);
        if (user == null) {
            throw new RuntimeException("Không tìm thấy người dùng với SĐT: " + phone);
        }
        
        user.setEmail(email);
        user.setAddress(address);
        // (Không cho sửa SĐT vì nó là ID đăng nhập)
        return userRepo.save(user);
    }

    // Chức năng 2: Đổi mật khẩu
    @Transactional
    public void changeUserPassword(String phone, String oldPassword, String newPassword) {
        User user = userRepo.findByPhone(phone);
        if (user == null) {
            throw new RuntimeException("Không tìm thấy người dùng với SĐT: " + phone);
        }

        // Kiểm tra mật khẩu cũ có khớp không
        if (!passwordEncoder.matches(oldPassword, user.getPassWord())) {
            throw new IllegalArgumentException("Mật khẩu cũ không chính xác!");
        }

        // Mã hóa và lưu mật khẩu mới
        user.setPassWord(passwordEncoder.encode(newPassword));
        userRepo.save(user);
    }
}