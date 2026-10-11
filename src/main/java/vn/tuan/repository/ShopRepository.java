package vn.tuan.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.tuan.entity.Account;
import vn.tuan.entity.Shop;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShopRepository extends JpaRepository<Shop, Integer> {
    
    // Tìm shop theo tài khoản chủ quán (Dành cho Vendor Dashboard)
    Optional<Shop> findByOwner(Account owner);

    List<Shop> findTop10ByStatusOrderByRatingDesc(String status);
    
    Page<Shop> findByShopNameContainingIgnoreCase(String shopName, Pageable pageable);
    
    long countByStatus(String status);
}