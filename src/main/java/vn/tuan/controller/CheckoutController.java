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
    @Autowired private vn.tuan.service.AccountService accountService;
    @ModelAttribute("savedAddresses") public java.util.List<vn.tuan.entity.Address> addresses(Authentication auth){return isLoggedIn(auth)?accountService.getUserAddresses(auth.getName()):java.util.List.of();}
    @Autowired private vn.tuan.service.QuoteService quotes;
    @Autowired private vn.tuan.service.PaymentService payments;
    @ModelAttribute("vnpayEnabled") public boolean vnpayEnabled(){return payments.available();}
    @Autowired private vn.tuan.service.StorefrontService storefront;



    @ModelAttribute("carriers") public java.util.List<vn.tuan.entity.Carrier> carriers(Authentication auth,HttpSession session){Cart cart=cartService.getOrCreateCart(resolveUserIdentifier(auth,session));Integer shopId=cart.getItems().isEmpty()?null:cart.getItems().get(0).getProduct().getShop().getId();return storefront.carriers().stream().filter(c->Boolean.TRUE.equals(c.getIsActive()) && (c.getShop()==null || c.getShop().getId().equals(shopId))).toList();}

    private boolean isLoggedIn(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName());
    }

    private String resolveUserIdentifier(Authentication authentication, HttpSession session) {
        if (isLoggedIn(authentication)) {
            return authentication.getName();
        }
        if(session.getAttribute("guestCartKey")==null)session.setAttribute("guestCartKey","GUEST_"+session.getId());
        return session.getAttribute("guestCartKey").toString();
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
            subtotal = subtotal.add(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
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
        model.addAttribute("shops", cart.getItems().isEmpty()?java.util.List.of():java.util.List.of(cart.getItems().get(0).getProduct().getShop()));

        return "checkout";
    }

    @PostMapping("/place-order")
    public String handlePlaceOrder(@Valid @ModelAttribute("checkoutDTO") CheckoutDTO checkoutDTO,
                                   BindingResult bindingResult,
                                   Model model,
                                   Authentication authentication,
                                   HttpSession session, jakarta.servlet.http.HttpServletRequest request) {

        String userIdentifier = resolveUserIdentifier(authentication, session);
        Cart cart = cartService.getOrCreateCart(userIdentifier);

        BigDecimal subtotal = BigDecimal.ZERO;
        if (cart != null && cart.getItems() != null) {
            for (var item : cart.getItems()) {
                subtotal = subtotal.add(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            }
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("cart", cart);
            model.addAttribute("subtotal", subtotal);
            model.addAttribute("shippingFee", new BigDecimal("15000"));
            model.addAttribute("total", subtotal.add(new BigDecimal("15000")));
            model.addAttribute("isLoggedIn", isLoggedIn(authentication));
            model.addAttribute("shops", cart.getItems().isEmpty()?java.util.List.of():java.util.List.of(cart.getItems().get(0).getProduct().getShop()));
            return "checkout";
        }

        // Truyền userIdentifier để OrderService biết giỏ hàng nào cần chốt đơn
        Order order = orderService.placeOrder(userIdentifier, checkoutDTO);
        session.setAttribute("lastOrderId",order.getId());
        if("VNPAY".equals(order.getPaymentMethod()))return "redirect:"+payments.redirect(order,request.getRemoteAddr());
        return "redirect:/checkout/success?orderId=" + order.getId();
    }

    @GetMapping("/success")
    public String orderSuccess(@RequestParam("orderId") Integer orderId, Model model, HttpSession session) {
        vn.tuan.service.OrderRules.require(orderId.equals(session.getAttribute("lastOrderId")),"Vui lòng tra cứu đơn bằng mã đơn và số điện thoại");
        model.addAttribute("order",storefront.order(orderId));
        model.addAttribute("orderId", orderId);
        return "order-success";
    }
 @PostMapping("/quote") @ResponseBody public org.springframework.http.ResponseEntity<?> quote(@RequestParam Integer shopId,@RequestParam(required=false)Integer carrierId,@RequestParam(required=false)String promotionCode,Authentication authentication,HttpSession session){try{Cart cart=cartService.getOrCreateCart(resolveUserIdentifier(authentication,session));vn.tuan.service.OrderRules.require(!cart.getItems().isEmpty(),"Giỏ hàng trống");vn.tuan.service.OrderRules.require(cart.getItems().stream().allMatch(i->i.getProduct().getShop().getId().equals(shopId)),"Món không thuộc chi nhánh");BigDecimal subtotal=cart.getItems().stream().map(i->i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity()))).reduce(BigDecimal.ZERO,BigDecimal::add);var q=quotes.calculate(subtotal,shopId,carrierId,promotionCode);return org.springframework.http.ResponseEntity.ok(java.util.Map.of("subtotal",q.subtotal(),"shipping",q.shipping(),"discount",q.discount(),"total",q.total()));}catch(IllegalArgumentException e){return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of("error",e.getMessage()));}}
}