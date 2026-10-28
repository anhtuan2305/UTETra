
package vn.tuan.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

import vn.tuan.entity.Account;
import vn.tuan.service.ActorService;
import vn.tuan.service.OrderLifecycleService;
import vn.tuan.service.StorefrontService;

@Controller
@RequestMapping("/shipper")
@RequiredArgsConstructor
public class ShipperController {

    private final StorefrontService storefront;
    private final ActorService actors;
    private final OrderLifecycleService lifecycle;

    private Account requireShipper(Principal principal) {
        if (principal == null) {
            throw new AccessDeniedException("Vui lòng đăng nhập.");
        }

        Account account = actors.account(principal.getName());

        if (account == null
                || !"SHIPPER".equals(actors.role(account))) {
            throw new AccessDeniedException(
                    "Chỉ tài khoản Shipper mới được truy cập."
            );
        }

        return account;
    }

    @GetMapping("/orders")
    public String list(Model model, Principal principal) {
        requireShipper(principal);

        var visible = storefront.deliveries(principal.getName());

        model.addAttribute("orders", visible);

        model.addAttribute(
                "completedCount",
                visible.stream()
                        .filter(o -> "COMPLETED".equals(o.getStatus()))
                        .count()
        );

        return "shipper/orders";
    }

    @PostMapping("/orders/update-status")
    public String update(
            @RequestParam Integer orderId,
            @RequestParam String status,
            Principal principal) {

        requireShipper(principal);

        lifecycle.delivery(
                principal.getName(),
                orderId,
                status
        );

        return "redirect:/shipper/orders";
    }
}
