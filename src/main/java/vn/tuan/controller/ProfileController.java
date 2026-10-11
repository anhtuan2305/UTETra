package vn.tuan.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.tuan.dto.AddressDTO;
import vn.tuan.dto.ChangePasswordDTO;
import vn.tuan.entity.Account;
import vn.tuan.repository.AccountRepository;
import vn.tuan.service.AccountService;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountService accountService;

    // Xem trang Profile (Thông tin cá nhân & Danh sách địa chỉ)
    @GetMapping
    public String showProfile(Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        String email = authentication.getName();
        Account account = accountRepository.findByEmail(email).orElse(null);
        if (account == null) {
            return "redirect:/login";
        }

        model.addAttribute("account", account);
        model.addAttribute("addresses", accountService.getUserAddresses(email));
        model.addAttribute("newAddress", new AddressDTO());
        model.addAttribute("changePasswordDTO", new ChangePasswordDTO());

        return "profile";
    }

    // Thêm địa chỉ mới
    @PostMapping("/address/add")
    public String addAddress(@Valid @ModelAttribute("newAddress") AddressDTO addressDTO,
                             BindingResult bindingResult,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorAddress", "Vui lòng nhập đầy đủ thông tin địa chỉ!");
            return "redirect:/profile";
        }

        accountService.saveAddress(authentication.getName(), addressDTO);
        redirectAttributes.addFlashAttribute("successMessage", "Thêm địa chỉ nhận hàng thành công!");
        return "redirect:/profile";
    }

    // Xóa địa chỉ
    @PostMapping("/address/delete")
    public String deleteAddress(@RequestParam("addressId") Integer addressId,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        accountService.deleteAddress(authentication.getName(), addressId);
        redirectAttributes.addFlashAttribute("successMessage", "Đã xóa địa chỉ thành công!");
        return "redirect:/profile";
    }

    // Đổi mật khẩu
    @PostMapping("/change-password")
    public String changePassword(@Valid @ModelAttribute("changePasswordDTO") ChangePasswordDTO dto,
                                 BindingResult bindingResult,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorPassword", "Vui lòng nhập đầy đủ các trường mật khẩu!");
            return "redirect:/profile";
        }

        String res = accountService.changePassword(authentication.getName(), dto);
        if ("SUCCESS".equals(res)) {
            redirectAttributes.addFlashAttribute("successMessage", "Đổi mật khẩu thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorPassword", res);
        }
        return "redirect:/profile";
    }
}