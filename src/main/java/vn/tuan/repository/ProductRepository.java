package vn.tuan.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.tuan.entity.Category;
import vn.tuan.entity.Product;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {
    // Lấy danh sách sản phẩm theo trạng thái có phân trang
    Page<Product> findByStatus(String status, Pageable pageable);
    
    // Tìm kiếm sản phẩm theo tên
    Page<Product> findByNameContainingIgnoreCaseAndStatus(String name, String status, Pageable pageable);

    // Lấy top 4 sản phẩm cùng danh mục (gợi ý món tương tự)
    List<Product> findTop4ByCategoryAndIdNotAndStatus(Category category, Integer id, String status);
}