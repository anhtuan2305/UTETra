package vn.tuan.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import vn.tuan.entity.OrderDetail;

import java.util.List;

@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetail, Integer> {

    @Modifying
    @Transactional
    @Query("DELETE FROM OrderDetail od WHERE od.order.id IN :orderIds")
    void deleteByOrderIds(@Param("orderIds") List<Integer> orderIds);
}