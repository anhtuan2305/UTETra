package vn.tuan.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.tuan.entity.Account;
import vn.tuan.entity.Order;
import vn.tuan.entity.Product;
import vn.tuan.entity.Shop;
import vn.tuan.repository.AccountRepository;
import vn.tuan.repository.OrderDetailRepository;
import vn.tuan.repository.OrderRepository;
import vn.tuan.repository.ProductRepository;
import vn.tuan.repository.ShopRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/vendor")
public class VendorController {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderDetailRepository orderDetailRepository;

    /**
     * Tự động đồng bộ quyền trong bộ nhớ SecurityContext từ Database
     * và lấy đúng Shop của chủ tài khoản hiện tại.
     */
    private Shop syncUserRoleAndGetShop(Authentication authentication) {
        if (authentication == null) return null;

        Account account = accountRepository.findByEmail(authentication.getName()).orElse(null);
        if (account == null) return null;

        Shop shop = shopRepository.findByOwner(account).orElse(null);

        // Kiểm tra xem session hiện tại đã có quyền VENDOR chưa
        boolean hasVendorAuthority = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().contains("VENDOR"));

        // Nếu trong DB đã là VENDOR hoặc quán đã ACTIVE nhưng Session chưa kịp cập nhật: Tự nạp quyền mới
        if (!hasVendorAuthority && ("VENDOR".equalsIgnoreCase(account.getRole()) || "ROLE_VENDOR".equalsIgnoreCase(account.getRole()) 
                || (shop != null && "ACTIVE".equalsIgnoreCase(shop.getStatus())))) {

            // Đồng bộ luôn DB nếu chưa đổi role
            if (!"ROLE_VENDOR".equalsIgnoreCase(account.getRole()) && !"VENDOR".equalsIgnoreCase(account.getRole())) {
                account.setRole("ROLE_VENDOR");
                accountRepository.save(account);
            }

            Authentication newAuth = new UsernamePasswordAuthenticationToken(
                    authentication.getPrincipal(),
                    authentication.getCredentials(),
                    List.of(new SimpleGrantedAuthority("ROLE_VENDOR"), new SimpleGrantedAuthority("VENDOR"))
            );
            SecurityContextHolder.getContext().setAuthentication(newAuth);
        }

