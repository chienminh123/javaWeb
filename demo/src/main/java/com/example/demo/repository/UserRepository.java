package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.User;

public interface UserRepository extends JpaRepository<User, Integer>{
    
    /**
     * @param Phone
     * @return
     */
    User findByPhone(String Phone);
    User findByEmail(String email);
    User findByResetToken(String resetToken);
    List<User> findByRoleAndPointsBetweenOrderByPointsDesc(String role, long min, long max);
    List<User> findByRole(String role);
}
