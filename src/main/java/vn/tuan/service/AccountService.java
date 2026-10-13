package vn.tuan.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.tuan.dto.AddressDTO;
import vn.tuan.dto.ChangePasswordDTO;
import vn.tuan.dto.RegisterDTO;
import vn.tuan.entity.Account;
import vn.tuan.entity.Address;
import vn.tuan.repository.AccountRepository;
import vn.tuan.repository.AddressRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Service
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AddressRepository addressRepository;

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

    // 1. Đăng ký tài khoản mới & gửi OTP qua email
    @Transactional
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
                .password(passwordEncoder.encode(dto.getPassword()))
                .role(dto.getRole() != null ? dto.getRole() : "USER")
                .isActive(false)
                .otpCode(otp)
                .otpExpiry(LocalDateTime.now().plusMinutes(5))
                .build();

        accountRepository.save(account);
        emailService.sendOtpEmail(dto.getEmail(), otp);
        return "SUCCESS";
    }

    // 2. Kích hoạt tài khoản bằng mã OTP (SHIPPER giữ isActive = false để Admin duyệt)
    @Transactional
    public boolean verifyOtp(String email, String otp) {
        var optionalAccount = accountRepository.findByEmail(email);
        if (optionalAccount.isEmpty()) return false;

        Account account = optionalAccount.get();
        if (account.getOtpCode() != null && account.getOtpCode().equals(otp) 
                && account.getOtpExpiry().isAfter(LocalDateTime.now())) {
            
            // Xóa mã OTP sau khi xác minh đúng
            account.setOtpCode(null);
            account.setOtpExpiry(null);

            // Phân luồng kích hoạt
            if ("SHIPPER".equalsIgnoreCase(account.getRole())) {
                // Tài xế: Giữ isActive = false chờ Admin duyệt
                account.setIsActive(false);
            } else {
                // Khách hàng (USER) hoặc Shop (VENDOR): Kích hoạt ngay
                account.setIsActive(true);
            }

            accountRepository.save(account);
            return true;
        }
        return false;
    }

    // 3. Quên mật khẩu: Tạo OTP và gửi email
    @Transactional
    public String processForgotPassword(String email) {
        var optionalAccount = accountRepository.findByEmail(email);
        if (optionalAccount.isEmpty()) {
            return "Không tìm thấy tài khoản với email này!";
        }
        Account account = optionalAccount.get();
        String otp = generateOtp();
        account.setOtpCode(otp);
        account.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        accountRepository.save(account);
        
        emailService.sendOtpEmail(email, otp);
        return "SUCCESS";
    }

    // 4. Đặt lại mật khẩu bằng mã OTP đã nhận qua mail
    @Transactional
    public boolean resetPassword(String email, String otp, String newPassword) {
        var optionalAccount = accountRepository.findByEmail(email);
        if (optionalAccount.isEmpty()) return false;

        Account account = optionalAccount.get();
        if (account.getOtpCode() != null && account.getOtpCode().equals(otp) 
                && account.getOtpExpiry().isAfter(LocalDateTime.now())) {
            
            account.setPassword(passwordEncoder.encode(newPassword));
            account.setOtpCode(null);
            account.setOtpExpiry(null);
            accountRepository.save(account);
            return true;
        }
        return false;
    }

    // 5. Lấy danh sách địa chỉ nhận hàng của người dùng
    public List<Address> getUserAddresses(String email) {
        Account account = accountRepository.findByEmail(email).orElse(null);
        if (account == null) return Collections.emptyList();
        return addressRepository.findByAccount(account);
    }

    // 6. Thêm mới hoặc cập nhật địa chỉ giao hàng
    @Transactional
    public void saveAddress(String email, AddressDTO dto) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại!"));

        // Nếu đặt làm địa chỉ mặc định, reset cờ mặc định của các địa chỉ trước đó
        if (Boolean.TRUE.equals(dto.getIsDefault())) {
            List<Address> currentAddresses = addressRepository.findByAccount(account);
            for (Address addr : currentAddresses) {
                addr.setIsDefault(false);
            }
            addressRepository.saveAll(currentAddresses);
        }

        Address address;
        if (dto.getId() != null) {
            address = addressRepository.findById(dto.getId()).orElse(new Address());
        } else {
            address = new Address();
            address.setAccount(account);
        }

        address.setReceiverName(dto.getReceiverName());
        address.setPhone(dto.getPhone());
        address.setStreetAddress(dto.getStreetAddress());
        address.setCity(dto.getCity());
        address.setIsDefault(dto.getIsDefault() != null ? dto.getIsDefault() : false);

        addressRepository.save(address);
    }

    // 7. Xóa địa chỉ giao hàng
    @Transactional
    public void deleteAddress(String email, Integer addressId) {
        Address addr = addressRepository.findById(addressId).orElse(null);
        if (addr != null && addr.getAccount().getEmail().equals(email)) {
            addressRepository.delete(addr);
        }
    }

    // 8. Đổi mật khẩu trong trang Profile (xác thực mật khẩu cũ)
    @Transactional
    public String changePassword(String email, ChangePasswordDTO dto) {
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            return "Mật khẩu xác nhận không trùng khớp!";
        }
        Account account = accountRepository.findByEmail(email).orElse(null);
        if (account == null) return "Tài khoản không tồn tại!";

        if (!passwordEncoder.matches(dto.getCurrentPassword(), account.getPassword())) {
            return "Mật khẩu hiện tại không chính xác!";
        }

        account.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        accountRepository.save(account);
        return "SUCCESS";
    }
}