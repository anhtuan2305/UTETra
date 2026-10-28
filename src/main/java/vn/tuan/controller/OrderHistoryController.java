	package vn.tuan.controller;
	import lombok.RequiredArgsConstructor;import org.springframework.stereotype.Controller;import org.springframework.ui.Model;import org.springframework.web.bind.annotation.*;import java.security.Principal;import vn.tuan.service.*;
	@Controller @RequestMapping("/orders") @RequiredArgsConstructor public class OrderHistoryController {
	 private final StorefrontService storefront;private final OrderLifecycleService lifecycle;
	 @GetMapping public String list(Principal p,Model m,@RequestParam(defaultValue="")String status){m.addAttribute("orders",storefront.orders(p.getName()).stream().filter(o->status.isBlank() || status.equals(o.getStatus())).toList());return "order-list";}
	 @GetMapping("/{id}")public String detail(Principal p,@PathVariable Integer id,Model m){m.addAttribute("order",storefront.ownedOrder(p.getName(),id));return "order-detail";}
	 @PostMapping("/cancel")public String cancel(Principal p,@RequestParam Integer orderId){lifecycle.cancel(p.getName(),orderId);return "redirect:/orders";}
	 @PostMapping("/{id}/return")public String requestReturn(Principal p,@PathVariable Integer id,@RequestParam String reason){lifecycle.requestReturn(p.getName(),id,reason);return "redirect:/orders/"+id;}
	 @GetMapping("/lookup")public String lookup(@RequestParam(required=false)Integer orderId,Model m){m.addAttribute("orderId",orderId);return "order-lookup";}
	 @PostMapping("/lookup")public String lookup(@RequestParam Integer orderId,@RequestParam String phone,Model m){var o=storefront.lookup(orderId,phone);if(o==null){m.addAttribute("error","Thông tin tra cứu không khớp");m.addAttribute("orderId",orderId);m.addAttribute("phone",phone);return "order-lookup";}m.addAttribute("order",o);return "order-lookup-result";}
	}
