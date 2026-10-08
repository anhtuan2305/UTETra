package vn.tuan.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.tuan.entity.Category;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {
}