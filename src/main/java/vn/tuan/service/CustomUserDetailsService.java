package vn.tuan.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import vn.tuan.entity.Account;
import vn.tuan.repository.AccountRepository;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private AccountRepository accountRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Account account = accountRepository.findByEmail(email.trim().toLowerCase(java.util.Locale.ROOT))
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản với email: " + email));

        // Kiểm tra trạng thái kích hoạt tài khoản
        if (!Boolean.TRUE.equals(account.getIsActive())) {
            if ("SHIPPER".equalsIgnoreCase(account.getRole())) {
                throw new DisabledException("Tài khoản Shipper của bạn đang chờ Quản trị viên (Admin) phê duyệt!");
            }
            throw new DisabledException("Tài khoản chưa được kích hoạt qua mã OTP!");
        }

        return new User(
                account.getEmail(),
                account.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + account.getRole().replace("ROLE_", "")))
        );
    }
}