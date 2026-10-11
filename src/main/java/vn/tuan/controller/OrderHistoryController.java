package vn.tuan.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
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

    // Xem danh sách tất cả các đơn hàng đã đặt của người dùng
    @GetMapping
    public String showOrderList(Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
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

    // Xem chi tiết một đơn hàng cụ thể
    @GetMapping("/{id}")
    public String showOrderDetail(@PathVariable("id") Integer id, Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        Order order = orderRepository.findById(id).orElse(null);
        if (order == null || !order.getAccount().getEmail().equals(authentication.getName())) {
            return "redirect:/orders";
        }

        model.addAttribute("order", order);
        return "order-detail";
    }
}