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

    // 1. Lấy hoặc tạo mới giỏ hàng (Hỗ trợ cả Guest và Tài khoản thành viên)
    @Transactional
    public Cart getOrCreateCart(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return null;
        }

        // Trường hợp khách vãng lai (Guest) qua Session ID
        if (identifier.startsWith("GUEST_")) {
            return cartRepository.findBySessionId(identifier).orElseGet(() -> {
                Cart newCart = Cart.builder()
                        .sessionId(identifier)
                        .account(null)
                        .items(new ArrayList<>())
                        .build();
                return cartRepository.save(newCart);
            });
        }

        // Trường hợp người dùng đã đăng nhập (Email)
        Account account = accountRepository.findByEmail(identifier)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản: " + identifier));

        return cartRepository.findByAccount(account).orElseGet(() -> {
            Cart newCart = Cart.builder()
                    .account(account)
                    .sessionId(null)
                    .items(new ArrayList<>())
                    .build();
            return cartRepository.save(newCart);
        });
    }

    // 2. Đếm tổng số lượng món trong giỏ để hiển thị lên Header Badge
    @Transactional(readOnly = true)
    public int countItemsInCart(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return 0;
        }
        try {
            Cart cart = getOrCreateCart(identifier);
            if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
                return 0;
            }
            return cart.getItems().stream().mapToInt(CartItem::getQuantity).sum();
        } catch (Exception e) {
            return 0;
        }
    }

    // 3. Thêm món trà sữa kèm các tùy chọn (Size, Đường, Đá, Topping) vào giỏ
    @Transactional
    public void addToCart(String identifier, Integer productId, Integer quantity, 
                          String size, String sugar, String ice, List<String> toppings) {
        Cart cart = getOrCreateCart(identifier);
        if (cart == null) {
            throw new RuntimeException("Không thể khởi tạo giỏ hàng cho phiên làm việc hiện tại!");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy món có ID: " + productId));

        // Chuẩn hóa chuỗi snapshot tùy chọn món
        StringBuilder optionsBuilder = new StringBuilder();
        optionsBuilder.append(size != null ? size : "Size M").append(" | ");
        optionsBuilder.append(sugar != null ? sugar : "100% Đường").append(" | ");
        optionsBuilder.append(ice != null ? ice : "100% Đá");
        
        if (toppings != null && !toppings.isEmpty()) {
            optionsBuilder.append(" | Topping: ").append(String.join(", ", toppings));
        }

        String optionsString = optionsBuilder.toString();

        if (cart.getItems() == null) {
            cart.setItems(new ArrayList<>());
        }

        // Kiểm tra xem món đó với đúng tập tùy chọn đã có trong giỏ chưa
        CartItem existingItem = null;
        for (CartItem item : cart.getItems()) {
            if (item.getProduct().getId().equals(productId) && optionsString.equals(item.getOptionsJson())) {
                existingItem = item;
                break;
            }
        }

        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + quantity);
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

    // 4. Cập nhật số lượng (+1 hoặc -1)
    @Transactional
    public void updateQuantity(Integer itemId, int delta) {
        CartItem item = cartItemRepository.findById(itemId).orElse(null);
        if (item != null) {
            int newQty = item.getQuantity() + delta;
            if (newQty <= 0) {
                cartItemRepository.delete(item);
            } else {
                item.setQuantity(newQty);
                cartItemRepository.save(item);
            }
        }
    }

    // 5. Xóa hẳn một món ra khỏi giỏ
    @Transactional
    public void removeItem(Integer itemId) {
        cartItemRepository.deleteById(itemId);
    }
}