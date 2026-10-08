package vn.tuan.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import vn.tuan.entity.*;
import vn.tuan.repository.*;

import java.math.BigDecimal;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private ShopRepository shopRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Chỉ nạp nếu chưa có danh mục nào
        if (categoryRepository.count() == 0) {
            // 1. Tạo Category mẫu
            Category c1 = categoryRepository.save(Category.builder().name("Trà Sữa Truyền Thống").imageUrl("https://images.unsplash.com/photo-1558857563-b37cf5ef7b98?w=300").build());
            Category c2 = categoryRepository.save(Category.builder().name("Trà Trái Cây Tươi").imageUrl("https://images.unsplash.com/photo-1513558161293-cdaf765ed2fd?w=300").build());
            Category c3 = categoryRepository.save(Category.builder().name("Trà Ô Long & Macchiato").imageUrl("https://images.unsplash.com/photo-1544787219-7f47ccb76574?w=300").build());

            // 2. Tạo tài khoản Vendor & Shop mẫu
            Account vendorAcc = accountRepository.findByEmail("shop1@utetra.vn").orElseGet(() -> {
                return accountRepository.save(Account.builder()
                        .email("shop1@utetra.vn")
                        .password(passwordEncoder.encode("123456"))
                        .role("VENDOR")
                        .isActive(true)
                        .build());
            });

            Shop shop1 = shopRepository.save(Shop.builder()
                    .owner(vendorAcc)
                    .shopName("Trà Sữa Đô Đô - Chi nhánh Thủ Đức")
                    .description("Thế giới trà sữa đồng giá siêu thơm ngon")
                    .logoUrl("https://images.unsplash.com/photo-1541658016709-82535e94bc69?w=150")
                    .status("ACTIVE")
                    .rating(BigDecimal.valueOf(4.9))
                    .build());

            // 3. Tạo một số sản phẩm trà sữa mẫu
            String[] names = {
                "Trà Sữa Nướng Trân Châu Đen", "Trà Đào Cam Sả Tươi", "Ô Long Sữa Thiết Quan Âm",
                "Hồng Trà Sữa Macchiato", "Trà Xanh Nhài Kem Cheese", "Trà Sữa Thái Xanh Thạch",
                "Trà Chanh Dây Kim Quất", "Trà Sữa Khoai Môn Hoàng Kim"
            };
            BigDecimal[] prices = {
                new BigDecimal("28000"), new BigDecimal("32000"), new BigDecimal("35000"),
                new BigDecimal("30000"), new BigDecimal("38000"), new BigDecimal("25000"),
                new BigDecimal("29000"), new BigDecimal("35000")
            };
            String[] imgs = {
                "https://images.unsplash.com/photo-1558857563-b37cf5ef7b98?w=500",
                "https://images.unsplash.com/photo-1513558161293-cdaf765ed2fd?w=500",
                "https://images.unsplash.com/photo-1544787219-7f47ccb76574?w=500",
                "https://images.unsplash.com/photo-1576092768241-dec231879fc3?w=500",
                "https://images.unsplash.com/photo-1556679343-c7306c1976bc?w=500",
                "https://images.unsplash.com/photo-1563227812-0ea4c22e6cc8?w=500",
                "https://images.unsplash.com/photo-1621263764928-df1444c5e859?w=500",
                "https://images.unsplash.com/photo-1595981267035-7b04ca84a82d?w=500"
            };

            for (int i = 0; i < names.length; i++) {
                productRepository.save(Product.builder()
                        .shop(shop1)
                        .category(i % 2 == 0 ? c1 : c2)
                        .name(names[i])
                        .description("Ly trà sữa thơm ngậy đượm vị lá trà cao nguyên, topping đầy đặn.")
                        .basePrice(prices[i])
                        .stock(100)
                        .imageUrl(imgs[i])
                        .status("ACTIVE")
                        .soldCount(15 + i * 8)
                        .build());
            }
        }
    }
}