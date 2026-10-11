package vn.tuan.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.tuan.entity.Carrier;

import java.util.List;

@Repository
public interface CarrierRepository extends JpaRepository<Carrier, Integer> {
    List<Carrier> findByIsActiveTrue();
}