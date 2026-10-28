package vn.tuan.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.tuan.entity.Account;
import vn.tuan.entity.Cart;
import vn.tuan.entity.CartItem;
import vn.tuan.entity.Product;
import vn.tuan.repository.AccountRepository;
import vn.tuan.repository.CartItemRepository;
import vn.tuan.repository.CartRepository;
import vn.tuan.repository.ProductRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ProductRepository productRepository;

    private String newMemberCartKey() {
        return "MEMBER_" + UUID.randomUUID();
    }

    private void ensureItems(Cart cart) {
        if (cart.getItems() == null) {
            cart.setItems(new ArrayList<>());
        }
    }

    // 1. Lấy hoặc tạo giỏ hàng.
    @Transactional
    public Cart getOrCreateCart(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return null;
        }

        // Giỏ khách chưa đăng nhập.
        if (identifier.startsWith("GUEST_")) {
            return cartRepository.findBySessionId(identifier)
                    .orElseGet(() -> {
                        Cart newCart = Cart.builder()
                                .sessionId(identifier)
                                .account(null)
                                .items(new ArrayList<>())
                                .build();

                        return cartRepository.save(newCart);
                    });
        }

        // Giỏ tài khoản thành viên.
        Account account = accountRepository.findByEmail(identifier)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy tài khoản: " + identifier
                ));

        Cart cart = cartRepository.findByAccount(account)
                .orElse(null);

        if (cart == null) {
            cart = Cart.builder()
                    .account(account)
                    .sessionId(newMemberCartKey())
                    .items(new ArrayList<>())
                    .build();

            return cartRepository.save(cart);
        }

        ensureItems(cart);

        // Cập nhật giỏ thành viên cũ đang có sessionId rỗng.
        if (cart.getSessionId() == null
                || cart.getSessionId().isBlank()) {
            cart.setSessionId(newMemberCartKey());
        }

        return cart;
    }

    // 2. Đếm số lượng món trên header.
    @Transactional(readOnly = true)
    public int countItemsInCart(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return 0;
        }

        try {
            Cart cart;

            if (identifier.startsWith("GUEST_")) {
                cart = cartRepository.findBySessionId(identifier)
                        .orElse(null);
            } else {
                cart = accountRepository.findByEmail(identifier)
                        .flatMap(cartRepository::findByAccount)
                        .orElse(null);
            }

            if (cart == null
                    || cart.getItems() == null
                    || cart.getItems().isEmpty()) {
                return 0;
            }

            return cart.getItems()
                    .stream()
                    .mapToInt(CartItem::getQuantity)
                    .sum();
        } catch (Exception ex) {
            return 0;
        }
    }

    // 3. Thêm món và tùy chọn vào giỏ.
    @Transactional
    public void addToCart(
            String identifier,
            Integer productId,
            Integer quantity,
            String size,
            String sugar,
            String ice,
            List<String> toppings) {

        OptionPricing.validate(size, sugar, ice, toppings);

        if (toppings != null) {
            toppings = toppings.stream().sorted().toList();
        }

        Cart cart = getOrCreateCart(identifier);

        if (cart == null) {
            throw new IllegalArgumentException(
                    "Không thể khởi tạo giỏ hàng cho phiên làm việc hiện tại!"
            );
        }

        ensureItems(cart);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy món có ID: " + productId
                ));

        OrderRules.quantity(quantity, product.getStock());

        OrderRules.require(
                "ACTIVE".equals(product.getStatus())
                        && "ACTIVE".equals(product.getShop().getStatus()),
                "Món hoặc cửa hàng đã ngừng bán"
        );

        for (CartItem item : cart.getItems()) {
            OrderRules.require(
                    item.getProduct().getShop().getId()
                            .equals(product.getShop().getId()),
                    "Mỗi đơn chỉ mua tại một chi nhánh. "
                            + "Vui lòng thanh toán hoặc xóa giỏ trước khi đổi quán."
            );
        }

        StringBuilder optionsBuilder = new StringBuilder();

        optionsBuilder.append(size != null ? size : "Size M")
                .append(" | ");

        optionsBuilder.append(
                sugar != null ? sugar : "100% Đường"
        ).append(" | ");

        optionsBuilder.append(
                ice != null ? ice : "100% Đá"
        );

        if (toppings != null && !toppings.isEmpty()) {
            optionsBuilder.append(" | Topping: ")
                    .append(String.join(", ", toppings));
        }

        String optionsString = optionsBuilder.toString();

        CartItem existingItem = null;

        for (CartItem item : cart.getItems()) {
            if (item.getProduct().getId().equals(productId)
                    && optionsString.equals(item.getOptionsJson())) {
                existingItem = item;
                break;
            }
        }

        if (existingItem != null) {
            int newQuantity = existingItem.getQuantity() + quantity;

            OrderRules.quantity(newQuantity, product.getStock());

            existingItem.setQuantity(newQuantity);
            cartItemRepository.save(existingItem);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(quantity)
                    .optionsJson(optionsString)
                    .build();

            cart.getItems().add(newItem);
            cartItemRepository.save(newItem);
        }
    }

    // Kiểm tra quyền sở hữu dòng giỏ hàng.
    private void owns(CartItem item, String identifier) {
        Cart cart = item.getCart();

        boolean owner = false;

        if (identifier != null && !identifier.isBlank()) {
            if (identifier.startsWith("GUEST_")) {
                owner = cart.getAccount() == null
                        && identifier.equals(cart.getSessionId());
            } else {
                owner = cart.getAccount() != null
                        && identifier.equals(
                                cart.getAccount().getEmail()
                        );
            }
        }

        OrderRules.require(
                owner,
                "Giỏ hàng không thuộc phiên của bạn"
        );
    }

    // 4. Tăng hoặc giảm số lượng.
    @Transactional
    public void updateQuantity(
            String identifier,
            Integer itemId,
            int delta) {

        CartItem item = cartItemRepository.findById(itemId)
                .orElse(null);

        if (item == null) {
            return;
        }

        owns(item, identifier);

        OrderRules.require(
                delta == 1 || delta == -1,
                "Chỉ tăng hoặc giảm một món"
        );

        int newQuantity = item.getQuantity() + delta;

        if (newQuantity <= 0) {
            cartItemRepository.delete(item);
        } else {
            OrderRules.quantity(
                    newQuantity,
                    item.getProduct().getStock()
            );

            item.setQuantity(newQuantity);
            cartItemRepository.save(item);
        }
    }

    // 5. Xóa món khỏi giỏ.
    @Transactional
    public void removeItem(String identifier, Integer itemId) {
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy món trong giỏ"
                ));

        owns(item, identifier);
        cartItemRepository.delete(item);
    }

    // 6. Gộp giỏ khách vào giỏ thành viên sau đăng nhập.
    @Transactional
    public void mergeGuest(String email, String guestKey) {
        if (guestKey == null
                || guestKey.isBlank()
                || !guestKey.startsWith("GUEST_")) {
            return;
        }

        Cart guest = cartRepository.findBySessionId(guestKey)
                .orElse(null);

        if (guest == null || guest.getAccount() != null) {
            return;
        }

        ensureItems(guest);

        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy tài khoản"
                ));

        Cart member = cartRepository.findByAccount(account)
                .orElse(null);

        // Chưa có giỏ thành viên:
        // chuyển giỏ khách thành giỏ thành viên.
        if (member == null) {
            guest.setAccount(account);

            // Cấp mã riêng thay vì gán null.
            guest.setSessionId(newMemberCartKey());

            cartRepository.save(guest);
            return;
        }

        ensureItems(member);

        // Sửa mã rỗng của giỏ thành viên cũ nếu có.
        if (member.getSessionId() == null
                || member.getSessionId().isBlank()) {
            member.setSessionId(newMemberCartKey());
        }

        if (guest.getItems().isEmpty()) {
            return;
        }

        // Giỏ thành viên trống: sao chép các món từ giỏ khách.
        if (member.getItems().isEmpty()) {
            for (CartItem item : new ArrayList<>(guest.getItems())) {
                member.getItems().add(copyItem(item, member));
            }

            guest.getItems().clear();
            return;
        }

        // Khác chi nhánh: giữ nguyên cả hai giỏ.
        Integer memberShopId = member.getItems()
                .get(0)
                .getProduct()
                .getShop()
                .getId();

        Integer guestShopId = guest.getItems()
                .get(0)
                .getProduct()
                .getShop()
                .getId();

        if (!memberShopId.equals(guestShopId)) {
            return;
        }

        // Cùng chi nhánh: gộp những dòng có cùng món và tùy chọn.
        for (CartItem guestItem : new ArrayList<>(guest.getItems())) {
            CartItem existingItem = member.getItems()
                    .stream()
                    .filter(item ->
                            item.getProduct().getId().equals(
                                    guestItem.getProduct().getId()
                            )
                            && Objects.equals(
                                    item.getOptionsJson(),
                                    guestItem.getOptionsJson()
                            )
                    )
                    .findFirst()
                    .orElse(null);

            if (existingItem != null) {
                existingItem.setQuantity(
                        Math.min(
                                99,
                                existingItem.getQuantity()
                                        + guestItem.getQuantity()
                        )
                );
            } else {
                member.getItems().add(
                        copyItem(guestItem, member)
                );
            }
        }

        // orphanRemoval sẽ xóa các dòng giỏ khách đã gộp.
        guest.getItems().clear();
    }

    private CartItem copyItem(CartItem source, Cart target) {
        return CartItem.builder()
                .cart(target)
                .product(source.getProduct())
                .quantity(source.getQuantity())
                .optionsJson(source.getOptionsJson())
                .build();
    }
}