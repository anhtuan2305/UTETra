package vn.tuan.controller;
import lombok.RequiredArgsConstructor;import org.springframework.stereotype.Controller;import org.springframework.ui.Model;import org.springframework.web.bind.annotation.*;import java.security.Principal;import vn.tuan.service.*;
@Controller @RequiredArgsConstructor public class ProductController {
 private final StorefrontService storefront;private final CustomerService customers;
 @GetMapping("/products/{id}")public String detail(@PathVariable Integer id,Model m,Principal p){var product=storefront.product(id);if(product==null || !"ACTIVE".equals(product.getStatus()) || !"ACTIVE".equals(product.getShop().getStatus()))return "redirect:/";if(p!=null)customers.activity(p.getName(),id,false);m.addAttribute("product",product);m.addAttribute("relatedProducts",storefront.related(product));m.addAttribute("reviews",storefront.reviews(product));return "product-detail";}
}
