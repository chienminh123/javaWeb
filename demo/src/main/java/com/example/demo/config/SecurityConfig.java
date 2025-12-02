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

@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        
        .authorizeHttpRequests((requests) -> requests
            // 1. CẤP QUYỀN MỞ CHO TRANG CHỦ, ĐĂNG KÝ VÀ TÀI NGUYÊN TĨNH
            .requestMatchers("/", "/Auth/**", "/register", "/css/**", "/js/**", "/img/**","/assets/**",  "/uploads/**","/forgot-password","/reset-password","/user-css/**").permitAll() 
            .requestMatchers("/Admin/addProvider", "/Admin/addGenre").permitAll() // Giữ nguyên

            // 2. CẤP QUYỀN XEM SẢN PHẨM & TRANG USER CHO TẤT CẢ MỌI NGƯỜI (KHÔNG CẦN ĐĂNG NHẬP)
            .requestMatchers("/products", "/search", "/product/**", "/api/products/suggest", "/User/index", "/User/**").permitAll()
            
            // 3. TRANG ĐĂNG NHẬP ADMIN
            .requestMatchers("/Admin/login").permitAll()
            
            // 4. CẤP QUYỀN XỬ LÝ GIỎ HÀNG CHO USER ĐÃ ĐĂNG NHẬP
            // Chỉ yêu cầu đăng nhập khi thêm vào giỏ hàng hoặc xem giỏ hàng
            .requestMatchers("/cart/add", "/cart", "/cart/**", "/checkout", "/checkout/**").hasRole("USER") 

            // 5. PHÂN QUYỀN THEO ROLE (Các trang Admin)
            .requestMatchers("/Admin/**").hasRole("ADMIN")

            // 6. CÁC YÊU CẦU CÒN LẠI PHẢI ĐĂNG NHẬP
            .anyRequest().authenticated()
        )
        // Xử lý exception khi chưa đăng nhập
        .exceptionHandling((exceptions) -> exceptions
            .authenticationEntryPoint((request, response, authException) -> {
                // Nếu truy cập trang admin thì redirect đến /Admin/login
                if (request.getRequestURI().startsWith("/Admin/")) {
                    response.sendRedirect(request.getContextPath() + "/Admin/login");
                } else {
                    response.sendRedirect(request.getContextPath() + "/Auth/login");
                }
            })
        )
        // Cấu hình form login - xử lý cả user và admin login
        .formLogin((form) -> form
            .loginPage("/Auth/login")
            .loginProcessingUrl("/Auth/login")
            .usernameParameter("Phone")
            .passwordParameter("Password")
            .successHandler(roleBasedAuthenticationSuccessHandler())
            .failureHandler((request, response, exception) -> {
                // Kiểm tra nếu đăng nhập từ trang admin
                String referer = request.getHeader("Referer");
                if (referer != null && referer.contains("/Admin/login")) {
                    response.sendRedirect(request.getContextPath() + "/Admin/login?error");
                } else {
                    response.sendRedirect(request.getContextPath() + "/Auth/login?error");
                }
            })
            .permitAll()
        )
        .logout((logout) -> logout
            .logoutUrl("/logout")
            .logoutSuccessHandler((request, response, authentication) -> {
                // Kiểm tra nếu đăng xuất từ trang admin thì redirect về /Admin/login
                String referer = request.getHeader("Referer");
                if (referer != null && referer.contains("/Admin/")) {
                    response.sendRedirect(request.getContextPath() + "/Admin/login?logout");
                } else {
                    response.sendRedirect(request.getContextPath() + "/Auth/login?logout");
                }
            })
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
                    // Kiểm tra nếu đăng nhập từ trang admin
                    String referer = request.getHeader("Referer");
                    if ("ROLE_ADMIN".equals(role)) {
                        response.sendRedirect(request.getContextPath() + "/Admin/home");
                        return;
                    } else if ("ROLE_USER".equals(role)) {
                        // Nếu user đăng nhập từ trang admin thì hiển thị thông báo lỗi
                        if (referer != null && referer.contains("/Admin/login")) {
                            request.getSession().setAttribute("error", "Bạn không có quyền truy cập trang admin!");
                            response.sendRedirect(request.getContextPath() + "/Admin/login?error=unauthorized");
                            return;
                        }
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