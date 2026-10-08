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

    // Lấy giỏ hàng theo email tài khoản, nếu chưa có thì tạo mới
    public Cart getOrCreateCart(String email) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản: " + email));

        return cartRepository.findByAccount(account).orElseGet(() -> {
            Cart newCart = Cart.builder()
                    .account(account)
                    .build();
            return cartRepository.save(newCart);
        });
    }

    // Đếm tổng số lượng món trong giỏ hàng để hiển thị lên Header Badge
    public int countItemsInCart(String email) {
        if (email == null) return 0;
        try {
            Cart cart = getOrCreateCart(email);
            return cart.getItems().stream().mapToInt(CartItem::getQuantity).sum();
        } catch (Exception e) {
            return 0;
        }
    }

    // Thêm món trà sữa kèm các tùy chọn (Size, Đường, Đá, Topping) vào giỏ
    @Transactional
    public void addToCart(String email, Integer productId, Integer quantity, 
                          String size, String sugar, String ice, List<String> toppings) {
        Cart cart = getOrCreateCart(email);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy món có ID: " + productId));

        // Gom toàn bộ tùy chọn thành chuỗi Text mô tả rõ ràng
        StringBuilder optionsBuilder = new StringBuilder();
        optionsBuilder.append(size != null ? size : "Size M").append(" | ");
        optionsBuilder.append(sugar != null ? sugar : "100% Đường").append(" | ");
        optionsBuilder.append(ice != null ? ice : "100% Đá");
        
        if (toppings != null && !toppings.isEmpty()) {
            optionsBuilder.append(" | Topping: ").append(String.join(", ", toppings));
        }

        String optionsString = optionsBuilder.toString();

        // Kiểm tra xem món đó với đúng tập tùy chọn đó đã tồn tại trong giỏ chưa
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

    // Cập nhật số lượng (+1 hoặc -1)
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

    // Xóa một món ra khỏi giỏ
    @Transactional
    public void removeItem(Integer itemId) {
        cartItemRepository.deleteById(itemId);
    }
}