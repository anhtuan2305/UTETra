package vn.tuan.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.tuan.entity.Account;
import vn.tuan.entity.Order;
import vn.tuan.entity.Product;
import vn.tuan.entity.Shop;
import vn.tuan.repository.AccountRepository;
import vn.tuan.repository.OrderRepository;
import vn.tuan.repository.ProductRepository;
import vn.tuan.repository.ShopRepository;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    // 1. Dashboard Quản trị viên tổng quan
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("totalUsers", accountRepository.count());
        model.addAttribute("totalShops", shopRepository.count());
        model.addAttribute("totalProducts", productRepository.count());
        model.addAttribute("totalOrders", orderRepository.count());

        // Lấy 5 quán mới nhất
        model.addAttribute("recentShops", shopRepository.findAll(PageRequest.of(0, 5, Sort.by("id").descending())).getContent());
        // Lấy 5 user mới nhất
        model.addAttribute("recentUsers", accountRepository.findAll(PageRequest.of(0, 5, Sort.by("id").descending())).getContent());

        return "admin/dashboard";
    }

    // 2. Quản lý Người dùng (UC_027)
    @GetMapping("/users")
    public String listUsers(@RequestParam(name = "keyword", required = false) String keyword,
                            @RequestParam(name = "page", defaultValue = "0") int page,
                            Model model) {
        var pageable = PageRequest.of(page, 10, Sort.by("id").descending());
        var userPage = (keyword != null && !keyword.trim().isEmpty())
                ? accountRepository.findByEmailContainingIgnoreCase(keyword.trim(), pageable)
                : accountRepository.findAll(pageable);

        model.addAttribute("userPage", userPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", userPage.getTotalPages());
        model.addAttribute("keyword", keyword);

        return "admin/users";
    }

    // Khóa / Kích hoạt tài khoản
    @PostMapping("/users/toggle-status")
    public String toggleUserStatus(@RequestParam("userId") Integer userId, RedirectAttributes redirectAttributes) {
        Account acc = accountRepository.findById(userId).orElse(null);
        if (acc != null) {
            acc.setIsActive(!Boolean.TRUE.equals(acc.getIsActive()));
            accountRepository.save(acc);
            redirectAttributes.addFlashAttribute("message", "Đã cập nhật trạng thái tài khoản: " + acc.getEmail());
        }
        return "redirect:/admin/users";
    }

    // 3. Quản lý Cửa hàng (UC_018: Cấp phép hoặc Cấm hoạt động)
    @GetMapping("/shops")
    public String listShops(@RequestParam(name = "keyword", required = false) String keyword,
                            @RequestParam(name = "page", defaultValue = "0") int page,
                            Model model) {
        var pageable = PageRequest.of(page, 10, Sort.by("id").descending());
        var shopPage = (keyword != null && !keyword.trim().isEmpty())
                ? shopRepository.findByShopNameContainingIgnoreCase(keyword.trim(), pageable)
                : shopRepository.findAll(pageable);

        model.addAttribute("shopPage", shopPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", shopPage.getTotalPages());
        model.addAttribute("keyword", keyword);

        return "admin/shops";
    }

    // Duyệt cấp phép (ACTIVE), Chờ duyệt (PENDING) hoặc Cấm (BANNED)
    @PostMapping("/shops/update-status")
    public String updateShopStatus(@RequestParam("shopId") Integer shopId,
                                   @RequestParam("status") String status,
                                   RedirectAttributes redirectAttributes) {
        Shop shop = shopRepository.findById(shopId).orElse(null);
        if (shop != null) {
            shop.setStatus(status);

            // 1. Khi DUYỆT CẤP PHÉP (ACTIVE): Tự động nâng vai trò chủ quán lên ROLE_VENDOR
            if ("ACTIVE".equalsIgnoreCase(status) && shop.getOwner() != null) {
                Account owner = shop.getOwner();
                owner.setRole("ROLE_VENDOR");
                accountRepository.save(owner);
            }

            // 2. Khi KHÓA QUÁN HOẶC TỪ CHỐI (BANNED): Hạ vai trò tài khoản về ROLE_USER
            if ("BANNED".equalsIgnoreCase(status) && shop.getOwner() != null) {
                Account owner = shop.getOwner();
                owner.setRole("ROLE_USER");
                accountRepository.save(owner);
            }

            shopRepository.save(shop);
            redirectAttributes.addFlashAttribute("message", "Đã cập nhật trạng thái quán: " + shop.getShopName() + " sang " + status);
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy quán cần cập nhật!");
        }
        return "redirect:/admin/shops";
    }

    // 4. Quản lý toàn bộ Đơn hàng hệ thống (UC_029)
    @GetMapping("/orders")
    public String listOrders(Model model) {
        model.addAttribute("orders", orderRepository.findAll(Sort.by("createdAt").descending()));
        return "admin/orders";
    }

    // 5. Thống kê Doanh Thu Toàn Sàn dành riêng cho Admin (/admin/finances)
    @GetMapping("/finances")
    public String adminFinances(Model model) {
        List<Order> allOrders = orderRepository.findAll(Sort.by("createdAt").descending());

        List<Order> completedOrders = allOrders.stream()
                .filter(o -> "COMPLETED".equalsIgnoreCase(o.getStatus()))
                .filter(o -> o.getFinalAmount() != null)
                .toList();

        double totalSystemRevenue = completedOrders.stream()
                .mapToDouble(o -> ((Number) o.getFinalAmount()).doubleValue())
                .sum();

        Map<Shop, Double> revenueByShop = completedOrders.stream()
                .filter(o -> o.getShop() != null)
                .collect(Collectors.groupingBy(
                        Order::getShop,
                        Collectors.summingDouble(o -> ((Number) o.getFinalAmount()).doubleValue())
                ));

        Map<LocalDate, Double> dailySystemRevenue = completedOrders.stream()
                .filter(o -> o.getCreatedAt() != null)
                .sorted(Comparator.comparing(Order::getCreatedAt).reversed())
                .collect(Collectors.groupingBy(
                        o -> o.getCreatedAt().toLocalDate(),
                        LinkedHashMap::new,
                        Collectors.summingDouble(o -> ((Number) o.getFinalAmount()).doubleValue())
                ));

        model.addAttribute("totalSystemRevenue", totalSystemRevenue);
        model.addAttribute("totalCompletedOrders", completedOrders.size());
        model.addAttribute("revenueByShop", revenueByShop);
        model.addAttribute("dailySystemRevenue", dailySystemRevenue);

        return "admin/finances";
    }

    // 6. Quản lý Sản phẩm toàn sàn (/admin/products)
    @GetMapping("/products")
    public String listProducts(Model model) {
        model.addAttribute("products", productRepository.findAll(Sort.by("id").descending()));
        model.addAttribute("shops", shopRepository.findAll());
        return "admin/products";
    }

    // 7. Admin Thêm / Cập nhật Sản phẩm
    @PostMapping(value = "/products/save", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String saveProduct(@RequestParam(value = "id", required = false) Integer id,
                              @RequestParam("name") String name,
                              @RequestParam("basePrice") BigDecimal basePrice,
                              @RequestParam(value = "description", required = false) String description,
                              @RequestParam(value = "shopId", required = false) Integer shopId,
                              @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                              @RequestParam(value = "status", defaultValue = "ACTIVE") String status,
                              RedirectAttributes redirectAttributes) {
        Product product;
        if (id != null) {
            product = productRepository.findById(id).orElse(new Product());
        } else {
            product = new Product();
            product.setCreatedAt(LocalDateTime.now());
        }

        if (shopId != null) {
            shopRepository.findById(shopId).ifPresent(product::setShop);
        }

        product.setName(name);
        product.setBasePrice(basePrice);
        product.setDescription(description);
        product.setStatus(status);

        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                Path uploadDir = Paths.get("uploads/products").toAbsolutePath().normalize();
                if (!Files.exists(uploadDir)) {
                    Files.createDirectories(uploadDir);
                }

                String originalFilename = imageFile.getOriginalFilename();
                String cleanName = (originalFilename != null) ? originalFilename.replaceAll("[^a-zA-Z0-9\\.\\-_]", "_") : "tea.png";
                String fileName = System.currentTimeMillis() + "_" + cleanName;

                Path targetLocation = uploadDir.resolve(fileName);
                try (InputStream inputStream = imageFile.getInputStream()) {
                    Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
                }

                product.setImageUrl("/uploads/products/" + fileName);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        productRepository.save(product);
        redirectAttributes.addFlashAttribute("message", "Đã lưu thông tin món thành công!");
        return "redirect:/admin/products";
    }

    // 8. Admin Xóa Sản phẩm
    @PostMapping("/products/delete")
    public String deleteProduct(@RequestParam("productId") Integer productId,
                                RedirectAttributes redirectAttributes) {
        try {
            productRepository.deleteById(productId);
            redirectAttributes.addFlashAttribute("message", "Đã xóa món thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa do món này đã có trong đơn hàng. Vui lòng chuyển trạng thái sang Ngừng Bán!");
        }
        return "redirect:/admin/products";
    }
}