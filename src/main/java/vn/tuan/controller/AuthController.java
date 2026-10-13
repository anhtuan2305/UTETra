package vn.tuan.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import vn.tuan.dto.RegisterDTO;
import vn.tuan.entity.Account;
import vn.tuan.repository.AccountRepository;
import vn.tuan.service.AccountService;

@Controller
public class AuthController {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @GetMapping("/login")
    public String showLoginForm() {
        return "login";
    }

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("registerDTO", new RegisterDTO());
        return "register";
    }

    @PostMapping("/register")
    public String handleRegister(@Valid @ModelAttribute("registerDTO") RegisterDTO registerDTO,
                                 BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) return "register";

        String result = accountService.register(registerDTO);
        if (!"SUCCESS".equals(result)) {
            model.addAttribute("errorMessage", result);
            return "register";
        }
        return "redirect:/verify-otp?email=" + registerDTO.getEmail();
    }

    @GetMapping("/verify-otp")
    public String showVerifyOtpForm(@RequestParam("email") String email, Model model) {
        model.addAttribute("email", email);
        return "verify-otp";
    }

    @PostMapping("/verify-otp")
    public String handleVerifyOtp(@RequestParam("email") String email,
                                  @RequestParam("otp") String otp, Model model) {
        if (accountService.verifyOtp(email, otp)) {
            Account account = accountRepository.findByEmail(email).orElse(null);

            // Phân nhánh thông báo: Shipper cần chờ Admin phê duyệt trước khi được đăng nhập
            if (account != null && "SHIPPER".equalsIgnoreCase(account.getRole())) {
                model.addAttribute("successMessage", 
                        "Xác thực OTP thành công! Hồ sơ đối tác Shipper của bạn đang chờ Admin phê duyệt.");
            } else {
                model.addAttribute("successMessage", 
                        "Kích hoạt tài khoản thành công! Bạn có thể đăng nhập ngay.");
            }
            return "login";
        }
        model.addAttribute("email", email);
        model.addAttribute("errorMessage", "Mã OTP không chính xác hoặc đã hết hạn!");
        return "verify-otp";
    }

    // ====== TÍNH NĂNG QUÊN MẬT KHẨU ======

    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String handleForgotPassword(@RequestParam("email") String email, Model model) {
        String result = accountService.processForgotPassword(email);
        if ("SUCCESS".equals(result)) {
            return "redirect:/reset-password?email=" + email;
        }
        model.addAttribute("errorMessage", result);
        return "forgot-password";
    }

    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam("email") String email, Model model) {
        model.addAttribute("email", email);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String handleResetPassword(@RequestParam("email") String email,
                                      @RequestParam("otp") String otp,
                                      @RequestParam("newPassword") String newPassword,
                                      Model model) {
        if (accountService.resetPassword(email, otp, newPassword)) {
            model.addAttribute("successMessage", "Đổi mật khẩu thành công! Hãy đăng nhập lại.");
            return "login";
        }
        model.addAttribute("email", email);
        model.addAttribute("errorMessage", "Mã OTP sai hoặc đã hết hạn!");
        return "reset-password";
    }
}