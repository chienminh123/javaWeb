package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

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
                // permit auth endpoints (login/register), root and static resources; do NOT permit Admin pages
                .requestMatchers("/", "/Auth/**", "/register", "/css/**", "/js/**").permitAll()
                // match the Admin controller path (case-sensitive)
                .requestMatchers("/Admin/**").hasRole("ADMIN")
                .requestMatchers("/User/**").hasRole("USER")
                .anyRequest().authenticated()
            )
            .formLogin((form) -> form
                .loginPage("/Auth/login")
                // process the login POST at /Auth/login and use the form's field names
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
                .invalidateHttpSession(true) // Đảm bảo session bị hủy
                .clearAuthentication(true)
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
                        response.sendRedirect(request.getContextPath() + "/User/home");
                        return;
                    }
                }
                // default fallback
                response.sendRedirect(request.getContextPath() + "/");
            }
        };
    }
}