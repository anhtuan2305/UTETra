package vn.tuan.repository;
import org.springframework.data.jpa.repository.JpaRepository;import vn.tuan.entity.*;import java.util.*;
public interface ReviewRepository extends JpaRepository<Review,Integer>{List<Review> findByProductOrderByCreatedAtDesc(Product p);boolean existsByAccountAndProductAndOrder(Account a,Product p,Order o);}
