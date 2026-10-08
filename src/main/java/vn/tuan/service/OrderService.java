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

    @Transactional
    public Order placeOrder(String email, CheckoutDTO dto) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản: " + email));

        Cart cart = cartRepository.findByAccount(account)
                .orElseThrow(() -> new RuntimeException("Giỏ hàng không tồn tại!"));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new RuntimeException("Giỏ hàng đang trống, không thể đặt hàng!");
        }

        // 1. Lưu địa chỉ giao hàng
        Address address = Address.builder()
                .account(account)
                .receiverName(dto.getReceiverName())
                .phone(dto.getPhone())
                .streetAddress(dto.getStreetAddress())
                .city(dto.getCity())
                .isDefault(true)
                .build();
        address = addressRepository.save(address);

        // 2. Gom nhóm các món theo từng Shop (Mỗi shop sẽ tạo 1 Đơn hàng tương ứng)
        // Với đơn hàng đơn giản từ 1 shop trước mắt, lấy Shop của món đầu tiên:
        Shop orderShop = cart.getItems().get(0).getProduct().getShop();

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal shippingFee = new BigDecimal("15000"); // Đồng giá ship 15.000 đ

        List<OrderDetail> detailsToSave = new ArrayList<>();

        for (CartItem item : cart.getItems()) {
            BigDecimal itemTotal = item.getProduct().getBasePrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);

            // Cập nhật tồn kho và lượt bán
            Product prod = item.getProduct();
            prod.setStock(prod.getStock() - item.getQuantity());
            prod.setSoldCount(prod.getSoldCount() + item.getQuantity());
            productRepository.save(prod);
        }

        BigDecimal finalAmount = totalAmount.add(shippingFee);

        // 3. Khởi tạo đối tượng Order
        Order order = Order.builder()
                .account(account)
                .shop(orderShop)
                .address(address)
                .status("NEW")
                .paymentMethod(dto.getPaymentMethod())
                .paymentStatus("COD".equalsIgnoreCase(dto.getPaymentMethod()) ? "UNPAID" : "PAID")
                .totalAmount(totalAmount)
                .shippingFee(shippingFee)
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(finalAmount)
                .build();

        order = orderRepository.save(order);

        // 4. Lưu từng dòng OrderDetail (Áp dụng Snapshot đúng theo thiết kế)
        for (CartItem item : cart.getItems()) {
            OrderDetail detail = OrderDetail.builder()
                    .order(order)
                    .product(item.getProduct())
                    .productName(item.getProduct().getName()) // SNAPSHOT tên
                    .unitPrice(item.getProduct().getBasePrice()) // SNAPSHOT giá
                    .quantity(item.getQuantity())
                    .optionsSnapshot(item.getOptionsJson()) // SNAPSHOT cấu hình trà sữa
                    .build();
            detailsToSave.add(detail);
        }
        orderDetailRepository.saveAll(detailsToSave);

        // 5. Dọn sạch giỏ hàng sau khi đặt thành công
        cart.getItems().clear();
        cartRepository.save(cart);

        return order;
    }
}