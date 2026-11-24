package com.stylehub.repo;

import com.stylehub.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByActiveTrueOrderByDisplayOrderAsc();
    Optional<Category> findBySlugIgnoreCase(String slug);
    Optional<Category> findByNameIgnoreCase(String name);
    boolean existsByName(String name);
    boolean existsBySlug(String slug);
}
