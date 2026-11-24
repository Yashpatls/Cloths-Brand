package com.stylehub.repo;

import com.stylehub.model.ComboItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComboItemRepository extends JpaRepository<ComboItem, Long> {
    List<ComboItem> findByComboPackageIdOrderByDisplayOrderAsc(Long comboPackageId);
    void deleteByComboPackageId(Long comboPackageId);
}
