package vn.tuan.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Tắt CSRF để tiện test các form POST
            .authorizeHttpRequests(auth -> auth
                // CÁC ĐƯỜNG DẪN CÔNG KHAI
                .requestMatchers(
                    "/", 
                    "/home", 
                    "/cart/**",
                    "/checkout/**",
                    "/orders/lookup/**",  // <-- Bổ sung đường dẫn này để khách vãng lai tra cứu đơn
                    "/register/**", 
                    "/verify-otp/**", 
                    "/forgot-password/**", 
                    "/reset-password/**", 
                    "/login", 
                    "/partner/**",        // Cho phép truy cập luồng đăng ký mở quán
                    "/products/**", 
                    "/css/**", 
                    "/js/**", 
                    "/images/**", 
                    "/uploads/**",       // Cho phép xem ảnh upload
                    "/favicon.ico"
                ).permitAll()
                
                // CÁC ĐƯỜNG DẪN DÀNH CHO VENDOR (CHỦ QUÁN): Cho phép cả VENDOR lẫn ROLE_VENDOR
                .requestMatchers("/vendor/**").hasAnyAuthority("VENDOR", "ROLE_VENDOR")
                
                // CÁC ĐƯỜNG DẪN DÀNH CHO ADMIN: Cho phép cả ADMIN lẫn ROLE_ADMIN
                .requestMatchers("/admin/**").hasAnyAuthority("ADMIN", "ROLE_ADMIN")
                
                // CÁC ĐƯỜNG DẪN DÀNH CHO SHIPPER: Cho phép cả SHIPPER lẫn ROLE_SHIPPER
                .requestMatchers("/shipper/**").hasAnyAuthority("SHIPPER", "ROLE_SHIPPER")
                
                // TẤT CẢ CÁC ĐƯỜNG DẪN CÒN LẠI PHẢI ĐĂNG NHẬP (VD: /orders, /orders/{id}, /profile)
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            );

        return http.build();
    }
}