package vn.tuan.repository;
import org.springframework.data.jpa.repository.JpaRepository;import vn.tuan.entity.*;import java.util.*;
public interface ProductActivityRepository extends JpaRepository<ProductActivity,Integer>{Optional<ProductActivity> findByAccountAndProduct(Account a,Product p);List<ProductActivity> findByAccountOrderByViewedAtDesc(Account a);}
