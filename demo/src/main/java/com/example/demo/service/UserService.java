package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.UUID;

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
        return userRepo.findByPhone(phone); 
    }

    @Transactional
    public User updateUserProfile(String phone,String userName, String email, String address) {
        User user = userRepo.findByPhone(phone);
        if (user == null) {
            throw new RuntimeException("Không tìm thấy người dùng với SĐT: " + phone);
        }
        user.setUserName(userName);
        user.setEmail(email);
        user.setAddress(address);
        return userRepo.save(user);
    }

    @Transactional
    public void changeUserPassword(String phone, String oldPassword, String newPassword) {
        User user = userRepo.findByPhone(phone);
        if (user == null) {
            throw new RuntimeException("Không tìm thấy người dùng với SĐT: " + phone);
        }

        if (!passwordEncoder.matches(oldPassword, user.getPassWord())) {
            throw new IllegalArgumentException("Mật khẩu cũ không chính xác!");
        }

        user.setPassWord(passwordEncoder.encode(newPassword));
        userRepo.save(user);
    }

    @Transactional
    public void changeUserPhone(String currentPhone, String newPhone, String password) {
        User user = userRepo.findByPhone(currentPhone);
        if (user == null) throw new RuntimeException("User không tồn tại");

        if (!passwordEncoder.matches(password, user.getPassWord())) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không đúng");
        }

        if (userRepo.findByPhone(newPhone) != null) {
            throw new IllegalArgumentException("Số điện thoại này đã được sử dụng bởi tài khoản khác");
        }

        user.setPhone(newPhone);
        userRepo.save(user);
    }

    @Transactional
    public String generateResetToken(String email) {
        User user = userRepo.findByEmail(email);
        if (user == null) {
            throw new RuntimeException("Không tìm thấy tài khoản với email này.");
        }

        String token = UUID.randomUUID().toString();

        user.setResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(15));
        userRepo.save(user);

        return token;
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        User user = userRepo.findByResetToken(token);
        
        if (user == null) {
            throw new RuntimeException("Token không hợp lệ.");
        }
        
        if (user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token đã hết hạn.");
        }

        user.setPassWord(passwordEncoder.encode(newPassword));

        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        
        userRepo.save(user);
    }
}