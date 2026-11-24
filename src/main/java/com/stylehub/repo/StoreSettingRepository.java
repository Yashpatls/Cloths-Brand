package com.stylehub.repo;

import com.stylehub.model.StoreSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface StoreSettingRepository extends JpaRepository<StoreSetting, Long> {
    Optional<StoreSetting> findBySettingKey(String settingKey);
    boolean existsBySettingKey(String settingKey);
}
