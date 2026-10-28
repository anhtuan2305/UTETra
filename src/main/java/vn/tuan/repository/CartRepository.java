package vn.tuan.repository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.tuan.entity.Account;
import vn.tuan.entity.Cart;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Integer> {
    Optional<Cart> findByAccount(Account account);
    Optional<Cart> findBySessionId(String sessionId);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select x from Cart x where x.id = :id")
    java.util.Optional<Cart> lockById(@org.springframework.data.repository.query.Param("id") Integer id);
}