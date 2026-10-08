package vn.tuan.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.tuan.entity.Shop;

import java.util.List;

@Repository
public interface ShopRepository extends JpaRepository<Shop, Integer> {
    // Lấy danh sách shop đang hoạt động
    List<Shop> findTop10ByStatusOrderByRatingDesc(String status);
}