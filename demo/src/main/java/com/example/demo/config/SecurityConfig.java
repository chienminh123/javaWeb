package com.example.demo.config;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return phone -> {
            User user = userRepository.findByPhone(phone);
            if (user == null) {
                throw new UsernameNotFoundException("User not found: " + phone);
            }
            return org.springframework.security.core.userdetails.User
                // use phone as the username because we authenticate by phone
                .withUsername(user.getPhone())
                .password(user.getPassWord())
                .authorities(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
                .build();
        };
    }

//     @Bean
// public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//     http
//         .authorizeHttpRequests((requests) -> requests
//             .requestMatchers("/", "/Auth/**", "/register", "/css/**", "/js/**").permitAll() 
//             .requestMatchers("/Admin/addProvider", "/Admin/addGenre").permitAll()
//             // trang ai cx xem ddc
//             .requestMatchers("/Admin/**").hasRole("ADMIN")
//             .requestMatchers("/User/**").hasRole("USER")
//             .anyRequest().authenticated()
//         )
//         .formLogin((form) -> form
//             .loginPage("/Auth/login")
//             .loginProcessingUrl("/Auth/login")
//             .usernameParameter("Phone")
//             .passwordParameter("Password")
//             .successHandler(roleBasedAuthenticationSuccessHandler())
//             .permitAll()
//         )
//         .logout((logout) -> logout
//             .logoutUrl("/logout")
//             .logoutSuccessUrl("/Auth/login")
//             .permitAll()
//             .invalidateHttpSession(true)
//             .clearAuthentication(true)
//         )
//         // BẬT CSRF NHƯNG CHO PHÉP ANONYMOUS
//         .csrf(csrf -> csrf
//             .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
//         );
//     return http.build();
// }
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        
        .authorizeHttpRequests((requests) -> requests
            // 1. CẤP QUYỀN MỞ CHO TRANG CHỦ, ĐĂNG KÝ VÀ TÀI NGUYÊN TĨNH
            .requestMatchers("/", "/Auth/**", "/register", "/css/**", "/js/**", "/img/**", "/uploads/**").permitAll() 
            .requestMatchers("/Admin/addProvider", "/Admin/addGenre").permitAll() // Giữ nguyên

            // 2. CẤP QUYỀN XEM SẢN PHẨM & TÌM KIẾM CHO TẤT CẢ MỌI NGƯỜI
            // Mở quyền truy cập cho /products, /product/{id}, /search, /api/products/suggest
            .requestMatchers("/products", "/search", "/product/**", "/api/products/suggest").permitAll()
            
            // 3. CẤP QUYỀN XỬ LÝ GIỎ HÀNG CHO USER ĐÃ ĐĂNG NHẬP
            // Fix lỗi 403 Forbidden cho /cart/update, đồng thời mở quyền cho /cart và /cart/delete
            .requestMatchers("/cart", "/cart/**").hasRole("USER") 

            // 4. PHÂN QUYỀN THEO ROLE (Các trang Admin/User khác)
            .requestMatchers("/Admin/**").hasRole("ADMIN")
            .requestMatchers("/User/**").hasRole("USER") // (Dùng cho /User/orders)

            // 5. CÁC YÊU CẦU CÒN LẠI PHẢI ĐĂNG NHẬP
            .anyRequest().authenticated()
        )
        // ... (Giữ nguyên formLogin, logout, và csrf)
        .formLogin((form) -> form
            .loginPage("/Auth/login")
            .loginProcessingUrl("/Auth/login")
            .usernameParameter("Phone")
            .passwordParameter("Password")
            .successHandler(roleBasedAuthenticationSuccessHandler())
            .permitAll()
        )
        .logout((logout) -> logout
            .logoutUrl("/logout")
            .logoutSuccessUrl("/Auth/login")
            .permitAll()
            .invalidateHttpSession(true)
            .clearAuthentication(true)
        )
        .csrf(csrf -> csrf
            .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
        );
    return http.build();
}

    /**
     * @return
     */
    @Bean
    public AuthenticationSuccessHandler roleBasedAuthenticationSuccessHandler() {
        return new AuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                    Authentication authentication) throws IOException, ServletException {
                for (GrantedAuthority authority : authentication.getAuthorities()) {
                    String role = authority.getAuthority();
                    if ("ROLE_ADMIN".equals(role)) {
                        response.sendRedirect(request.getContextPath() + "/Admin/home");
                        return;
                    } else if ("ROLE_USER".equals(role)) {
                        response.sendRedirect(request.getContextPath() + "/User/index");
                        return;
                    }
                }
                // default fallback
                response.sendRedirect(request.getContextPath() + "/");
            }
        };
    }
}