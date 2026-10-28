package vn.tuan.config;
import org.springframework.context.annotation.*;import org.springframework.security.config.annotation.web.builders.HttpSecurity;import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;import org.springframework.security.crypto.password.PasswordEncoder;import org.springframework.security.web.SecurityFilterChain;import org.springframework.security.web.access.intercept.AuthorizationFilter;import vn.tuan.repository.AccountRepository;
@Configuration @EnableMethodSecurity public class SecurityConfig {
 @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
 @Bean @org.springframework.core.annotation.Order(2) SecurityFilterChain web(HttpSecurity h,AccountRepository accounts,LoginSuccessHandler success)throws Exception{
 h.authorizeHttpRequests(a->a.requestMatchers("/","/home","/catalog","/cart/**","/checkout/**","/orders/lookup/**","/register/**","/verify-otp/**","/forgot-password/**","/reset-password/**","/login","/products/**","/css/**","/js/**","/images/**","/uploads/**","/favicon.ico","/error","/payment/**").permitAll().requestMatchers("/admin/**").hasRole("ADMIN").requestMatchers("/vendor/**").hasRole("VENDOR").requestMatchers("/manager/**").hasRole("MANAGER").requestMatchers("/console/**").hasAnyRole("ADMIN","MANAGER","VENDOR").requestMatchers("/shipper/**").hasRole("SHIPPER").anyRequest().authenticated());
 h.addFilterBefore(new SessionAccountFilter(accounts),AuthorizationFilter.class);
 h.formLogin(f->f.loginPage("/login").loginProcessingUrl("/login").usernameParameter("username").successHandler(success).failureUrl("/login?error=true").permitAll());
 h.logout(l->l.logoutSuccessUrl("/login?logout=true").invalidateHttpSession(true).deleteCookies("JSESSIONID"));return h.build();
 }
}
