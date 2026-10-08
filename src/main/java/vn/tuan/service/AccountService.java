package vn.tuan.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import vn.tuan.dto.RegisterDTO;
import vn.tuan.entity.Account;
import vn.tuan.repository.AccountRepository;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    // Sinh mã OTP 6 chữ số ngẫu nhiên
    private String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    public String register(RegisterDTO dto) {
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            return "Mật khẩu xác nhận không khớp!";
        }

        if (accountRepository.existsByEmail(dto.getEmail())) {
            return "Email này đã được sử dụng!";
        }

        String otp = generateOtp();

        Account account = Account.builder()
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword())) // MÃ HÓA BCRYPT
                .role(dto.getRole() != null ? dto.getRole() : "USER")
                .isActive(false) // Chưa kích hoạt
                .otpCode(otp)
                .otpExpiry(LocalDateTime.now().plusMinutes(5)) // Hết hạn sau 5 phút
                .build();

        accountRepository.save(account);

        // Gửi email OTP ngầm
        emailService.sendOtpEmail(dto.getEmail(), otp);

        return "SUCCESS";
    }

    public boolean verifyOtp(String email, String otp) {
        var optionalAccount = accountRepository.findByEmail(email);
        if (optionalAccount.isEmpty()) {
            return false;
        }

        Account account = optionalAccount.get();

        // Kiểm tra đúng mã và chưa hết hạn
        if (account.getOtpCode() != null 
                && account.getOtpCode().equals(otp) 
                && account.getOtpExpiry().isAfter(LocalDateTime.now())) {
            
            account.setIsActive(true);
            account.setOtpCode(null);
            account.setOtpExpiry(null);
            accountRepository.save(account);
            return true;
        }

        return false;
    }
}