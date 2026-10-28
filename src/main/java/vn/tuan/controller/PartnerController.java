package vn.tuan.controller;
import lombok.RequiredArgsConstructor;import org.springframework.stereotype.Controller;import org.springframework.ui.Model;import org.springframework.web.bind.annotation.*;import java.security.Principal;import vn.tuan.service.StorefrontService;
@Controller @RequestMapping("/partner") @RequiredArgsConstructor public class PartnerController {
 private final StorefrontService storefront;
 @GetMapping("/register")public String form(Model m,Principal p){var shop=storefront.ownedShop(p.getName());if(shop!=null){m.addAttribute("shop",shop);return "partner/status";}m.addAttribute("account",storefront.account(p.getName()));return "partner/register";}
 @PostMapping("/register")public String submit(Principal p,@RequestParam String shopName,@RequestParam(required=false)String description,@RequestParam(required=false)String logoUrl){storefront.registerShop(p.getName(),shopName,description,logoUrl);return "redirect:/partner/register";}
}
