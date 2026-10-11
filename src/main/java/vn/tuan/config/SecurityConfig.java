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
                // CÁC ĐƯỜNG DẪN CÔNG KHAI (Cho phép Guest và Chưa đăng nhập truy cập)
                .requestMatchers(
                    "/", 
                    "/home", 
                    "/register/**", 
                    "/verify-otp/**", 
                    "/forgot-password/**", 
                    "/reset-password/**", 
                    "/login", 
                    "/products/**", 
                    "/css/**", 
                    "/js/**", 
                    "/images/**", 
                    "/favicon.ico"
                ).permitAll()
                
                // CÁC ĐƯỜNG DẪN DÀNH CHO VENDOR (CHỦ QUÁN)
                .requestMatchers("/vendor/**").hasRole("VENDOR")
                
                // CÁC ĐƯỜNG DẪN DÀNH CHO ADMIN (QUẢN TRỊ VIÊN)
                .requestMatchers("/admin/**").hasRole("ADMIN")
             // CÁC ĐƯỜNG DẪN DÀNH CHO SHIPPER
                .requestMatchers("/shipper/**").hasRole("SHIPPER")
                
                // TẤT CẢ CÁC ĐƯỜNG DẪN CÒN LẠI PHẢI ĐĂNG NHẬP
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