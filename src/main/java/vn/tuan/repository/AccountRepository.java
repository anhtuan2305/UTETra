package vn.tuan.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.tuan.entity.Account;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Integer> {
    Optional<Account> findByEmail(String email);
    boolean existsByEmail(String email);
    
    // Tìm kiếm tài khoản có phân trang
    Page<Account> findByEmailContainingIgnoreCase(String email, Pageable pageable);
    
    // Đếm số lượng theo role
    long countByRole(String role);

    // Lọc danh sách tài khoản theo Role và trạng thái kích hoạt (dùng cho Admin duyệt Shipper)
    List<Account> findByRoleAndIsActiveOrderByCreatedAtDesc(String role, Boolean isActive);
}