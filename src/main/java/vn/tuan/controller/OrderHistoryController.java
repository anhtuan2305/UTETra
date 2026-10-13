package vn.tuan.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.tuan.entity.Account;
import vn.tuan.entity.Order;
import vn.tuan.repository.AccountRepository;
import vn.tuan.repository.OrderRepository;

import java.util.List;

@Controller
@RequestMapping("/orders")
public class OrderHistoryController {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private OrderRepository orderRepository;

    // 1. Xem danh sách tất cả các đơn hàng của tài khoản đã đăng nhập
    @GetMapping
    public String showOrderList(Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return "redirect:/login";
        }

        Account account = accountRepository.findByEmail(authentication.getName()).orElse(null);
        if (account == null) {
            return "redirect:/login";
        }

        List<Order> orders = orderRepository.findByAccountOrderByCreatedAtDesc(account);
        model.addAttribute("orders", orders);

        return "order-list";
    }

    // 2. Xem chi tiết đơn hàng (xác thực chủ sở hữu nếu là đơn của thành viên)
    @GetMapping("/{id}")
    public String showOrderDetail(@PathVariable("id") Integer id, Model model, Authentication authentication) {
        Order order = orderRepository.findById(id).orElse(null);
        if (order == null) {
            return "redirect:/orders";
        }

        if (order.getAccount() != null) {
            if (authentication == null || !authentication.isAuthenticated() 
                    || !order.getAccount().getEmail().equals(authentication.getName())) {
                return "redirect:/login";
            }
        }

        model.addAttribute("order", order);
        return "order-detail";
    }

    // 3. Khách hàng hủy đơn (chỉ khi đơn ở trạng thái NEW)
    @PostMapping("/cancel")
    public String cancelOrder(@RequestParam("orderId") Integer orderId,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy đơn hàng!");
            return "redirect:/orders";
        }

        if (order.getAccount() != null) {
            if (authentication == null || !authentication.isAuthenticated() 
                    || !order.getAccount().getEmail().equals(authentication.getName())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền thao tác trên đơn hàng này!");
                return "redirect:/orders";
            }
        }

        if (!"NEW".equalsIgnoreCase(order.getStatus())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Đơn hàng đã được quán xác nhận hoặc đang chuẩn bị, không thể hủy!");
            return "redirect:/orders/" + orderId;
        }

        order.setStatus("CANCELLED");
        orderRepository.save(order);

        redirectAttributes.addFlashAttribute("successMessage", "Đã hủy đơn hàng #" + orderId + " thành công!");
        return "redirect:/orders";
    }

    // 4. Mở form tra cứu nhanh đơn hàng cho khách vãng lai
    @GetMapping("/lookup")
    public String showLookupForm(@RequestParam(value = "orderId", required = false) Integer orderId,
                                 Model model) {
        if (orderId != null) {
            model.addAttribute("orderId", orderId);
        }
        return "order-lookup";
    }

    // 5. Xử lý tra cứu đơn hàng bằng Mã đơn + Số điện thoại
    @PostMapping("/lookup")
    public String handleLookupOrder(@RequestParam("orderId") Integer orderId,
                                    @RequestParam("phone") String phone,
                                    Model model) {
        Order order = orderRepository.findByIdAndPhone(orderId, phone.trim()).orElse(null);

        if (order == null) {
            model.addAttribute("error", "Không tìm thấy đơn hàng #" + orderId + " với số điện thoại " + phone + "!");
            model.addAttribute("orderId", orderId);
            model.addAttribute("phone", phone);
            return "order-lookup";
        }

        model.addAttribute("order", order);
        return "order-lookup-result";
    }
}