        return shop;
    }

    // 1. Dashboard tổng quan
    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication, RedirectAttributes redirectAttributes) {
        if (authentication == null) return "redirect:/login";

        Shop shop = syncUserRoleAndGetShop(authentication);
        if (shop == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Tài khoản của bạn chưa liên kết với quán nào!");
            return "redirect:/partner/register";
        }

        if (!"ACTIVE".equalsIgnoreCase(shop.getStatus())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Gian hàng đang chờ duyệt hoặc đã bị tạm khóa!");
            return "redirect:/partner/register";
        }

        List<Order> orders = orderRepository.findByShopOrderByCreatedAtDesc(shop);

        // Tính doanh thu các đơn đã hoàn thành (COMPLETED) của chính quán này
        double completedRevenue = orders.stream()
                .filter(o -> "COMPLETED".equalsIgnoreCase(o.getStatus()))
                .filter(o -> o.getFinalAmount() != null)
                .mapToDouble(o -> ((Number) o.getFinalAmount()).doubleValue())
                .sum();

        model.addAttribute("shop", shop);
        model.addAttribute("totalProducts", productRepository.findByShopOrderByCreatedAtDesc(shop).size());
        model.addAttribute("totalOrders", orders.size());
        model.addAttribute("completedRevenue", completedRevenue);
        model.addAttribute("recentOrders", orders.stream().limit(5).toList());

        return "vendor/dashboard";
    }

    // 2. Trang Quản lý Đơn Hàng Của Quán (/vendor/orders)
    @GetMapping("/orders")
    public String listOrders(Model model, Authentication authentication, RedirectAttributes redirectAttributes) {
        if (authentication == null) return "redirect:/login";

        Shop shop = syncUserRoleAndGetShop(authentication);
        if (shop == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Tài khoản của bạn chưa liên kết với quán nào!");
            return "redirect:/partner/register";
        }

        List<Order> orders = orderRepository.findByShopOrderByCreatedAtDesc(shop);

        double completedRevenue = orders.stream()
                .filter(o -> "COMPLETED".equalsIgnoreCase(o.getStatus()))
                .filter(o -> o.getFinalAmount() != null)
                .mapToDouble(o -> ((Number) o.getFinalAmount()).doubleValue())
                .sum();

        model.addAttribute("shop", shop);
        model.addAttribute("orders", orders);
        model.addAttribute("completedRevenue", completedRevenue);

        return "vendor/orders";
    }

    // 3. Quán xác nhận / cập nhật trạng thái đơn (PREPARING, SHIPPING, CANCELLED, COMPLETED)
    @PostMapping("/orders/update-status")
    public String updateOrderStatus(@RequestParam("orderId") Integer orderId,
                                    @RequestParam("status") String status,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        if (authentication == null) return "redirect:/login";

        Shop shop = syncUserRoleAndGetShop(authentication);
        if (shop == null) return "redirect:/login";

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order != null && order.getShop() != null && order.getShop().getId().equals(shop.getId())) {
            order.setStatus(status);
            orderRepository.save(order);
            redirectAttributes.addFlashAttribute("message", "Đã cập nhật trạng thái đơn #" + orderId + " thành: " + status);
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền chỉnh sửa đơn hàng này!");
        }
        return "redirect:/vendor/orders";
    }

    // 4. Quán xóa đơn hàng khỏi lịch sử
    @PostMapping("/orders/delete")
    @Transactional
    public String deleteOrders(@RequestParam(value = "orderIds", required = false) List<Integer> orderIds,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        if (authentication == null) return "redirect:/login";

        if (orderIds == null || orderIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn ít nhất một đơn hàng cần xóa!");
            return "redirect:/vendor/orders";
        }

        Shop shop = syncUserRoleAndGetShop(authentication);
        if (shop == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy quán của bạn!");
            return "redirect:/vendor/orders";
        }

        List<Order> validOrders = orderRepository.findAllById(orderIds).stream()
                .filter(o -> o.getShop() != null && o.getShop().getId().equals(shop.getId()))
                .filter(o -> "COMPLETED".equalsIgnoreCase(o.getStatus()) || "CANCELLED".equalsIgnoreCase(o.getStatus()))
                .toList();

        if (validOrders.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Chỉ có thể xóa các đơn hàng thuộc quán của bạn đã Hoàn Thành hoặc Đã Hủy!");
            return "redirect:/vendor/orders";
        }

        List<Integer> validOrderIds = validOrders.stream().map(Order::getId).toList();

        orderDetailRepository.deleteByOrderIds(validOrderIds);
        orderRepository.deleteAll(validOrders);

        redirectAttributes.addFlashAttribute("message", "Đã xóa thành công " + validOrders.size() + " đơn hàng khỏi lịch sử quán!");
        return "redirect:/vendor/orders";
    }

    // 5. Báo cáo Doanh Thu Theo Từng Ngày (/vendor/finances)
    @GetMapping("/finances")
    public String finances(Model model, Authentication authentication, RedirectAttributes redirectAttributes) {
        if (authentication == null) return "redirect:/login";

        Shop shop = syncUserRoleAndGetShop(authentication);
        if (shop == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Tài khoản của bạn chưa liên kết với quán nào!");
            return "redirect:/partner/register";
        }

        List<Order> orders = orderRepository.findByShopOrderByCreatedAtDesc(shop);

        List<Order> completedOrders = orders.stream()
                .filter(o -> "COMPLETED".equalsIgnoreCase(o.getStatus()))
                .toList();

        double totalRevenue = completedOrders.stream()
                .filter(o -> o.getFinalAmount() != null)
                .mapToDouble(o -> ((Number) o.getFinalAmount()).doubleValue())
                .sum();

        Map<LocalDate, Double> dailyRevenueMap = completedOrders.stream()
                .filter(o -> o.getCreatedAt() != null && o.getFinalAmount() != null)
                .sorted(Comparator.comparing(Order::getCreatedAt).reversed())
                .collect(Collectors.groupingBy(
                        o -> o.getCreatedAt().toLocalDate(),
                        LinkedHashMap::new,
                        Collectors.summingDouble(o -> ((Number) o.getFinalAmount()).doubleValue())
                ));

        Map<LocalDate, Long> dailyOrderCountMap = completedOrders.stream()
                .filter(o -> o.getCreatedAt() != null)
                .sorted(Comparator.comparing(Order::getCreatedAt).reversed())
                .collect(Collectors.groupingBy(
                        o -> o.getCreatedAt().toLocalDate(),
                        LinkedHashMap::new,
                        Collectors.counting()
                ));

        model.addAttribute("shop", shop);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("totalCompletedOrders", completedOrders.size());
        model.addAttribute("dailyRevenueMap", dailyRevenueMap);
        model.addAttribute("dailyOrderCountMap", dailyOrderCountMap);

        return "vendor/finances";
    }

    // 6. Quản Lý Món Của Quán (/vendor/products)
    @GetMapping("/products")
    public String listProducts(Model model, Authentication authentication, RedirectAttributes redirectAttributes) {
        if (authentication == null) return "redirect:/login";

        Shop shop = syncUserRoleAndGetShop(authentication);
        if (shop == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Tài khoản của bạn chưa liên kết với quán nào!");
            return "redirect:/partner/register";
        }

        List<Product> products = productRepository.findByShopOrderByCreatedAtDesc(shop);

        model.addAttribute("shop", shop);
        model.addAttribute("products", products);
        return "vendor/products";
    }

    // 7. Thêm mới / Cập nhật thông tin món
    @PostMapping("/products/save")
    public String saveProduct(@RequestParam(value = "id", required = false) Integer id,
                              @RequestParam("name") String name,
                              @RequestParam("basePrice") BigDecimal basePrice,
                              @RequestParam(value = "description", required = false) String description,
                              @RequestParam(value = "status", defaultValue = "ACTIVE") String status,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        if (authentication == null) return "redirect:/login";

        Shop shop = syncUserRoleAndGetShop(authentication);
        if (shop == null) return "redirect:/login";

        Product product;
        if (id != null) {
            product = productRepository.findById(id).orElse(new Product());
            if (product.getShop() != null && !product.getShop().getId().equals(shop.getId())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền chỉnh sửa món này!");
                return "redirect:/vendor/products";
            }
        } else {
            product = new Product();
            product.setShop(shop);
            product.setCreatedAt(LocalDateTime.now());
            product.setImageUrl("/images/default-tea.png");
        }

        product.setName(name);
        product.setBasePrice(basePrice);
        product.setDescription(description);
        product.setStatus(status);

        productRepository.save(product);
        redirectAttributes.addFlashAttribute("message", "Đã lưu thông tin món thành công!");
        return "redirect:/vendor/products";
    }

    // 8. Xóa món khỏi thực đơn
    @PostMapping("/products/delete")
    public String deleteProduct(@RequestParam("productId") Integer productId,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        if (authentication == null) return "redirect:/login";

        Shop shop = syncUserRoleAndGetShop(authentication);
        if (shop == null) return "redirect:/login";

        Product product = productRepository.findById(productId).orElse(null);
        if (product != null && product.getShop() != null && product.getShop().getId().equals(shop.getId())) {
            try {
                productRepository.delete(product);
                redirectAttributes.addFlashAttribute("message", "Đã xóa món thành công!");
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa món này do đã phát sinh trong đơn hàng của khách. Hãy chuyển trạng thái sang Ngừng Bán!");
            }
        }
        return "redirect:/vendor/products";
    }
}