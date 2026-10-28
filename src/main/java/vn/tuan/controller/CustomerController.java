package vn.tuan.controller;
import lombok.RequiredArgsConstructor;import org.springframework.stereotype.Controller;import org.springframework.web.bind.annotation.*;import org.springframework.ui.Model;import org.springframework.web.multipart.MultipartFile;import java.security.Principal;import vn.tuan.service.*;
@Controller @RequiredArgsConstructor public class CustomerController {
 private final CustomerService customers;
 @GetMapping("/my-products") public String history(Model m,Principal p){m.addAttribute("activities",customers.history(p.getName()));return "my-products";}
 @PostMapping("/favorite/{id}") public String favorite(@PathVariable Integer id,Principal p){customers.activity(p.getName(),id,true);return "redirect:/products/"+id;}
 @PostMapping("/reviews") public String review(@RequestParam Integer orderId,@RequestParam Integer productId,@RequestParam int rating,@RequestParam String comment,@RequestParam(required=false) MultipartFile media,Principal p){customers.review(p.getName(),orderId,productId,rating,comment,media);return "redirect:/products/"+productId;}
}
