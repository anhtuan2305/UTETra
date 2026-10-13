package vn.tuan.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.tuan.entity.Account;
import vn.tuan.entity.Order;
import vn.tuan.entity.Shop;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {

    List<Order> findByAccountOrderByCreatedAtDesc(Account account);
    
    // Shipper
    List<Order> findByStatusInOrderByCreatedAtDesc(List<String> statuses);
    
    // Vendor: Lấy danh sách đơn theo shop
    List<Order> findByShopOrderByCreatedAtDesc(Shop shop);

    long countByStatus(String status);

    // Tính tổng doanh thu toàn sàn từ các đơn hàng hoàn tất (COMPLETED)
    @Query("SELECT COALESCE(SUM(o.finalAmount), 0) FROM Order o WHERE o.status = 'COMPLETED'")
    BigDecimal calculateTotalRevenue();

    // Tra cứu đơn cho khách vãng lai (khớp ID và số điện thoại ở guestPhone hoặc Address)
    @Query("SELECT o FROM Order o LEFT JOIN o.address a " +
           "WHERE o.id = :orderId AND (o.guestPhone = :phone OR a.phone = :phone)")
    Optional<Order> findByIdAndPhone(@Param("orderId") Integer orderId, @Param("phone") String phone);
}