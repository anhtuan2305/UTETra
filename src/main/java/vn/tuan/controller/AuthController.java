package vn.tuan.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import vn.tuan.dto.RegisterDTO;
import vn.tuan.service.AccountService;

@Controller
public class AuthController {

    @Autowired
    private AccountService accountService;

    // Trang đăng nhập
    @GetMapping("/login")
    public String showLoginForm() {
        return "login";
    }

    // Trang đăng ký
    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("registerDTO", new RegisterDTO());
        return "register";
    }

    // Xử lý gửi form đăng ký
    @PostMapping("/register")
    public String handleRegister(@Valid @ModelAttribute("registerDTO") RegisterDTO registerDTO,
                                 BindingResult bindingResult,
                                 Model model) {
        if (bindingResult.hasErrors()) {
            return "register";
        }

        String result = accountService.register(registerDTO);
        if (!"SUCCESS".equals(result)) {
            model.addAttribute("errorMessage", result);
            return "register";
        }

        // Chuyển hướng sang trang nhập OTP kèm theo email
        return "redirect:/verify-otp?email=" + registerDTO.getEmail();
    }

    // Trang nhập mã OTP
    @GetMapping("/verify-otp")
    public String showVerifyOtpForm(@RequestParam("email") String email, Model model) {
        model.addAttribute("email", email);
        return "verify-otp";
    }

    // Xử lý xác thực OTP
    @PostMapping("/verify-otp")
    public String handleVerifyOtp(@RequestParam("email") String email,
                                  @RequestParam("otp") String otp,
                                  Model model) {
        boolean isVerified = accountService.verifyOtp(email, otp);
        if (isVerified) {
            model.addAttribute("successMessage", "Kích hoạt tài khoản thành công! Bạn có thể đăng nhập ngay.");
            return "login";
        }

        model.addAttribute("email", email);
        model.addAttribute("errorMessage", "Mã OTP không chính xác hoặc đã hết hạn!");
        return "verify-otp";
    }
}