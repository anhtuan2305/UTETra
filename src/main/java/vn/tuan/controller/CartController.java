package vn.tuan.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.tuan.entity.Cart;
import vn.tuan.service.CartService;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    // Xem trang Giỏ hàng
    @GetMapping
    public String viewCart(Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        String email = authentication.getName();
        Cart cart = cartService.getOrCreateCart(email);

        // Tính tạm tính tổng tiền giỏ hàng (chưa bao gồm phí ship)
        BigDecimal totalAmount = BigDecimal.ZERO;
        if (cart.getItems() != null) {
            for (var item : cart.getItems()) {
                BigDecimal itemPrice = item.getProduct().getBasePrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity()));
                totalAmount = totalAmount.add(itemPrice);
            }
        }

        model.addAttribute("cart", cart);
        model.addAttribute("totalAmount", totalAmount);

        return "cart";
    }

    // Nhận request POST từ form chi tiết sản phẩm
    @PostMapping("/add")
    public String addToCart(@RequestParam("productId") Integer productId,
                            @RequestParam(value = "quantity", defaultValue = "1") Integer quantity,
                            @RequestParam(value = "size", required = false) String size,
                            @RequestParam(value = "sugar", required = false) String sugar,
                            @RequestParam(value = "ice", required = false) String ice,
                            @RequestParam(value = "toppings", required = false) List<String> toppings,
                            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        cartService.addToCart(authentication.getName(), productId, quantity, size, sugar, ice, toppings);
        return "redirect:/cart";
    }

    // Tăng / giảm số lượng
    @PostMapping("/update-qty")
    public String updateQuantity(@RequestParam("itemId") Integer itemId,
                                 @RequestParam("delta") Integer delta) {
        cartService.updateQuantity(itemId, delta);
        return "redirect:/cart";
    }

    // Xóa một món
    @PostMapping("/remove")
    public String removeItem(@RequestParam("itemId") Integer itemId) {
        cartService.removeItem(itemId);
        return "redirect:/cart";
    }
}