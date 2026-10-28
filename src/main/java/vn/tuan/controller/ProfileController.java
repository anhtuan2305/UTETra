package vn.tuan.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import vn.tuan.dto.AddressDTO;
import vn.tuan.dto.ChangePasswordDTO;
import vn.tuan.entity.Account;
import vn.tuan.service.AccountService;
import vn.tuan.service.StorefrontService;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    @Autowired
    private StorefrontService storefront;

    @Autowired
    private AccountService accountService;

    private boolean signedIn(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    // Xem hồ sơ
    @GetMapping
    public String showProfile(Model model, Authentication authentication) {
        if (!signedIn(authentication)) {
            return "redirect:/login";
        }

        String email = authentication.getName();
        Account account = storefront.account(email);

        if (account == null) {
            return "redirect:/login";
        }

        model.addAttribute("account", account);
        model.addAttribute(
                "addresses",
                accountService.getUserAddresses(email)
        );
        model.addAttribute("newAddress", new AddressDTO());
        model.addAttribute("changePasswordDTO", new ChangePasswordDTO());

        return "profile";
    }

    // Cập nhật họ tên và số điện thoại
    @PostMapping("/info")
    public String info(
            @RequestParam(name = "fullName", defaultValue = "") String fullName,
            @RequestParam(name = "phone", defaultValue = "") String phone,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        if (!signedIn(authentication)) {
            return "redirect:/login";
        }

        String normalizedName = fullName.strip();
        String normalizedPhone = phone.strip();

        redirectAttributes.addFlashAttribute(
                "profileFullName",
                normalizedName
        );
        redirectAttributes.addFlashAttribute(
                "profilePhone",
                normalizedPhone
        );

        if (normalizedName.isBlank() || normalizedName.length() > 100) {
            redirectAttributes.addFlashAttribute(
                    "errorProfile",
                    "Họ tên không được để trống và tối đa 100 ký tự."
            );
            return "redirect:/profile";
        }

        if (!normalizedPhone.matches("0[0-9]{9}")) {
            redirectAttributes.addFlashAttribute(
                    "errorProfile",
                    "Số điện thoại phải gồm 10 chữ số và bắt đầu bằng 0."
            );
            return "redirect:/profile";
        }

        try {
            storefront.profile(
                    authentication.getName(),
                    normalizedName,
                    normalizedPhone
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Cập nhật thông tin cá nhân thành công!"
            );
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute(
                    "errorProfile",
                    ex.getMessage() != null
                            ? ex.getMessage()
                            : "Thông tin cá nhân không hợp lệ."
            );
        }

        return "redirect:/profile";
    }

    // Thêm địa chỉ
    @PostMapping("/address/add")
    public String addAddress(
            @Valid @ModelAttribute("newAddress") AddressDTO addressDTO,
            BindingResult bindingResult,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        if (!signedIn(authentication)) {
            return "redirect:/login";
        }

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "errorAddress",
                    "Vui lòng nhập đầy đủ thông tin địa chỉ hợp lệ!"
            );
            return "redirect:/profile";
        }

        accountService.saveAddress(
                authentication.getName(),
                addressDTO
        );

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Thêm địa chỉ nhận hàng thành công!"
        );

        return "redirect:/profile";
    }

    // Xóa địa chỉ
    @PostMapping("/address/delete")
    public String deleteAddress(
            @RequestParam("addressId") Integer addressId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        if (!signedIn(authentication)) {
            return "redirect:/login";
        }

        accountService.deleteAddress(
                authentication.getName(),
                addressId
        );

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Đã xóa địa chỉ thành công!"
        );

        return "redirect:/profile";
    }

    // Đổi mật khẩu
    @PostMapping("/change-password")
    public String changePassword(
            @Valid @ModelAttribute("changePasswordDTO") ChangePasswordDTO dto,
            BindingResult bindingResult,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        if (!signedIn(authentication)) {
            return "redirect:/login";
        }

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "errorPassword",
                    "Vui lòng nhập đầy đủ các trường mật khẩu hợp lệ!"
            );
            return "redirect:/profile";
        }

        String result = accountService.changePassword(
                authentication.getName(),
                dto
        );

        if ("SUCCESS".equals(result)) {
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Đổi mật khẩu thành công!"
            );
        } else {
            redirectAttributes.addFlashAttribute(
                    "errorPassword",
                    result
            );
        }

        return "redirect:/profile";
    }
}