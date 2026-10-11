package vn.tuan.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.tuan.entity.Account;
import vn.tuan.entity.Order;
import vn.tuan.entity.Shop;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {
    List<Order> findByAccountOrderByCreatedAtDesc(Account account);
    
    // Shipper
    List<Order> findByStatusInOrderByCreatedAtDesc(List<String> statuses);
    
    // Vendor: Lấy danh sách đơn theo shop
    List<Order> findByShopOrderByCreatedAtDesc(Shop shop);

    long countByStatus(String status);
}