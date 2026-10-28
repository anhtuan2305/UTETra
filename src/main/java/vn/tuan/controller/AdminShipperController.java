package vn.tuan.controller;
import lombok.RequiredArgsConstructor;import org.springframework.stereotype.Controller;import org.springframework.ui.Model;import org.springframework.web.bind.annotation.*;import vn.tuan.service.AdminService;
@Controller @RequestMapping("/admin/shippers") @RequiredArgsConstructor public class AdminShipperController {
 private final AdminService admin;
 @GetMapping public String list(Model m){m.addAttribute("pendingShippers",admin.shippers(false));m.addAttribute("activeShippers",admin.shippers(true));return "admin/shippers";}
 @PostMapping("/{id}/approve")public String approve(@PathVariable Integer id){admin.shipperStatus(id,true);return "redirect:/admin/shippers";}
 @PostMapping("/{id}/lock")public String lock(@PathVariable Integer id){admin.shipperStatus(id,false);return "redirect:/admin/shippers";}
}
