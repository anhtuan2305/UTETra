package vn.tuan.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.tuan.entity.Account;
import vn.tuan.entity.Shop;
import vn.tuan.repository.AccountRepository;
import vn.tuan.repository.ShopRepository;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/partner")
public class PartnerController {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ShopRepository shopRepository;

    // 1. Hiển thị form đăng ký hoặc thông báo trạng thái hồ sơ
    @GetMapping("/register")
    public String showRegisterForm(Authentication authentication, Model model) {
        if (authentication == null) {
            return "redirect:/login";
        }

        Account account = accountRepository.findByEmail(authentication.getName()).orElse(null);
        if (account == null) {
            return "redirect:/login";
        }

        // Kiểm tra xem tài khoản này đã có quán hoặc đã gửi đơn chưa
        Shop existingShop = shopRepository.findByOwner(account).orElse(null);
        if (existingShop != null) {
            model.addAttribute("shop", existingShop);
            return "partner/status"; // Chuyển sang trang xem tiến độ duyệt
        }

        model.addAttribute("account", account);
        return "partner/register"; // Chưa gửi đơn thì mở form đăng ký
    }

    // 2. Tiếp nhận thông tin đăng ký gửi lên
    @PostMapping("/register")
    public String submitRegisterShop(@RequestParam("shopName") String shopName,
                                     @RequestParam(value = "description", required = false) String description,
                                     @RequestParam(value = "logoUrl", required = false) String logoUrl,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes) {
        if (authentication == null) {
            return "redirect:/login";
        }

        Account account = accountRepository.findByEmail(authentication.getName()).orElse(null);
        if (account == null) {
            return "redirect:/login";
        }

        // Kiểm tra tránh gửi lặp lại
        if (shopRepository.findByOwner(account).isPresent()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn đã có hồ sơ đăng ký quán trên hệ thống!");
            return "redirect:/partner/register";
        }

        Shop shop = new Shop();
        shop.setShopName(shopName);
        shop.setDescription(description);
        shop.setOwner(account);
        // Gán ảnh logo mặc định nếu chưa có
        shop.setLogoUrl((logoUrl != null && !logoUrl.isBlank()) ? logoUrl : "https://placehold.co/100x100?text=Shop");
        shop.setRating(new java.math.BigDecimal("5.0"));
        shop.setStatus("PENDING"); // Đặt trạng thái: Chờ Admin phê duyệt
        shop.setCreatedAt(LocalDateTime.now());

        shopRepository.save(shop);

        redirectAttributes.addFlashAttribute("message", "Gửi hồ sơ đăng ký quán thành công! BQT UTETra sẽ xét duyệt trong thời gian sớm nhất.");
        return "redirect:/partner/register";
    }
}