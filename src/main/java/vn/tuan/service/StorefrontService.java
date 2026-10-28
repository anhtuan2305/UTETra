
package vn.tuan.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.tuan.entity.*;
import vn.tuan.repository.*;

import java.util.*;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class StorefrontService {

    private final AccountRepository accounts;
    private final ShopRepository shops;
    private final ProductRepository products;
    private final CategoryRepository categories;
    private final CarrierRepository carriers;
    private final OrderRepository orders;
    private final ReviewRepository reviews;
    private final ActorService actors;

    public Account account(String email) {
        return accounts.findByEmail(email).orElse(null);
    }

    public List<Category> categories() {
        return categories.findAll();
    }

    public List<Shop> activeShops() {
        return shops.findTop10ByStatusOrderByRatingDesc("ACTIVE");
    }

    public List<Carrier> carriers() {
        return carriers.findAll();
    }

    public List<Review> reviews(Product product) {
        return reviews.findByProductOrderByCreatedAtDesc(product);
    }

    public Product product(Integer id) {
        return products.findById(id).orElse(null);
    }

    public List<Product> related(Product product) {
        return products.findTop4ByCategoryAndIdNotAndStatus(
                        product.getCategory(),
                        product.getId(),
                        "ACTIVE"
                )
                .stream()
                .filter(p -> "ACTIVE".equals(p.getShop().getStatus()))
                .toList();
    }

    public Order order(Integer id) {
        return orders.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Không tìm thấy đơn"));
    }

    public List<Order> orders(String email) {
        Account account = actors.account(email);

        if (account == null) {
            return List.of();
        }

        return orders.findByAccountOrderByCreatedAtDesc(account);
    }

    public Order ownedOrder(String email, Integer id) {
        Order order = order(id);

        OrderRules.require(
                order.getAccount() != null
                        && order.getAccount().getEmail().equals(email),
                "Đơn không thuộc tài khoản"
        );

        return order;
    }

    public Order lookup(Integer id, String phone) {
        return orders.findByIdAndPhone(id, phone.trim()).orElse(null);
    }

    public Shop ownedShop(String email) {
        return shops.findByOwner(actors.account(email)).orElse(null);
    }

    @Transactional
    public void registerShop(
            String email,
            String name,
            String description,
            String logo) {

        Account account = actors.account(email);

        OrderRules.require(
                account != null
                        && "USER".equals(actors.role(account))
                        && shops.findByOwner(account).isEmpty(),
                "Chỉ khách hàng chưa có quán được đăng ký"
        );

        OrderRules.require(
                name != null
                        && !name.isBlank()
                        && name.length() <= 150
                        && (description == null || description.length() <= 4000),
                "Thông tin quán không hợp lệ"
        );

        OrderRules.require(
                logo == null || logo.isBlank() || logo.startsWith("https://"),
                "Logo phải là URL HTTPS"
        );

        shops.save(
                Shop.builder()
                        .owner(account)
                        .shopName(name)
                        .description(description)
                        .logoUrl(
                                logo == null || logo.isBlank()
                                        ? "/images/tea-placeholder.svg"
                                        : logo
                        )
                        .status("PENDING")
                        .rating(BigDecimal.ZERO)
                        .build()
        );
    }

    @Transactional
    public void profile(String email, String name, String phone) {
        OrderRules.require(
                name != null
                        && !name.isBlank()
                        && name.length() <= 100
                        && phone != null
                        && phone.matches("0[0-9]{9}"),
                "Họ tên hoặc số điện thoại không hợp lệ"
        );

        Account account = actors.account(email);

        OrderRules.require(account != null, "Không tìm thấy tài khoản");

        account.setFullName(name.trim());
        account.setPhone(phone);
    }

    public List<Order> deliveries(String email) {
        Account account = actors.account(email);

        OrderRules.require(
                account != null
                        && "SHIPPER".equals(actors.role(account)),
                "Chỉ tài khoản Shipper mới được xem đơn giao hàng"
        );

        return orders.findByStatusInOrderByCreatedAtDesc(
                        List.of("READY", "SHIPPING", "COMPLETED")
                )
                .stream()
                .filter(order ->
                        ("READY".equals(order.getStatus())
                                && order.getShipper() == null)
                        || (order.getShipper() != null
                                && order.getShipper().getId()
                                        .equals(account.getId()))
                )
                .toList();
    }
}
