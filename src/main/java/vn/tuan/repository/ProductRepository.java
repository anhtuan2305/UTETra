package vn.tuan.repository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.tuan.entity.Category;
import vn.tuan.entity.Product;
import vn.tuan.entity.Shop;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Product> {

    // Lấy sản phẩm đang bán có phân trang
    Page<Product> findByStatus(String status, Pageable pageable);

    // Tìm kiếm theo tên
    Page<Product> findByNameContainingIgnoreCaseAndStatus(String name, String status, Pageable pageable);

    // Gợi ý món cùng loại
    List<Product> findTop4ByCategoryAndIdNotAndStatus(Category category, Integer id, String status);

    // Dành cho Vendor quản lý
    List<Product> findByShopOrderByCreatedAtDesc(Shop shop);

    // DÀNH CHO GUEST: Sản phẩm bán trên 10 đơn, sắp xếp theo lượt bán giảm dần
    Page<Product> findByStatusAndSoldCountGreaterThanOrderBySoldCountDesc(String status, int minSold, Pageable pageable);

    // DÀNH CHO USER: Lấy theo thứ tự tạo mới nhất
    Page<Product> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select x from Product x where x.id = :id")
    java.util.Optional<Product> lockById(@org.springframework.data.repository.query.Param("id") Integer id);
}