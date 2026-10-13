package vn.tuan.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.tuan.entity.Account;
import vn.tuan.repository.AccountRepository;

import java.util.List;

@Controller
@RequestMapping("/admin/shippers")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class AdminShipperController {

    @Autowired
    private AccountRepository accountRepository;

    @GetMapping
    public String viewShipperManagement(Model model) {
        // Lấy danh sách Shipper chờ duyệt (isActive = false) và đã duyệt (isActive = true)
        List<Account> pendingList = accountRepository.findByRoleAndIsActiveOrderByCreatedAtDesc("SHIPPER", false);
        List<Account> activeList = accountRepository.findByRoleAndIsActiveOrderByCreatedAtDesc("SHIPPER", true);

        model.addAttribute("pendingShippers", pendingList);
        model.addAttribute("activeShippers", activeList);
        return "admin/shippers";
    }

    // Bấm Duyệt -> Kích hoạt tài khoản
    @PostMapping("/{id}/approve")
    public String approveShipper(@PathVariable("id") Integer id, RedirectAttributes ra) {
        Account account = accountRepository.findById(id).orElse(null);
        if (account != null && "SHIPPER".equalsIgnoreCase(account.getRole())) {
            account.setIsActive(true);
            accountRepository.save(account);
            ra.addFlashAttribute("message", "Đã phê duyệt tài xế: " + account.getEmail());
        }
        return "redirect:/admin/shippers";
    }

    // Bấm Khóa / Thu hồi quyền
    @PostMapping("/{id}/lock")
    public String lockShipper(@PathVariable("id") Integer id, RedirectAttributes ra) {
        Account account = accountRepository.findById(id).orElse(null);
        if (account != null && "SHIPPER".equalsIgnoreCase(account.getRole())) {
            account.setIsActive(false);
            accountRepository.save(account);
            ra.addFlashAttribute("message", "Đã khóa / thu hồi quyền của tài xế: " + account.getEmail());
        }
        return "redirect:/admin/shippers";
    }
}