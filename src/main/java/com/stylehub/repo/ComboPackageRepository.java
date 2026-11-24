package com.stylehub.repo;

import com.stylehub.model.ComboPackage;
import com.stylehub.model.ComboTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ComboPackageRepository extends JpaRepository<ComboPackage, Long> {

    List<ComboPackage> findByActiveTrueOrderByTierAscComboPriceAsc();

    List<ComboPackage> findByActiveTrueAndTierOrderByComboPriceAsc(ComboTier tier);

    @Query("SELECT c FROM ComboPackage c WHERE c.active = true AND " +
           "(:tier IS NULL OR c.tier = :tier) AND " +
           "(:minPrice IS NULL OR c.comboPrice >= :minPrice) AND " +
           "(:maxPrice IS NULL OR c.comboPrice <= :maxPrice) " +
           "ORDER BY c.comboPrice ASC")
    List<ComboPackage> filterCombos(@Param("tier") ComboTier tier,
                                    @Param("minPrice") BigDecimal minPrice,
                                    @Param("maxPrice") BigDecimal maxPrice);

    Optional<ComboPackage> findBySlugIgnoreCase(String slug);

    List<ComboPackage> findAllByOrderByCreatedAtDesc();

    List<ComboPackage> findTop10ByOrderBySalesCountDesc();
}
