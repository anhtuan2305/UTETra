package vn.tuan;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import vn.tuan.dto.CheckoutDTO;
import vn.tuan.dto.RegisterDTO;
import vn.tuan.entity.Account;
import vn.tuan.entity.Carrier;
import vn.tuan.entity.Category;
import vn.tuan.entity.Order;
import vn.tuan.entity.Product;
import vn.tuan.entity.Promotion;
import vn.tuan.entity.Shop;
import vn.tuan.repository.AccountRepository;
import vn.tuan.repository.CarrierRepository;
import vn.tuan.repository.CartRepository;
import vn.tuan.repository.CategoryRepository;
import vn.tuan.repository.ProductRepository;
import vn.tuan.repository.PromotionRepository;
import vn.tuan.repository.ShopRepository;
import vn.tuan.service.AccountService;
import vn.tuan.service.CartService;
import vn.tuan.service.OrderLifecycleService;
import vn.tuan.service.OrderService;
import vn.tuan.service.QuoteService;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderWorkflowIntegrationTest {

    @Autowired
    private AccountRepository accounts;

    @Autowired
    private ShopRepository shops;

    @Autowired
    private CategoryRepository categories;

    @Autowired
    private ProductRepository products;

    @Autowired
    private CarrierRepository carriers;

    @Autowired
    private CartRepository carts;

    @Autowired
    private CartService cartService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderLifecycleService lifecycle;

    @Autowired
    private AccountService accountService;

    @Autowired
    private QuoteService quotes;

    @Autowired
    private PromotionRepository promotions;

    private Account user;
    private Account vendor;
    private Account driver;
    private Account other;

    private Shop shop;
    private Product product;
    private Carrier carrier;

    private Account createAccount(String role) {
        return accounts.save(
                Account.builder()
                        .email(UUID.randomUUID() + "@test.vn")
                        .password("hash")
                        .role(role)
                        .emailVerified(true)
                        .isActive(true)
                        .build()
        );
    }

    @BeforeEach
    void setup() {
        user = createAccount("USER");
        vendor = createAccount("VENDOR");
        driver = createAccount("SHIPPER");
        other = createAccount("SHIPPER");

        shop = shops.save(
                Shop.builder()
                        .owner(vendor)
                        .shopName("Test")
                        .status("ACTIVE")
                        .commissionPercent(new BigDecimal("5"))
                        .build()
        );

        Category category = categories.save(
                Category.builder()
                        .name("Tea")
                        .build()
        );

        product = products.save(
                Product.builder()
                        .shop(shop)
                        .category(category)
                        .name("Milk tea")
                        .basePrice(new BigDecimal("25000"))
                        .stock(3)
                        .soldCount(0)
                        .build()
        );

        carrier = carriers.save(
                new Carrier(
                        "Delivery",
                        new BigDecimal("15000"),
                        true
                )
        );
    }

    private CheckoutDTO checkoutDTO() {
        CheckoutDTO dto = new CheckoutDTO(
                shop.getId(),
                "Test",
                "0912345678",
                "1 Test",
                "HCM",
                "COD",
                ""
        );

        dto.setCarrierId(carrier.getId());
        return dto;
    }

    private Order buy() {
        cartService.addToCart(
                user.getEmail(),
                product.getId(),
                1,
                "Size L",
                null,
                null,
                List.of("Trân châu đen (+5.000đ)")
        );

        return orderService.placeOrder(
                user.getEmail(),
                checkoutDTO()
        );
    }

    @Test
    void accuratePriceAndCancelRestoresStock() {
        Order order = buy();

        assertEquals(
                0,
                new BigDecimal("51000").compareTo(order.getFinalAmount())
        );
        assertEquals(2, product.getStock());

        lifecycle.cancel(user.getEmail(), order.getId());

        assertEquals(3, product.getStock());
        assertEquals(0, product.getSoldCount());

        assertThrows(
                IllegalArgumentException.class,
                () -> lifecycle.cancel(user.getEmail(), order.getId())
        );
    }

    @Test
    void onlyAssignedShipperCanComplete() {
        Order order = buy();

        assertThrows(
                IllegalArgumentException.class,
                () -> lifecycle.delivery(
                        driver.getEmail(),
                        order.getId(),
                        "SHIPPING"
                )
        );

        lifecycle.vendor(
                vendor.getEmail(),
                order.getId(),
                "CONFIRMED"
        );

        lifecycle.vendor(
                vendor.getEmail(),
                order.getId(),
                "PREPARING"
        );

        lifecycle.vendor(
                vendor.getEmail(),
                order.getId(),
                "READY"
        );

        lifecycle.delivery(
                driver.getEmail(),
                order.getId(),
                "SHIPPING"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> lifecycle.delivery(
                        other.getEmail(),
                        order.getId(),
                        "COMPLETED"
                )
        );

        lifecycle.delivery(
                driver.getEmail(),
                order.getId(),
                "COMPLETED"
        );

        assertEquals("PAID", order.getPaymentStatus());
        assertNotNull(order.getCompletedAt());
    }

    @Test
    void doesNotCheckoutEmptyCartTwice() {
        buy();

        assertThrows(
                IllegalArgumentException.class,
                () -> orderService.placeOrder(
                        user.getEmail(),
                        checkoutDTO()
                )
        );
    }

    @Test
    void protectsCartOwnership() {
        cartService.addToCart(
                user.getEmail(),
                product.getId(),
                1,
                null,
                null,
                null,
                null
        );

        var cart = carts.findByAccount(user).orElseThrow();
        Integer itemId = cart.getItems().get(0).getId();

        assertThrows(
                IllegalArgumentException.class,
                () -> cartService.removeItem(other.getEmail(), itemId)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> cartService.updateQuantity(
                        other.getEmail(),
                        itemId,
                        1
                )
        );
    }

    @Test
    void rejectsNegativeQuantityAndStockOverflow() {
        assertThrows(
                IllegalArgumentException.class,
                () -> cartService.addToCart(
                        user.getEmail(),
                        product.getId(),
                        -1,
                        null,
                        null,
                        null,
                        null
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> cartService.addToCart(
                        user.getEmail(),
                        product.getId(),
                        4,
                        null,
                        null,
                        null,
                        null
                )
        );
    }

    @Test
    void registrationCannotChooseAdmin() {
        RegisterDTO dto = new RegisterDTO();
        dto.setEmail("bad@test.vn");
        dto.setPassword("Password123");
        dto.setConfirmPassword("Password123");
        dto.setRole("ADMIN");

        assertNotEquals("SUCCESS", accountService.register(dto));
        assertFalse(accounts.existsByEmail("bad@test.vn"));
    }

    @Test
    void shippingVoucherNeverDiscountsMoreThanShipping() {
        Promotion promotion = new Promotion(
                "TESTSHIP",
                100,
                new BigDecimal("30000"),
                BigDecimal.ZERO,
                true
        );

        promotion.setType("SHIPPING");
        promotions.save(promotion);

        var quote = quotes.calculate(
                new BigDecimal("100000"),
                shop.getId(),
                carrier.getId(),
                "TESTSHIP"
        );

        assertEquals(
                0,
                new BigDecimal("15000").compareTo(quote.discount())
        );

        assertEquals(
                0,
                new BigDecimal("100000").compareTo(quote.total())
        );
    }
}