package com.stylehub.model;

public enum ComboTier {
    BUDGET("Budget Combos", "Affordable daily essentials and value sets", 0, 999),
    STANDARD("Standard Combos", "Popular casual and smart-casual collections", 1000, 2499),
    PREMIUM("Premium Combos", "High-fashion statements and complete formal looks", 2500, 5999),
    LUXURY("Luxury Combos", "Exclusive designer suits and luxury ethnic ensembles", 6000, 999999);

    private final String displayName;
    private final String description;
    private final int minPrice;
    private final int maxPrice;

    ComboTier(String displayName, String description, int minPrice, int maxPrice) {
        this.displayName = displayName;
        this.description = description;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public int getMinPrice() { return minPrice; }
    public int getMaxPrice() { return maxPrice; }
}
