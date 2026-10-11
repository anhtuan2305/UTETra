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

        // 1. Kiểm tra trạng thái đăng nhập thực tế
        boolean isAuthenticated = authentication != null 
                && authentication.isAuthenticated() 
                && authentication.getAuthorities().stream()
                    .noneMatch(a -> a.getAuthority().equals("ROLE_ANONYMOUS"));

        if (isAuthenticated) {
            model.addAttribute("currentUser", authentication.getName());
        }
        model.addAttribute("isAuthenticated", isAuthenticated);

        // 2. Lấy danh mục và top gian hàng
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("shops", shopRepository.findTop10ByStatusOrderByRatingDesc("ACTIVE"));

        // 3. Phân luồng dữ liệu hiển thị theo đối tượng
        Page<Product> productPage;

        if (keyword != null && !keyword.trim().isEmpty()) {
            // Khi có từ khóa tìm kiếm: ưu tiên kết quả tìm kiếm theo lượt bán
            int pageSize = isAuthenticated ? 20 : 8;
            productPage = productRepository.findByNameContainingIgnoreCaseAndStatus(
                    keyword.trim(), "ACTIVE", PageRequest.of(page, pageSize, Sort.by("soldCount").descending())
            );
            model.addAttribute("keyword", keyword.trim());
        } else {
            if (!isAuthenticated) {
                // GUEST: Chỉ hiển thị các món bán > 10 ly, xếp từ cao xuống thấp (8 món/trang)
                productPage = productRepository.findByStatusAndSoldCountGreaterThanOrderBySoldCountDesc(
                        "ACTIVE", 10, PageRequest.of(page, 8)
                );
            } else {
                // USER ĐÃ ĐĂNG NHẬP: Hiển thị 20 sản phẩm mới nhất / bán chạy nhất có phân trang
                productPage = productRepository.findByStatusOrderByCreatedAtDesc(
                        "ACTIVE", PageRequest.of(page, 20)
                );
            }
        }

        model.addAttribute("productPage", productPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productPage.getTotalPages());

        return "index";
    }
}