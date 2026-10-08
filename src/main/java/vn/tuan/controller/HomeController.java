package vn.tuan.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import vn.tuan.entity.Product;
import vn.tuan.repository.CategoryRepository;
import vn.tuan.repository.ProductRepository;
import vn.tuan.repository.ShopRepository;

@Controller
public class HomeController {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private ProductRepository productRepository;

    @GetMapping({"/", "/home"})
    public String homePage(@RequestParam(name = "keyword", required = false) String keyword,
                           @RequestParam(name = "page", defaultValue = "0") int page,
                           Model model,
                           Authentication authentication) {
        
        // 1. Kiểm tra tài khoản đăng nhập
        if (authentication != null && authentication.isAuthenticated()) {
            model.addAttribute("currentUser", authentication.getName());
        }

        // 2. Lấy danh mục và top gian hàng
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("shops", shopRepository.findTop10ByStatusOrderByRatingDesc("ACTIVE"));

        // 3. Lấy sản phẩm có phân trang (mỗi trang 8 sản phẩm, sắp xếp theo lượt bán giảm dần)
        Page<Product> productPage;
        if (keyword != null && !keyword.trim().isEmpty()) {
            productPage = productRepository.findByNameContainingIgnoreCaseAndStatus(
                    keyword.trim(), "ACTIVE", PageRequest.of(page, 8, Sort.by("soldCount").descending())
            );
            model.addAttribute("keyword", keyword);
        } else {
            productPage = productRepository.findByStatus(
                    "ACTIVE", PageRequest.of(page, 8, Sort.by("soldCount").descending())
            );
        }

        model.addAttribute("productPage", productPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productPage.getTotalPages());

        return "index";
    }
}