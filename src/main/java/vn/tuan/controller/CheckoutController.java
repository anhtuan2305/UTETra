package vn.tuan.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import vn.tuan.dto.CheckoutDTO;
import vn.tuan.entity.Cart;
import vn.tuan.entity.Order;
import vn.tuan.service.CartService;
import vn.tuan.service.OrderService;

import java.math.BigDecimal;

@Controller
@RequestMapping("/checkout")
public class CheckoutController {

    @Autowired
    private CartService cartService;

    @Autowired
    private OrderService orderService;

    @GetMapping
    public String showCheckoutPage(Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        String email = authentication.getName();
        Cart cart = cartService.getOrCreateCart(email);

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            return "redirect:/cart";
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        for (var item : cart.getItems()) {
            subtotal = subtotal.add(item.getProduct().getBasePrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        BigDecimal shippingFee = new BigDecimal("15000");
        BigDecimal total = subtotal.add(shippingFee);

        model.addAttribute("cart", cart);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("shippingFee", shippingFee);
        model.addAttribute("total", total);
        model.addAttribute("checkoutDTO", new CheckoutDTO());

        return "checkout";
    }

    @PostMapping("/place-order")
    public String handlePlaceOrder(@Valid @ModelAttribute("checkoutDTO") CheckoutDTO checkoutDTO,
                                   BindingResult bindingResult,
                                   Model model,
                                   Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        String email = authentication.getName();

        if (bindingResult.hasErrors()) {
            Cart cart = cartService.getOrCreateCart(email);
            BigDecimal subtotal = BigDecimal.ZERO;
            for (var item : cart.getItems()) {
                subtotal = subtotal.add(item.getProduct().getBasePrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            }
            model.addAttribute("cart", cart);
            model.addAttribute("subtotal", subtotal);
            model.addAttribute("shippingFee", new BigDecimal("15000"));
            model.addAttribute("total", subtotal.add(new BigDecimal("15000")));
            return "checkout";
        }

        Order order = orderService.placeOrder(email, checkoutDTO);
        return "redirect:/checkout/success?orderId=" + order.getId();
    }

    @GetMapping("/success")
    public String orderSuccess(@RequestParam("orderId") Integer orderId, Model model) {
        model.addAttribute("orderId", orderId);
        return "order-success";
    }
}