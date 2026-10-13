package vn.tuan.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.tuan.dto.CheckoutDTO;
import vn.tuan.entity.*;
import vn.tuan.repository.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderDetailRepository orderDetailRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ShopRepository shopRepository;

    @Transactional
    public Order placeOrder(String identifier, CheckoutDTO dto) {
        Account account = null;
        Cart cart = null;

        // 1. Xác định tài khoản và giỏ hàng (Đăng nhập vs Khách vãng lai)
        if (identifier != null && !identifier.trim().isEmpty() && !identifier.startsWith("GUEST_")) {
            account = accountRepository.findByEmail(identifier).orElse(null);
            if (account != null) {
                cart = cartRepository.findByAccount(account).orElse(null);
            }
        } else if (identifier != null && identifier.startsWith("GUEST_")) {
            cart = cartRepository.findBySessionId(identifier).orElse(null);
        }

        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new RuntimeException("Giỏ hàng đang trống, không thể đặt hàng!");
        }

        // 2. Lưu địa chỉ giao hàng (Account nhận null đối với khách vãng lai)
        Address address = new Address();
        address.setAccount(account);
        address.setReceiverName(dto.getReceiverName());
        address.setPhone(dto.getPhone());
        address.setStreetAddress(dto.getStreetAddress());
        address.setCity(dto.getCity());
        address.setIsDefault(account != null);

        address = addressRepository.save(address);

        // 3. Xác định chi nhánh quán phục vụ từ lựa chọn của khách hàng
        Shop orderShop = null;
        if (dto.getShopId() != null) {
            orderShop = shopRepository.findById(dto.getShopId()).orElse(null);
        }
        
        // Dự phòng: nếu khách không chọn shopId thì lấy shop từ sản phẩm đầu tiên
        if (orderShop == null && !cart.getItems().isEmpty() && cart.getItems().get(0).getProduct() != null) {
            orderShop = cart.getItems().get(0).getProduct().getShop();
        }

        if (orderShop == null) {
            throw new RuntimeException("Không tìm thấy chi nhánh quán phục vụ đơn hàng!");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal shippingFee = new BigDecimal("15000");

        List<OrderDetail> detailsToSave = new ArrayList<>();

        for (CartItem item : cart.getItems()) {
            BigDecimal itemTotal = item.getProduct().getBasePrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);

            // Cập nhật tồn kho và lượt bán
            Product prod = item.getProduct();
            if (prod.getStock() != null) {
                prod.setStock(prod.getStock() - item.getQuantity());
            }
            if (prod.getSoldCount() != null) {
                prod.setSoldCount(prod.getSoldCount() + item.getQuantity());
            } else {
                prod.setSoldCount(item.getQuantity());
            }
            productRepository.save(prod);
        }

        BigDecimal finalAmount = totalAmount.add(shippingFee);

        // 4. Khởi tạo đối tượng Order gắn với đúng chi nhánh Shop đã chọn
        Order order = Order.builder()
                .account(account)
                .shop(orderShop)
                .address(address)
                .guestName(dto.getReceiverName())
                .guestPhone(dto.getPhone())
                .guestAddress(dto.getStreetAddress() + ", " + dto.getCity())
                .note(dto.getNote())
                .status("NEW")
                .paymentMethod(dto.getPaymentMethod())
                .paymentStatus("COD".equalsIgnoreCase(dto.getPaymentMethod()) ? "UNPAID" : "PAID")
                .totalAmount(totalAmount)
                .shippingFee(shippingFee)
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(finalAmount)
                .build();

        order = orderRepository.save(order);

        // 5. Lưu từng dòng OrderDetail
        for (CartItem item : cart.getItems()) {
            OrderDetail detail = OrderDetail.builder()
                    .order(order)
                    .product(item.getProduct())
                    .productName(item.getProduct().getName())
                    .unitPrice(item.getProduct().getBasePrice())
                    .quantity(item.getQuantity())
                    .optionsSnapshot(item.getOptionsJson())
                    .build();
            detailsToSave.add(detail);
        }
        orderDetailRepository.saveAll(detailsToSave);

        // 6. Dọn sạch giỏ hàng sau khi hoàn tất đặt đơn
        cartItemRepository.deleteAll(cart.getItems());
        cart.getItems().clear();
        cartRepository.save(cart);

        return order;
    }
}