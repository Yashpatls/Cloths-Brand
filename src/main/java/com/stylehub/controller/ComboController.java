package com.stylehub.controller;

import com.stylehub.model.ComboPackage;
import com.stylehub.model.ComboTier;
import com.stylehub.service.CartService;
import com.stylehub.service.ComboImageService;
import com.stylehub.service.ComboService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Controller
@RequestMapping("/combos")
public class ComboController {

    private final ComboService comboService;
    private final CartService cartService;
    private final ComboImageService comboImageService;

    public ComboController(ComboService comboService, CartService cartService, ComboImageService comboImageService) {
        this.comboService = comboService;
        this.cartService = cartService;
        this.comboImageService = comboImageService;
    }

    @GetMapping
    public String listCombos(
            @RequestParam(required = false) String tier,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            Model model) {

        ComboTier selectedTier = null;
        if (tier != null && !tier.isBlank() && !tier.equalsIgnoreCase("ALL")) {
            try {
                selectedTier = ComboTier.valueOf(tier.toUpperCase());
            } catch (Exception ignored) {}
        }

        List<ComboPackage> combos = comboService.getAllActive(selectedTier, minPrice, maxPrice);

        model.addAttribute("combos", combos);
        model.addAttribute("selectedTier", selectedTier != null ? selectedTier.name() : "ALL");
        model.addAttribute("tiers", ComboTier.values());
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        return "combos";
    }

    @GetMapping("/{slugOrId}")
    public String comboDetail(@PathVariable String slugOrId, Model model) {
        final ComboPackage combo;
        if (slugOrId.matches("\\d+")) {
            combo = comboService.getById(Long.parseLong(slugOrId));
        } else {
            combo = comboService.getBySlug(slugOrId);
        }

        model.addAttribute("combo", combo);
        model.addAttribute("relatedCombos", comboService.getAllActive(combo.getTier(), null, null)
                .stream().filter(c -> !c.getId().equals(combo.getId())).limit(3).toList());
        return "combo-detail";
    }

    @PostMapping("/add-to-cart")
    public String addComboToCart(
            @RequestParam Long comboId,
            @RequestParam(defaultValue = "1") int quantity,
            @RequestParam(required = false) List<Long> includedItems,
            @RequestParam Map<String, String> allParams,
            Authentication auth,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        try {
            ComboPackage combo = comboService.getById(comboId);
            List<com.stylehub.model.ComboItem> allComboItems = combo.getItems() != null ? combo.getItems() : List.of();

            // Determine included item IDs (either from includedItems list or included_item_{id} checkboxes)
            final java.util.Set<Long> includedIds = new java.util.HashSet<>();
            if (includedItems != null && !includedItems.isEmpty()) {
                includedIds.addAll(includedItems);
            } else {
                for (var item : allComboItems) {
                    String paramVal = allParams.get("included_item_" + item.getId());
                    if ("true".equalsIgnoreCase(paramVal) || "1".equals(paramVal) || "on".equalsIgnoreCase(paramVal)) {
                        includedIds.add(item.getId());
                    }
                }
            }

            // If no explicit inclusion parameters were passed, default to all items
            if (includedIds.isEmpty() && (includedItems == null && allParams.keySet().stream().noneMatch(k -> k.startsWith("included_item_")))) {
                for (var item : allComboItems) {
                    includedIds.add(item.getId());
                }
            }

            if (includedIds.isEmpty()) {
                throw new IllegalArgumentException("At least one garment must be selected to purchase this combo.");
            }

            // Automatically recalculate price & construct breakdown
            BigDecimal customPrice = BigDecimal.ZERO;
            StringBuilder sbIncluded = new StringBuilder();
            StringBuilder sbExcluded = new StringBuilder();
            StringBuilder sbPids = new StringBuilder();

            for (var item : allComboItems) {
                if (includedIds.contains(item.getId())) {
                    customPrice = customPrice.add(item.getItemPrice());

                    String size = allParams.getOrDefault("size_" + item.getId(), item.getDefaultSize());
                    String color = allParams.getOrDefault("color_" + item.getId(), item.getDefaultColor());

                    if (!sbIncluded.isEmpty()) sbIncluded.append(" | ");
                    sbIncluded.append(item.getCustomTitle()).append(": ").append(size).append(" (").append(color).append(")");

                    if (item.getProduct() != null) {
                        if (!sbPids.isEmpty()) sbPids.append(",");
                        sbPids.append(item.getProduct().getId());
                    }
                } else {
                    if (!sbExcluded.isEmpty()) sbExcluded.append(", ");
                    sbExcluded.append(item.getCustomTitle());
                }
            }

            String selections = sbIncluded.toString();
            if (!sbExcluded.isEmpty()) {
                selections += " [Excluded: " + sbExcluded.toString() + "]";
            }
            if (selections.isBlank()) selections = "Standard Set";

            String email = auth != null ? auth.getName() : null;
            cartService.addComboItem(email, session.getId(), comboId, quantity, selections, customPrice, sbPids.toString());

            if ("true".equalsIgnoreCase(allParams.get("buyNow"))) {
                return "redirect:/checkout";
            }
            redirectAttributes.addFlashAttribute("successMessage", "Added '" + combo.getName() + "' (₹" + customPrice + ") to your shopping bag!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/cart";
    }

    /**
     * Serves the composed combo collage JPEG image for a package.
     * Optionally filters to only include the specified combo item IDs or product IDs.
     */
    @GetMapping(value = "/{id}/collage.jpg", produces = MediaType.IMAGE_JPEG_VALUE)
    @ResponseBody
    public ResponseEntity<byte[]> getComboCollage(
            @PathVariable Long id,
            @RequestParam(required = false) List<Long> itemIds,
            @RequestParam(required = false) List<Long> productIds) {

        try {
            byte[] imageBytes = comboImageService.generateCollageForCombo(id, itemIds, productIds);
            if (imageBytes != null && imageBytes.length > 0) {
                return ResponseEntity.ok()
                        .cacheControl(CacheControl.noCache())
                        .contentType(MediaType.IMAGE_JPEG)
                        .body(imageBytes);
            }
        } catch (Exception ignored) {}

        try {
            ComboPackage combo = comboService.getById(id);
            if (combo != null && combo.getImageUrl() != null && !combo.getImageUrl().isBlank()) {
                return ResponseEntity.status(302).location(java.net.URI.create(combo.getImageUrl())).build();
            }
        } catch (Exception ignored) {}

        return ResponseEntity.notFound().build();
    }

    /**
     * Generates a live composite preview image for any arbitrary list of product IDs.
     * Used by client-side dynamic item toggling and admin combo creation preview.
     */
    @GetMapping(value = "/live-collage.jpg", produces = MediaType.IMAGE_JPEG_VALUE)
    @ResponseBody
    public ResponseEntity<byte[]> getLiveCollage(
            @RequestParam(required = false) List<Long> productIds,
            @RequestParam(required = false) String title) {

        byte[] imageBytes = comboImageService.generateCollageForProducts(productIds, title);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .contentType(MediaType.IMAGE_JPEG)
                .body(imageBytes);
    }
}
