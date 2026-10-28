package vn.tuan.controller;
import org.springframework.stereotype.Controller;import org.springframework.web.bind.annotation.GetMapping;
@Controller public class VendorController {
 @GetMapping({"/vendor/dashboard","/vendor/finances"})public String dashboard(){return "redirect:/console/dashboard";}
 @GetMapping("/vendor/orders")public String orders(){return "redirect:/console/orders";}
 @GetMapping("/vendor/products")public String products(){return "redirect:/console/products";}
}
