package com.stylehub.service;

import com.stylehub.model.ComboItem;
import com.stylehub.model.ComboPackage;
import com.stylehub.model.Product;
import com.stylehub.repo.ComboPackageRepository;
import com.stylehub.repo.ProductRepository;
import com.stylehub.service.image.ComboImageGenerator;
import com.stylehub.service.image.ComboImageGenerator.GarmentVisualSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * High-level service that manages dynamic combo preview image generation,
 * inventory image composition, and caching.
 */
@Service
public class ComboImageService {

    private static final Logger log = LoggerFactory.getLogger(ComboImageService.class);

    private final ComboImageGenerator generator;
    private final ProductRepository productRepo;
    private final ComboPackageRepository comboRepo;

    // Fast in-memory cache keyed by sorted product IDs
    private final Map<String, byte[]> collageCache = new ConcurrentHashMap<>();

    public ComboImageService(ComboImageGenerator generator,
                             ProductRepository productRepo,
                             ComboPackageRepository comboRepo) {
        this.generator = generator;
        this.productRepo = productRepo;
        this.comboRepo = comboRepo;
    }

    /**
     * Generates a JPEG collage image from the given list of Product IDs.
     */
    @Transactional(readOnly = true)
    public byte[] generateCollageForProducts(List<Long> productIds, String comboTitle) {
        if (productIds == null || productIds.isEmpty()) {
            return generator.generateCollage(Collections.emptyList(), comboTitle);
        }

        String cacheKey = "pids:" + productIds.stream().sorted().map(String::valueOf).collect(Collectors.joining(","))
                + ":" + (comboTitle != null ? comboTitle : "");

        byte[] cached = collageCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        List<GarmentVisualSpec> specs = new ArrayList<>();
        for (Long pid : productIds) {
            Optional<Product> pOpt = productRepo.findById(pid);
            if (pOpt.isPresent()) {
                Product p = pOpt.get();
                specs.add(new GarmentVisualSpec(p.getName(), p.getImageUrl(), p.getCategoryName()));
            }
        }

        byte[] bytes = generator.generateCollage(specs, comboTitle);
        if (bytes.length > 0) {
            collageCache.put(cacheKey, bytes);
        }
        return bytes;
    }

    /**
     * Generates a JPEG collage image for a specific combo package, filtering to only
     * include constituent garments that the user has selected (by ComboItem ID or Product ID).
     */
    @Transactional(readOnly = true)
    public byte[] generateCollageForCombo(Long comboId, List<Long> includedItemIds, List<Long> includedProductIds) {
        Optional<ComboPackage> comboOpt = comboRepo.findById(comboId);
        if (comboOpt.isEmpty()) {
            return generator.generateCollage(Collections.emptyList(), "StyleHub");
        }

        ComboPackage combo = comboOpt.get();
        List<ComboItem> items = combo.getItems() != null ? combo.getItems() : Collections.emptyList();

        Set<Long> allowedItemIds = (includedItemIds != null && !includedItemIds.isEmpty())
                ? new HashSet<>(includedItemIds)
                : null;
        Set<Long> allowedPids = (includedProductIds != null && !includedProductIds.isEmpty())
                ? new HashSet<>(includedProductIds)
                : null;

        List<GarmentVisualSpec> specs = new ArrayList<>();
        for (ComboItem item : items) {
            // Filter by ComboItem ID if provided
            if (allowedItemIds != null && !allowedItemIds.contains(item.getId())) {
                continue;
            }
            // Filter by Product ID if provided
            if (allowedPids != null && (item.getProduct() == null || !allowedPids.contains(item.getProduct().getId()))) {
                continue;
            }

            if (item.getProduct() != null) {
                Product p = item.getProduct();
                specs.add(new GarmentVisualSpec(
                        item.getCustomTitle() != null ? item.getCustomTitle() : p.getName(),
                        p.getImageUrl(),
                        p.getCategoryName()
                ));
            }
        }

        return generator.generateCollage(specs, combo.getName());
    }

    @Transactional(readOnly = true)
    public byte[] generateCollageForCombo(Long comboId, List<Long> includedProductIds) {
        return generateCollageForCombo(comboId, null, includedProductIds);
    }

    /**
     * Clears cached collage images when combos or products are updated.
     */
    public void invalidateCache() {
        collageCache.clear();
    }
}
