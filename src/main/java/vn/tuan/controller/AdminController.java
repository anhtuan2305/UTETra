package vn.tuan.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.tuan.entity.Account;
import vn.tuan.entity.Shop;
import vn.tuan.repository.AccountRepository;
import vn.tuan.repository.OrderRepository;
import vn.tuan.repository.ProductRepository;
import vn.tuan.repository.ShopRepository;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    // 1. Dashboard Quản trị viên tổng quan
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("totalUsers", accountRepository.count());
        model.addAttribute("totalShops", shopRepository.count());
        model.addAttribute("totalProducts", productRepository.count());
        model.addAttribute("totalOrders", orderRepository.count());

        // Lấy 5 quán mới nhất
        model.addAttribute("recentShops", shopRepository.findAll(PageRequest.of(0, 5, Sort.by("id").descending())).getContent());
        // Lấy 5 user mới nhất
        model.addAttribute("recentUsers", accountRepository.findAll(PageRequest.of(0, 5, Sort.by("id").descending())).getContent());

        return "admin/dashboard";
    }

    // 2. Quản lý Người dùng (UC_027)
    @GetMapping("/users")
    public String listUsers(@RequestParam(name = "keyword", required = false) String keyword,
                            @RequestParam(name = "page", defaultValue = "0") int page,
                            Model model) {
        var pageable = PageRequest.of(page, 10, Sort.by("id").descending());
        var userPage = (keyword != null && !keyword.trim().isEmpty())
                ? accountRepository.findByEmailContainingIgnoreCase(keyword.trim(), pageable)
                : accountRepository.findAll(pageable);

        model.addAttribute("userPage", userPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", userPage.getTotalPages());
        model.addAttribute("keyword", keyword);

        return "admin/users";
    }

    // Khóa / Kích hoạt tài khoản
    @PostMapping("/users/toggle-status")
    public String toggleUserStatus(@RequestParam("userId") Integer userId, RedirectAttributes redirectAttributes) {
        Account acc = accountRepository.findById(userId).orElse(null);
        if (acc != null) {
            acc.setIsActive(!Boolean.TRUE.equals(acc.getIsActive()));
            accountRepository.save(acc);
            redirectAttributes.addFlashAttribute("message", "Đã cập nhật trạng thái tài khoản: " + acc.getEmail());
        }
        return "redirect:/admin/users";
    }

    // 3. Quản lý Cửa hàng (UC_018: Cấp phép hoặc Cấm hoạt động)
    @GetMapping("/shops")
    public String listShops(@RequestParam(name = "keyword", required = false) String keyword,
                            @RequestParam(name = "page", defaultValue = "0") int page,
                            Model model) {
        var pageable = PageRequest.of(page, 10, Sort.by("id").descending());
        var shopPage = (keyword != null && !keyword.trim().isEmpty())
                ? shopRepository.findByShopNameContainingIgnoreCase(keyword.trim(), pageable)
                : shopRepository.findAll(pageable);

        model.addAttribute("shopPage", shopPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", shopPage.getTotalPages());
        model.addAttribute("keyword", keyword);

        return "admin/shops";
    }

    // Duyệt cấp phép (ACTIVE) hoặc Cấm (BANNED)
    @PostMapping("/shops/update-status")
    public String updateShopStatus(@RequestParam("shopId") Integer shopId,
                                   @RequestParam("status") String status,
                                   RedirectAttributes redirectAttributes) {
        Shop shop = shopRepository.findById(shopId).orElse(null);
        if (shop != null) {
            shop.setStatus(status);
            shopRepository.save(shop);
            redirectAttributes.addFlashAttribute("message", "Đã cập nhật trạng thái quán: " + shop.getShopName() + " sang " + status);
        }
        return "redirect:/admin/shops";
    }

    // 4. Quản lý toàn bộ Đơn hàng hệ thống (UC_029)
    @GetMapping("/orders")
    public String listOrders(Model model) {
        model.addAttribute("orders", orderRepository.findAll(Sort.by("createdAt").descending()));
        return "admin/orders";
    }
}