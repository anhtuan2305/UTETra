package vn.tuan.controller;
import lombok.RequiredArgsConstructor;import org.springframework.stereotype.Controller;import org.springframework.ui.Model;import org.springframework.web.bind.annotation.*;import vn.tuan.service.AdminService;
@Controller @RequestMapping("/admin") @RequiredArgsConstructor public class AdminController {
 private final AdminService admin;
 @GetMapping({"/dashboard","/finances"})public String dashboard(){return "redirect:/console/dashboard";}
 @GetMapping("/orders")public String orders(){return "redirect:/console/orders";}
 @GetMapping("/products")public String products(){return "redirect:/console/products";}
 @GetMapping("/users")public String users(@RequestParam(required=false)String keyword,@RequestParam(defaultValue="0")int page,Model m){var data=admin.users(keyword,page);m.addAttribute("userPage",data);m.addAttribute("currentPage",Math.max(0,page));m.addAttribute("totalPages",data.getTotalPages());m.addAttribute("keyword",keyword);return "admin/users";}
 @PostMapping("/users/toggle-status")public String toggle(@RequestParam Integer userId){admin.toggle(userId);return "redirect:/admin/users";}
 @GetMapping("/shops")public String shops(@RequestParam(required=false)String keyword,@RequestParam(defaultValue="0")int page,Model m){var data=admin.shops(keyword,page);m.addAttribute("shopPage",data);m.addAttribute("currentPage",Math.max(0,page));m.addAttribute("totalPages",data.getTotalPages());m.addAttribute("keyword",keyword);return "admin/shops";}
 @PostMapping("/shops/update-status")public String status(@RequestParam Integer shopId,@RequestParam String status){admin.shopStatus(shopId,status);return "redirect:/admin/shops";}
}
