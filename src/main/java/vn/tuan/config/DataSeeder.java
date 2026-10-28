
package vn.tuan.config;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import vn.tuan.entity.*;
import vn.tuan.repository.*;

@Component
@ConditionalOnProperty(
    name = "app.seed.enabled",
    havingValue = "true"
)
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final AccountRepository accounts;
    private final ShopRepository shops;
    private final ProductRepository products;
    private final CategoryRepository categories;
    private final CarrierRepository carriers;
    private final PromotionRepository promotions;
    private final PasswordEncoder passwords;

    private Account account(String email, String role) {
        return accounts.findByEmail(email)
                .orElseGet(() -> accounts.save(
                        Account.builder()
                                .email(email)
                                .password(passwords.encode("Utetra@123"))
                                .role(role)
                                .isActive(true)
                                .emailVerified(true)
                                .build()
                ));
    }

    @Override
    @Transactional
    public void run(String... args) {

        // 1. Khoi tao tai khoan
        account("admin@utetra.vn", "ADMIN");
        account("user@utetra.vn", "USER");
        account("shipper1@utetra.vn", "SHIPPER");
        account("shipper2@utetra.vn", "SHIPPER");

        Account manager = account(
                "manager@utetra.vn",
                "MANAGER"
        );

        // 2. Khoi tao don vi van chuyen
        if (carriers.count() == 0) {
            carriers.save(
                    new Carrier(
                            "UTETra Express",
                            new BigDecimal("15000"),
                            true
                    )
            );
        }

        // 3. Khoi tao danh muc san pham
        if (categories.count() == 0) {

            categories.save(
                    Category.builder()
                            .name("Trà sữa")
                            .build()
            );

            categories.save(
                    Category.builder()
                            .name("Trà trái cây")
                            .build()
            );

            categories.save(
                    Category.builder()
                            .name("Ô long")
                            .build()
            );
        }

        List<Category> cs = categories.findAll();

        // 4. Duong dan anh san pham
        String[] photos = {
                "/uploads/products/1791698269049_Tr%C3%A0%20sua%20khoai%20mon%20hoang%20kim.jpg",
                "/uploads/products/1791698893043_tr%C3%A0%20s%E1%BB%AFa%20th%C3%A1i%20xanh.jpg",
                "/uploads/products/1791699141701_tr%C3%A0%20s%E1%BB%AFa%20th%C3%A1i%20xanh.jpg",
                "/uploads/products/1791799500122_Tr__S_a_Th_i__xanh.jpg",
                "/uploads/products/1791799588439_tr__chanh_d_y.jpg",
                "/uploads/products/1791799900962_tr__xanh_nh_i.jpg",
                "/uploads/products/1791800081664_tr____long.jpg",
                "/uploads/products/1791800247219_tr__s_a_th_i_xanh_m_i.jpg",
                "/uploads/products/1791800759989_tr__s_a_khoai_m_n.jpg",
                "/uploads/products/1791800925230_tr__xanh_nh_i.jpg",
                "/uploads/products/1791801032667_tr__s_a_chocolate.jpg",
                "/uploads/products/1791801150199_tr__chanh.jpg",
                "/uploads/products/1791801263201_tr__b___ao.jpg",
                "/uploads/products/1791801449909_olong_latte_v_i.jpg"
        };

        // 5. Khoi tao cua hang va san pham
        for (int branch = 1; branch <= 2; branch++) {

            Account owner = account(
                    "shop" + branch + "@utetra.vn",
                    "VENDOR"
            );

            Shop s = shops.findByOwner(owner).orElse(null);

            if (s == null) {

                s = Shop.builder()
                        .owner(owner)
                        .manager(branch == 1 ? manager : null)
                        .shopName(
                                "UTETra — " +
                                (branch == 1 ? "Thủ Đức" : "Bình Thạnh")
                        )
                        .status("ACTIVE")
                        .rating(BigDecimal.ZERO)
                        .commissionPercent(new BigDecimal("5"))
                        .logoUrl("/images/tea-placeholder.svg")
                        .build();

                shops.save(s);
            }

            if (products.findByShopOrderByCreatedAtDesc(s).isEmpty()) {

                String[] names = {
                        "Trà sữa trân châu",
                        "Trà đào cam sả",
                        "Ô long sữa",
                        "Trà lài",
                        "Sữa tươi đường đen",
                        "Trà vải",
                        "Trà sữa khoai môn",
                        "Trà dâu",
                        "Trà matcha",
                        "Trà nhãn"
                };

                for (int i = 0; i < 24; i++) {

                    Product p = Product.builder()
                            .shop(s)
                            .category(cs.get(i % cs.size()))
                            .name(
                                    names[i % names.length] +
                                    (i >= 10
                                            ? " đặc biệt " + (i / 10)
                                            : "")
                            )
                            .basePrice(
                                    BigDecimal.valueOf(
                                            25000 + (i % 6) * 5000
                                    )
                            )
                            .stock(100)
                            .soldCount(12 + i)
                            .description(
                                    "Đồ uống mẫu phục vụ kiểm thử. " +
                                    "Giá cơ bản áp dụng size M."
                            )
                            .imageUrl(
                                    photos.length == 0
                                            ? "/images/tea-placeholder.svg"
                                            : photos[i % photos.length]
                            )
                            .status("ACTIVE")
                            .build();

                    products.save(p);
                }
            }
        }

        // 6. Khoi tao ma khuyen mai
        if (promotions.count() == 0) {

            Promotion discount = new Promotion(
                    "UTETRA10",
                    10,
                    new BigDecimal("20000"),
                    new BigDecimal("50000"),
                    true
            );

            promotions.save(discount);

            Promotion freeship = new Promotion(
                    "FREESHIP",
                    100,
                    new BigDecimal("15000"),
                    new BigDecimal("60000"),
                    true
            );

            freeship.setType("SHIPPING");

            promotions.save(freeship);
        }
    }
}
