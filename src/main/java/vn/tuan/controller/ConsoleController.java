
package vn.tuan.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;

import vn.tuan.service.*;

@Controller
@RequestMapping("/console")
@RequiredArgsConstructor
public class ConsoleController {

    private final ConsoleService console;
    private final ActorService actors;
    private final OrderLifecycleService lifecycle;

    @ModelAttribute
    public void common(Model m, Principal p) {
        var a = actors.account(p.getName());
        m.addAttribute("role", actors.role(a));
        m.addAttribute("shops", console.shops(p.getName()));
        m.addAttribute("categories", console.categories(p.getName()));
        m.addAttribute("carriers", console.carriers(p.getName()));
    }

    @GetMapping({"", "/dashboard"})
    public String dashboard(Model m, Principal p) {
        var orders = console.orders(p.getName());

        var complete = orders.stream()
                .filter(o -> "COMPLETED".equals(o.getStatus()))
                .toList();

        m.addAttribute("orders", orders);

        m.addAttribute("revenue", complete.stream()
                .map(o -> o.getFinalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        m.addAttribute("commission", complete.stream()
                .map(o -> o.getCommissionAmount() == null
                        ? BigDecimal.ZERO
                        : o.getCommissionAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        m.addAttribute("completed", complete.size());

        return "console/dashboard";
    }

    @GetMapping("/products")
    public String products(
            Model m,
            Principal p,
            @RequestParam(required = false) Integer edit,
            @RequestParam(defaultValue = "") String keyword) {

        var list = console.products(p.getName());

        m.addAttribute("products", list.stream()
                .filter(x -> x.getName().toLowerCase()
                        .contains(keyword.toLowerCase()))
                .toList());

        if (edit == null) {
            m.addAttribute("editProduct", null);
        } else {
            var product = list.stream()
                    .filter(x -> x.getId().equals(edit))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.FORBIDDEN,
                            "Ban khong co quyen chinh sua san pham nay"));

            m.addAttribute("editProduct", product);
        }

        return "console/products";
    }

    @PostMapping("/products")
    public String product(
            Principal p,
            @RequestParam(required = false) Integer id,
            @RequestParam Integer shopId,
            @RequestParam Integer categoryId,
            @RequestParam String name,
            @RequestParam BigDecimal price,
            @RequestParam Integer stock,
            @RequestParam(defaultValue = "") String description,
            @RequestParam String status,
            @RequestParam(required = false) MultipartFile image) {

        console.product(
                p.getName(), id, shopId, categoryId,
                name, price, stock, description, status, image);

        return "redirect:/console/products";
    }

    @PostMapping("/products/{id}/hide")
    public String hide(Principal p, @PathVariable Integer id) {
        console.hideProduct(p.getName(), id);
        return "redirect:/console/products";
    }

    @GetMapping("/orders")
    public String orders(
            Model m,
            Principal p,
            @RequestParam(defaultValue = "") String status) {

        m.addAttribute("orders", console.orders(p.getName())
                .stream()
                .filter(o -> status.isBlank()
                        || status.equals(o.getStatus()))
                .toList());

        return "console/orders";
    }

    @PostMapping("/orders/{id}")
    public String status(
            Principal p,
            @PathVariable Integer id,
            @RequestParam String status) {

        lifecycle.vendor(p.getName(), id, status);
        return "redirect:/console/orders";
    }

    @GetMapping("/promotions")
    public String promotions(
            Model m,
            Principal p,
            @RequestParam(required = false) Integer edit) {

        var all = console.promotions(p.getName());

        m.addAttribute("promotions", all);

        if (edit == null) {
            m.addAttribute("editPromotion", null);
        } else {
            var promotion = all.stream()
                    .filter(x -> x.getId().equals(edit))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.FORBIDDEN,
                            "Ban khong co quyen chinh sua khuyen mai nay"));

            m.addAttribute("editPromotion", promotion);
        }

        return "console/promotions";
    }

    @PostMapping("/promotions")
    public String promotion(
            Principal p,
            @RequestParam(required = false) Integer id,
            @RequestParam(required = false) Integer shopId,
            @RequestParam String code,
            @RequestParam int percent,
            @RequestParam BigDecimal max,
            @RequestParam BigDecimal min,
            @RequestParam String type,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime expiry,
            @RequestParam(defaultValue = "false") boolean active) {

        console.promotion(
                p.getName(), id, shopId, code,
                percent, max, min, type, expiry, active);

        return "redirect:/console/promotions";
    }

    @GetMapping("/settings")
    public String settings(Model m, Principal p) {
        m.addAttribute("managers", console.managers());
        return "console/settings";
    }

    @PostMapping("/categories")
    public String category(
            Principal p,
            @RequestParam(required = false) Integer id,
            @RequestParam(required = false) Integer shopId,
            @RequestParam String name) {

        console.category(p.getName(), id, shopId, name);
        return "redirect:/console/settings";
    }

    @PostMapping("/carriers")
    public String carrier(
            Principal p,
            @RequestParam(required = false) Integer id,
            @RequestParam(required = false) Integer shopId,
            @RequestParam String name,
            @RequestParam BigDecimal fee,
            @RequestParam(defaultValue = "false") boolean active) {

        console.carrier(p.getName(), id, shopId, name, fee, active);
        return "redirect:/console/settings";
    }

    @PostMapping("/shop")
    public String shop(
            Principal p,
            @RequestParam Integer id,
            @RequestParam String name,
            @RequestParam String description,
            @RequestParam(required = false) MultipartFile logo) {

        console.shop(p.getName(), id, name, description, logo);
        return "redirect:/console/settings";
    }

    @PostMapping("/assignment")
    public String assign(
            Principal p,
            @RequestParam Integer shopId,
            @RequestParam(required = false) Integer managerId,
            @RequestParam BigDecimal commission) {

        console.assignManager(
                p.getName(), shopId, managerId, commission);

        return "redirect:/console/settings";
    }

    @GetMapping("/users")
    public String users(
            Model m,
            Principal p,
            @RequestParam(defaultValue = "") String keyword) {

        m.addAttribute("users", console.users(p.getName())
                .stream()
                .filter(a -> a.getEmail().toLowerCase()
                        .contains(keyword.toLowerCase()))
                .toList());

        return "console/users";
    }

    @PostMapping("/managers")
    public String manager(
            Principal p,
            @RequestParam String email,
            @RequestParam String password) {

        console.manager(p.getName(), email, password);
        return "redirect:/console/settings";
    }

    @PostMapping("/refund/{id}")
    public String refund(Principal p, @PathVariable Integer id) {
        lifecycle.confirmRefund(p.getName(), id);
        return "redirect:/console/orders";
    }
}
