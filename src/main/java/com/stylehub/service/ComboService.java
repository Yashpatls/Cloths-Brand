package com.stylehub.service;

import com.stylehub.exception.ResourceNotFoundException;
import com.stylehub.model.ComboItem;
import com.stylehub.model.ComboPackage;
import com.stylehub.model.ComboTier;
import com.stylehub.model.Product;
import com.stylehub.repo.ComboItemRepository;
import com.stylehub.repo.ComboPackageRepository;
import com.stylehub.repo.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ComboService {

    private final ComboPackageRepository comboRepo;
    private final ComboItemRepository comboItemRepo;
    private final ProductRepository productRepo;
    private final ComboImageService comboImageService;

    public ComboService(ComboPackageRepository comboRepo, ComboItemRepository comboItemRepo,
                        ProductRepository productRepo, ComboImageService comboImageService) {
        this.comboRepo = comboRepo;
        this.comboItemRepo = comboItemRepo;
        this.productRepo = productRepo;
        this.comboImageService = comboImageService;
    }

    public List<ComboPackage> getAllActive(ComboTier tier, BigDecimal minPrice, BigDecimal maxPrice) {
        if (tier == null && minPrice == null && maxPrice == null) {
            return comboRepo.findByActiveTrueOrderByTierAscComboPriceAsc();
        }
        return comboRepo.filterCombos(tier, minPrice, maxPrice);
    }

    public List<ComboPackage> getAllCombos() {
        return comboRepo.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public ComboPackage getById(Long id) {
        ComboPackage combo = comboRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Combo package not found with ID: " + id));
        if (combo.getItems() != null) {
            combo.getItems().size(); // Touch items
            for (ComboItem item : combo.getItems()) {
                if (item.getProduct() != null) {
                    if (item.getProduct().getSizes() != null) item.getProduct().getSizes().size();
                    if (item.getProduct().getColors() != null) item.getProduct().getColors().size();
                }
            }
        }
        return combo;
    }

    @Transactional(readOnly = true)
    public ComboPackage getBySlug(String slug) {
        ComboPackage combo = comboRepo.findBySlugIgnoreCase(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Combo package not found with slug: " + slug));
        if (combo.getItems() != null) {
            combo.getItems().size();
            for (ComboItem item : combo.getItems()) {
                if (item.getProduct() != null) {
                    if (item.getProduct().getSizes() != null) item.getProduct().getSizes().size();
                    if (item.getProduct().getColors() != null) item.getProduct().getColors().size();
                }
            }
        }
        return combo;
    }

    public List<ComboPackage> getTopSelling(int limit) {
        return comboRepo.findTop10ByOrderBySalesCountDesc().stream().limit(limit).toList();
    }

    @Transactional
    public ComboPackage save(ComboPackage combo, List<Long> productIds, List<Integer> quantities, List<String> customTitles) {
        if (combo.getSlug() == null || combo.getSlug().isBlank()) {
            String generatedSlug = combo.getName().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
            combo.setSlug(generatedSlug);
        }

        combo.setUpdatedAt(LocalDateTime.now());
        ComboPackage saved = comboRepo.save(combo);

        // Update items if productIds provided
        if (productIds != null && !productIds.isEmpty()) {
            comboItemRepo.deleteByComboPackageId(saved.getId());
            saved.getItems().clear();

            BigDecimal calculatedOriginal = BigDecimal.ZERO;
            for (int i = 0; i < productIds.size(); i++) {
                Long pId = productIds.get(i);
                Product product = productRepo.findById(pId).orElse(null);
                if (product != null) {
                    int qty = (quantities != null && quantities.size() > i && quantities.get(i) != null) ? quantities.get(i) : 1;
                    String title = (customTitles != null && customTitles.size() > i) ? customTitles.get(i) : product.getName();

                    BigDecimal itemOrigPrice = product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO;
                    calculatedOriginal = calculatedOriginal.add(itemOrigPrice.multiply(BigDecimal.valueOf(qty)));

                    BigDecimal itemBundlePrice = itemOrigPrice;
                    if (saved.getComboPrice() != null && calculatedOriginal.compareTo(BigDecimal.ZERO) > 0) {
                        itemBundlePrice = itemOrigPrice.multiply(saved.getComboPrice()).divide(calculatedOriginal, 2, java.math.RoundingMode.HALF_UP);
                    }

                    ComboItem item = new ComboItem(product, title, qty, itemBundlePrice, "M", "Default", i);
                    item.setComboPackage(saved);
                    comboItemRepo.save(item);
                    saved.getItems().add(item);
                }
            }

            if (saved.getOriginalPrice() == null || saved.getOriginalPrice().compareTo(BigDecimal.ZERO) == 0) {
                saved.setOriginalPrice(calculatedOriginal);
            }

            if (saved.getImageUrl() == null || saved.getImageUrl().isBlank()) {
                saved.setImageUrl("/combos/" + saved.getId() + "/collage.jpg");
            }

            saved = comboRepo.save(saved);
        } else if (saved.getImageUrl() == null || saved.getImageUrl().isBlank()) {
            saved.setImageUrl("/combos/" + saved.getId() + "/collage.jpg");
            saved = comboRepo.save(saved);
        }

        comboImageService.invalidateCache();
        return saved;
    }

    @Transactional
    public void toggleActive(Long id) {
        ComboPackage combo = getById(id);
        combo.setActive(!combo.isActive());
        comboRepo.save(combo);
    }

    @Transactional
    public void delete(Long id) {
        comboRepo.deleteById(id);
    }
}
