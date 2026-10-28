package vn.tuan.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import vn.tuan.dto.CheckoutDTO;
import vn.tuan.entity.Account;
import vn.tuan.entity.Carrier;
import vn.tuan.entity.Cart;
import vn.tuan.entity.CartItem;
import vn.tuan.entity.Order;
import vn.tuan.entity.OrderDetail;
import vn.tuan.entity.Product;
import vn.tuan.entity.Shop;
import vn.tuan.repository.AccountRepository;
import vn.tuan.repository.CarrierRepository;
import vn.tuan.repository.CartRepository;
import vn.tuan.repository.OrderRepository;
import vn.tuan.repository.ProductRepository;
import vn.tuan.repository.ShopRepository;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final AccountRepository accounts;
    private final CartRepository carts;
    private final ProductRepository products;
    private final ShopRepository shops;
    private final OrderRepository orders;
    private final CarrierRepository carriers;

    private final OrderEvents events;
    private final PaymentService payments;
    private final QuoteService quotes;

    private final EntityManager em;

    @Transactional
    public Order placeOrder(String identifier, CheckoutDTO dto) {

        // 1. Kiểm tra định danh và dữ liệu đầu vào.
        OrderRules.require(
                identifier != null && !identifier.isBlank(),
                "Không xác định được tài khoản hoặc phiên giỏ hàng"
        );

        OrderRules.require(
                dto != null,
                "Thông tin đặt hàng không hợp lệ"
        );

        // 2. Kiểm tra phương thức thanh toán.
        boolean isCod = "COD".equals(dto.getPaymentMethod());
        boolean isVnpay = "VNPAY".equals(dto.getPaymentMethod());

        OrderRules.require(
                isCod || (isVnpay && payments.available()),
                "Phương thức thanh toán chưa được cấu hình"
        );

        // 3. Xác định tài khoản hoặc khách vãng lai.
        Account account = null;

        if (!identifier.startsWith("GUEST_")) {
            account = accounts.findByEmail(identifier)
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Không tìm thấy tài khoản"
                            )
                    );

            OrderRules.require(
                    Boolean.TRUE.equals(account.getIsActive()),
                    "Tài khoản bị khóa hoặc chưa được kích hoạt"
            );
        }

        // 4. Tìm giỏ hàng tương ứng.
        Cart foundCart;

        if (account == null) {
            foundCart = carts.findBySessionId(identifier)
                    .orElseThrow(() ->
                            new IllegalArgumentException("Giỏ hàng trống")
                    );
        } else {
            foundCart = carts.findByAccount(account)
                    .orElseThrow(() ->
                            new IllegalArgumentException("Giỏ hàng trống")
                    );
        }

        // 5. Khóa giỏ và đọc lại dữ liệu mới nhất từ database.
        Cart cart = carts.lockById(foundCart.getId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Không tìm thấy giỏ hàng"
                        )
                );

        em.refresh(cart);

        OrderRules.require(
                !cart.getItems().isEmpty(),
                "Giỏ hàng đã được thanh toán hoặc đang trống"
        );

        // 6. Kiểm tra chi nhánh.
        OrderRules.require(
                dto.getShopId() != null,
                "Vui lòng chọn chi nhánh"
        );

        Shop shop = shops.findById(dto.getShopId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Không tìm thấy chi nhánh"
                        )
                );

        OrderRules.require(
                "ACTIVE".equals(shop.getStatus()),
                "Chi nhánh không hoạt động"
        );

        // 7. Kiểm tra đơn vị vận chuyển.
        Carrier carrier;

        if (dto.getCarrierId() == null) {
            carrier = carriers.findAll()
                    .stream()
                    .filter(item ->
                            Boolean.TRUE.equals(item.getIsActive())
                    )
                    .filter(item ->
                            item.getShop() == null
                                    || item.getShop().getId()
                                            .equals(shop.getId())
                    )
                    .findFirst()
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Chưa có đơn vị giao hàng phù hợp"
                            )
                    );
        } else {
            carrier = carriers.findById(dto.getCarrierId())
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Không tìm thấy đơn vị giao hàng"
                            )
                    );
        }

        OrderRules.require(
                Boolean.TRUE.equals(carrier.getIsActive()),
                "Đơn vị giao hàng đã ngừng hoạt động"
        );

        OrderRules.require(
                carrier.getShop() == null
                        || carrier.getShop().getId().equals(shop.getId()),
                "Đơn vị giao hàng không áp dụng cho chi nhánh này"
        );

        // 8. Khởi tạo đơn hàng.
        Order order = Order.builder()
                .account(account)
                .shop(shop)
                .carrier(carrier)
                .guestName(dto.getReceiverName().trim())
                .guestPhone(dto.getPhone())
                .guestAddress(
                        dto.getStreetAddress() + ", " + dto.getCity()
                )
                .note(dto.getNote())
                .status("NEW")
                .paymentMethod(dto.getPaymentMethod())
                .paymentReference(UUID.randomUUID().toString())
                .paymentExpiresAt(
                        LocalDateTime.now().plusMinutes(20)
                )
                .paymentStatus("UNPAID")
                .shippingFee(carrier.getBaseFee())
                .discountAmount(BigDecimal.ZERO)
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;

        // 9. Khóa sản phẩm theo thứ tự ID thống nhất.
        // Mỗi sản phẩm chỉ khóa và refresh một lần,
        // kể cả khi giỏ có nhiều dòng tùy chọn của cùng sản phẩm.
        Map<Integer, Product> lockedProducts = new HashMap<>();

        var productIds = cart.getItems()
                .stream()
                .map(item -> item.getProduct().getId())
                .distinct()
                .sorted()
                .toList();

        for (Integer productId : productIds) {
            Product product = products.lockById(productId)
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Sản phẩm không còn tồn tại"
                            )
                    );

            em.refresh(product);
            lockedProducts.put(productId, product);
        }

        // 10. Kiểm tra từng dòng giỏ, trừ tồn và lưu chi tiết đơn.
        var sortedItems = cart.getItems()
                .stream()
                .sorted(
                        Comparator.comparing(
                                item -> item.getProduct().getId()
                        )
                )
                .toList();

        for (CartItem item : sortedItems) {
            Product product = lockedProducts.get(
                    item.getProduct().getId()
            );

            OrderRules.require(
                    product.getShop().getId().equals(shop.getId()),
                    "Món không thuộc chi nhánh đã chọn"
            );

            OrderRules.require(
                    "ACTIVE".equals(product.getStatus()),
                    "Món đã ngừng bán: " + product.getName()
            );

            OrderRules.quantity(
                    item.getQuantity(),
                    product.getStock()
            );

            product.setStock(
                    product.getStock() - item.getQuantity()
            );

            product.setSoldCount(
                    product.getSoldCount() + item.getQuantity()
            );

            // Giá thực tế gồm giá gốc, size và topping.
            BigDecimal unitPrice = OptionPricing.price(
                    product.getBasePrice(),
                    item.getOptionsJson()
            );

            BigDecimal lineTotal = unitPrice.multiply(
                    BigDecimal.valueOf(item.getQuantity())
            );

            totalAmount = totalAmount.add(lineTotal);

            OrderDetail detail = OrderDetail.builder()
                    .order(order)
                    .product(product)
                    .productName(product.getName())
                    .unitPrice(unitPrice)
                    .quantity(item.getQuantity())
                    .optionsSnapshot(item.getOptionsJson())
                    .build();

            order.getOrderDetails().add(detail);
        }

        // 11. Tính phí giao hàng và mã giảm giá trên server.
        order.setTotalAmount(totalAmount);

        QuoteService.Quote quote = quotes.calculate(
                totalAmount,
                shop.getId(),
                carrier.getId(),
                dto.getPromotionCode()
        );

        order.setCarrier(quote.carrier());
        order.setPromotion(quote.promotion());
        order.setShippingFee(quote.shipping());
        order.setDiscountAmount(quote.discount());
        order.setFinalAmount(quote.total());

        // 12. Snapshot chiết khấu ứng dụng tại thời điểm đặt.
        BigDecimal commissionPercent =
                shop.getCommissionPercent() == null
                        ? BigDecimal.ZERO
                        : shop.getCommissionPercent();

        BigDecimal commissionAmount = totalAmount
                .multiply(commissionPercent)
                .divide(
                        BigDecimal.valueOf(100),
                        0,
                        RoundingMode.HALF_UP
                );

        order.setCommissionAmount(commissionAmount);

        // 13. Lưu đơn hàng và chi tiết đơn.
        orders.saveAndFlush(order);

        // 14. Xóa các món trong giỏ.
        // Cart.items có orphanRemoval nên các dòng CartItem
        // được xóa khỏi database khi flush.
        cart.getItems().clear();

        // Đồng bộ việc xóa trước khi checkout khác refresh giỏ.
        // Flush không phải commit: nếu giao dịch thất bại,
        // toàn bộ thay đổi vẫn được rollback.
        em.flush();

        // 15. Đăng ký thông báo sau khi giao dịch commit.
        events.changed(order);

        return order;
    }
}