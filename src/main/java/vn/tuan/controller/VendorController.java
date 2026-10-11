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
import vn.tuan.entity.Order;
import vn.tuan.entity.Shop;
import vn.tuan.repository.AccountRepository;
import vn.tuan.repository.OrderRepository;
import vn.tuan.repository.ProductRepository;
import vn.tuan.repository.ShopRepository;

import java.util.List;

@Controller
@RequestMapping("/vendor")
public class VendorController {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    private Shop getCurrentShop(Authentication authentication) {
        if (authentication == null) return null;
        Account account = accountRepository.findByEmail(authentication.getName()).orElse(null);
        if (account == null) return null;
        return shopRepository.findByOwner(account)
                .orElseGet(() -> shopRepository.findAll().stream().findFirst().orElse(null));
    }

    // 1. Dashboard tổng quan
    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication) {
        if (authentication == null) return "redirect:/login";

        Shop shop = getCurrentShop(authentication);
        if (shop == null) return "redirect:/login";

        List<Order> orders = orderRepository.findByShopOrderByCreatedAtDesc(shop);

        model.addAttribute("shop", shop);
        model.addAttribute("totalProducts", productRepository.findByShopOrderByCreatedAtDesc(shop).size());
        model.addAttribute("totalOrders", orders.size());
        model.addAttribute("recentOrders", orders.stream().limit(5).toList());

        return "vendor/dashboard";
    }

    // 2. Trang Quản lý Đơn Hàng Của Quán (/vendor/orders)
    @GetMapping("/orders")
    public String listOrders(Model model, Authentication authentication) {
        if (authentication == null) return "redirect:/login";

        Shop shop = getCurrentShop(authentication);
        if (shop == null) return "redirect:/login";

        List<Order> orders = orderRepository.findByShopOrderByCreatedAtDesc(shop);

        model.addAttribute("shop", shop);
        model.addAttribute("orders", orders);

        return "vendor/orders";
    }

    // 3. Quán xác nhận đơn: Chuẩn bị món xong (NEW -> PREPARING)
    @PostMapping("/orders/update-status")
    public String updateOrderStatus(@RequestParam("orderId") Integer orderId,
                                    @RequestParam("status") String status,
                                    RedirectAttributes redirectAttributes) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order != null) {
            order.setStatus(status);
            orderRepository.save(order);
            redirectAttributes.addFlashAttribute("message", "Đã cập nhật trạng thái đơn #" + orderId + " thành: " + status);
        }
        return "redirect:/vendor/orders";
    }
}