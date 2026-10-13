package vn.tuan.controller;

import jakarta.servlet.http.HttpSession;
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
import vn.tuan.repository.ShopRepository;
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

    @Autowired
    private ShopRepository shopRepository;

    private boolean isLoggedIn(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName());
    }

    private String resolveUserIdentifier(Authentication authentication, HttpSession session) {
        if (isLoggedIn(authentication)) {
            return authentication.getName();
        }
        return "GUEST_" + session.getId();
    }

    @GetMapping
    public String showCheckoutPage(Model model, Authentication authentication, HttpSession session) {
        String userIdentifier = resolveUserIdentifier(authentication, session);
        Cart cart = cartService.getOrCreateCart(userIdentifier);

        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            return "redirect:/cart";
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        for (var item : cart.getItems()) {
            subtotal = subtotal.add(item.getProduct().getBasePrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        BigDecimal shippingFee = new BigDecimal("15000");
        BigDecimal total = subtotal.add(shippingFee);

        CheckoutDTO checkoutDTO = new CheckoutDTO();

        // Gợi ý chọn sẵn quán của sản phẩm đầu tiên trong giỏ nếu có
        if (!cart.getItems().isEmpty() && cart.getItems().get(0).getProduct() != null && cart.getItems().get(0).getProduct().getShop() != null) {
            checkoutDTO.setShopId(cart.getItems().get(0).getProduct().getShop().getId());
        }

        model.addAttribute("cart", cart);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("shippingFee", shippingFee);
        model.addAttribute("total", total);
        model.addAttribute("checkoutDTO", checkoutDTO);
        model.addAttribute("isLoggedIn", isLoggedIn(authentication));
        model.addAttribute("shops", shopRepository.findAll());

        return "checkout";
    }

    @PostMapping("/place-order")
    public String handlePlaceOrder(@Valid @ModelAttribute("checkoutDTO") CheckoutDTO checkoutDTO,
                                   BindingResult bindingResult,
                                   Model model,
                                   Authentication authentication,
                                   HttpSession session) {

        String userIdentifier = resolveUserIdentifier(authentication, session);
        Cart cart = cartService.getOrCreateCart(userIdentifier);

        BigDecimal subtotal = BigDecimal.ZERO;
        if (cart != null && cart.getItems() != null) {
            for (var item : cart.getItems()) {
                subtotal = subtotal.add(item.getProduct().getBasePrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            }
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("cart", cart);
            model.addAttribute("subtotal", subtotal);
            model.addAttribute("shippingFee", new BigDecimal("15000"));
            model.addAttribute("total", subtotal.add(new BigDecimal("15000")));
            model.addAttribute("isLoggedIn", isLoggedIn(authentication));
            model.addAttribute("shops", shopRepository.findAll());
            return "checkout";
        }

        // Truyền userIdentifier để OrderService biết giỏ hàng nào cần chốt đơn
        Order order = orderService.placeOrder(userIdentifier, checkoutDTO);
        return "redirect:/checkout/success?orderId=" + order.getId();
    }

    @GetMapping("/success")
    public String orderSuccess(@RequestParam("orderId") Integer orderId, Model model) {
        model.addAttribute("orderId", orderId);
        return "order-success";
    }
}