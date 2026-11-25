package com.stylehub.config;

import com.stylehub.model.*;
import com.stylehub.repo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Bean
    CommandLineRunner seedDatabase(
            UserRepository userRepo,
            AddressRepository addressRepo,
            CategoryRepository categoryRepo,
            ProductRepository productRepo,
            ProductImageRepository imageRepo,
            ProductSizeRepository sizeRepo,
            ProductColorRepository colorRepo,
            CouponRepository couponRepo,
            StoreSettingRepository settingRepo,
            ComboPackageRepository comboRepo,
            ComboItemRepository comboItemRepo,
            PasswordEncoder encoder) {

        return args -> {
            log.info("Checking database seed state for StyleHub...");

            // 1. Seed Store Settings
            if (settingRepo.count() == 0) {
                settingRepo.save(new StoreSetting("store_name", "StyleHub", "Official store brand name"));
                settingRepo.save(new StoreSetting("currency_symbol", "₹", "Default currency symbol"));
                settingRepo.save(new StoreSetting("support_email", "support@stylehub.local", "Customer care email"));
                settingRepo.save(new StoreSetting("support_phone", "+91 (022) 555-0199", "Customer care hotline"));
                settingRepo.save(new StoreSetting("free_shipping_threshold", "1500.00", "Order total for free delivery"));
                settingRepo.save(new StoreSetting("flat_shipping_rate", "99.00", "Standard delivery charge"));
                settingRepo.save(new StoreSetting("tax_percentage", "5.0", "Applicable GST/VAT percentage"));
            }

            // 2. Seed Users (Admin & Customer)
            User admin = userRepo.findByEmail("admin@stylehub.local").orElseGet(() -> {
                User u = new User();
                u.setName("StyleHub Administrator");
                u.setEmail("admin@stylehub.local");
                u.setPassword(encoder.encode("Admin@12345"));
                u.setRole(Role.ADMIN);
                u.setPhone("+91 98765 43210");
                u.setAddress("StyleHub HQ, BKC, Bandra East, Mumbai, Maharashtra 400051");
                return userRepo.save(u);
            });

            User demoCustomer = userRepo.findByEmail("customer@stylehub.local").orElseGet(() -> {
                User u = new User();
                u.setName("Aarav Sharma");
                u.setEmail("customer@stylehub.local");
                u.setPassword(encoder.encode("Customer@12345"));
                u.setRole(Role.CUSTOMER);
                u.setPhone("+91 91234 56789");
                u.setAddress("Flat 402, Lotus Residency, Powai, Mumbai, Maharashtra 400076");
                User saved = userRepo.save(u);

                Address addr = new Address();
                addr.setUser(saved);
                addr.setFullName("Aarav Sharma");
                addr.setPhone("+91 91234 56789");
                addr.setStreetAddress("Flat 402, Lotus Residency, Central Avenue");
                addr.setApartment("Building 4B");
                addr.setCity("Mumbai");
                addr.setState("Maharashtra");
                addr.setPostalCode("400076");
                addr.setDefaultShipping(true);
                addressRepo.save(addr);

                return saved;
            });

            // 3. Seed Core Categories
            Category menCat = categoryRepo.findBySlugIgnoreCase("men").orElseGet(() ->
                    categoryRepo.save(new Category("Men", "men", "Tailored menswear, casual essentials, outerwear and refined denim.", "https://images.unsplash.com/photo-1617137984095-74e4e5e3613f?w=900", 1)));

            Category womenCat = categoryRepo.findBySlugIgnoreCase("women").orElseGet(() ->
                    categoryRepo.save(new Category("Women", "women", "Effortless silhouettes, relaxed linen, dresses, and modern outerwear.", "https://images.unsplash.com/photo-1483985988355-763728e1935b?w=900", 2)));

            Category kidsCat = categoryRepo.findBySlugIgnoreCase("kids").orElseGet(() ->
                    categoryRepo.save(new Category("Kids", "kids", "Comfort-first organic cotton basics, seasonal sets, and playwear.", "https://images.unsplash.com/photo-1519238263530-99bdd11df2ea?w=900", 3)));

            Category accCat = categoryRepo.findBySlugIgnoreCase("accessories").orElseGet(() ->
                    categoryRepo.save(new Category("Accessories", "accessories", "Hand-finished leather goods, carry-all totes, and accents.", "https://images.unsplash.com/photo-1590874103328-eac38a683ce7?w=900", 4)));

            Category footCat = categoryRepo.findBySlugIgnoreCase("footwear").orElseGet(() ->
                    categoryRepo.save(new Category("Footwear", "footwear", "Architectural minimal sneakers, Chelsea boots, and everyday loafers.", "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=900", 5)));

            // 4. Seed Rich Product Catalog & Reference Products
            if (productRepo.count() == 0) {
                createProduct(productRepo, imageRepo, sizeRepo, colorRepo,
                        "Tailored Oxford Cotton Shirt", "SH-M-001",
                        "Crafted from premium 100% Egyptian combed cotton with a structured button-down collar.",
                        1499, 1299, menCat,
                        "https://images.unsplash.com/photo-1596755094514-f87e34085b2c?w=900",
                        List.of("https://images.unsplash.com/photo-1602810318383-e386cc2a3ccf?w=900"),
                        List.of("S", "M", "L", "XL", "XXL"),
                        List.of("Crisp White", "Powder Blue", "Navy"),
                        45, true);

                createProduct(productRepo, imageRepo, sizeRepo, colorRepo,
                        "Vintage Washed Denim Jacket", "SH-M-002",
                        "Heavyweight 13.5oz Japanese selvedge denim jacket finished with subtle distressed abrasions.",
                        2899, 2499, menCat,
                        "https://images.unsplash.com/photo-1576995853123-5a10305d93c0?w=900",
                        List.of(
                                "https://images.unsplash.com/photo-1551028719-00167b16eac5?w=900",
                                "https://images.unsplash.com/photo-1544441893-675973e31985?w=900"
                        ),
                        List.of("M", "L", "XL"),
                        List.of("Indigo Wash", "Charcoal Black"),
                        28, true);

                createProduct(productRepo, imageRepo, sizeRepo, colorRepo,
                        "Pleated Linen Chino Trousers", "SH-M-004",
                        "Tailored wide-leg trousers cut in breathable French linen-cotton blend.",
                        1999, 1699, menCat,
                        "https://images.unsplash.com/photo-1479064555552-3ef4979f8908?w=900",
                        List.of(
                                "https://images.unsplash.com/photo-1624378439575-d8705ad7ae80?w=900",
                                "https://images.unsplash.com/photo-1594938298603-c8148c4dae35?w=900",
                                "https://images.unsplash.com/photo-1506630448388-4e683c67ddb0?w=900"
                        ),
                        List.of("30", "32", "34", "36"),
                        List.of("Khaki Oat", "French Navy", "Olive Drab"),
                        32, true);

                createProduct(productRepo, imageRepo, sizeRepo, colorRepo,
                        "Silk Satin Midi Slip Dress", "SH-W-005",
                        "Bias-cut Mulberry silk slip dress with delicate adjustable rouleau straps.",
                        3499, 2999, womenCat,
                        "https://images.unsplash.com/photo-1595777457583-95e059d581b8?w=900",
                        List.of(),
                        List.of("XS", "S", "M", "L"),
                        List.of("Champagne", "Emerald Green", "Noir Black"),
                        24, true);

                createProduct(productRepo, imageRepo, sizeRepo, colorRepo,
                        "Pima Cotton Essential T-Shirt", "SH-M-007",
                        "Everyday staple jersey t-shirt crafted from long-staple Peruvian pima cotton.",
                        599, 499, menCat,
                        "https://images.unsplash.com/photo-1581655353564-df123a1eb820?w=900",
                        List.of(),
                        List.of("S", "M", "L", "XL"),
                        List.of("Optic White", "Heather Grey", "Black"),
                        60, false);
            }

            // Reference Product: Round Toe Pivot Pro Sneakers (U.S. Polo Assn. inspired high-tech footwear)
            Product pivotSneakers = productRepo.findBySku("SH-FT-031").orElseGet(() -> {
                Product p = new Product();
                p.setName("Round Toe Pivot Pro Sneakers");
                p.setSku("SH-FT-031");
                p.setDescription("Engineered for all-day athletic comfort and urban sophistication. The Round Toe Pivot Pro Sneakers combine breathable high-density engineered mesh with premium microfiber leather overlays, an ergonomic shock-absorbing pivot-traction outsole, and dual-density memory foam footbed for maximum durability and effortless styling.");
                p.setPrice(BigDecimal.valueOf(5999));
                p.setDiscountPrice(BigDecimal.valueOf(3999));
                p.setCategory(footCat);
                p.setCategoryName(footCat.getName());
                p.setImageUrl("https://images.unsplash.com/photo-1549298916-b41d501d3772?w=900");
                p.setStock(35);
                p.setFeatured(true);
                p.setActive(true);
                Product saved = productRepo.save(p);

                imageRepo.save(new ProductImage(saved, "https://images.unsplash.com/photo-1549298916-b41d501d3772?w=900", "Round Toe Pivot Pro Sneakers - Lateral Profile", true, 0));
                imageRepo.save(new ProductImage(saved, "https://images.unsplash.com/photo-1525966222134-fcfa99b8ae77?w=900", "Round Toe Pivot Pro Sneakers - Top Angle View", false, 1));
                imageRepo.save(new ProductImage(saved, "https://images.unsplash.com/photo-1595950653106-6c9ebd614d3a?w=900", "Round Toe Pivot Pro Sneakers - Ergonomic Sole & Tech Mesh", false, 2));
                imageRepo.save(new ProductImage(saved, "https://images.unsplash.com/photo-1600185365926-3a2ce3cdb9eb?w=900", "Round Toe Pivot Pro Sneakers - Heel Counter & Cushioning", false, 3));

                for (String sz : List.of("UK 6", "UK 7", "UK 8", "UK 9", "UK 10", "UK 11")) {
                    sizeRepo.save(new ProductSize(saved, sz, 30));
                }

                colorRepo.save(new ProductColor(saved, "Optic White", "#ffffff"));
                colorRepo.save(new ProductColor(saved, "Triple Black", "#111111"));
                colorRepo.save(new ProductColor(saved, "Navy Tech", "#1a237e"));
                return saved;
            });

            // Ensure helper products exist for bundles
            Product shirt = productRepo.findBySku("SH-M-001").orElseGet(() ->
                    productRepo.findAll().stream().filter(p -> "men".equalsIgnoreCase(p.getCategoryName())).findFirst().orElse(null));
            Product jacket = productRepo.findBySku("SH-M-002").orElseGet(() -> shirt);
            Product trousers = productRepo.findBySku("SH-M-004").orElseGet(() -> shirt);
            Product dress = productRepo.findBySku("SH-W-005").orElseGet(() ->
                    productRepo.findBySku("SH-W-001").orElseGet(() ->
                            productRepo.findAll().stream().filter(p -> "women".equalsIgnoreCase(p.getCategoryName())).findFirst().orElse(null)));
            Product tshirt = productRepo.findBySku("SH-M-007").orElseGet(() -> {
                Product p = new Product();
                p.setName("Classic Crew Neck Cotton T-Shirt");
                p.setSku("SH-M-007");
                p.setDescription("Premium 100% combed cotton jersey t-shirt with ribbed crew neck. Essential everyday basic.");
                p.setPrice(BigDecimal.valueOf(499));
                p.setCategory(menCat);
                p.setCategoryName(menCat.getName());
                p.setImageUrl("https://images.unsplash.com/photo-1581655353564-df123a1eb820?w=900");
                p.setStock(50);
                p.setActive(true);
                Product saved = productRepo.save(p);
                sizeRepo.save(new ProductSize(saved, "M", 30));
                colorRepo.save(new ProductColor(saved, "White", "#ffffff"));
                return saved;
            });

            Product jeans = productRepo.findBySku("SH-M-015").orElseGet(() -> {
                Product p = new Product();
                p.setName("Straight-Leg Denim Jeans");
                p.setSku("SH-M-015");
                p.setDescription("Premium durable cotton denim with authentic wash.");
                p.setPrice(BigDecimal.valueOf(1799));
                p.setCategory(menCat);
                p.setCategoryName(menCat.getName());
                p.setImageUrl("https://images.unsplash.com/photo-1542272604-787c3835535d?w=900");
                p.setStock(40);
                p.setActive(true);
                Product saved = productRepo.save(p);
                sizeRepo.save(new ProductSize(saved, "32", 40));
                colorRepo.save(new ProductColor(saved, "Dark Indigo", "#1a237e"));
                return saved;
            });

            Product belt = productRepo.findBySku("SH-ACC-016").orElseGet(() -> {
                Product p = new Product();
                p.setName("Handcrafted Leather Belt");
                p.setSku("SH-ACC-016");
                p.setDescription("Vegetable tanned Italian full-grain leather belt with solid brass buckle.");
                p.setPrice(BigDecimal.valueOf(899));
                p.setCategory(accCat);
                p.setCategoryName(accCat.getName());
                p.setImageUrl("https://images.unsplash.com/photo-1624222247344-550fb60583dc?w=900");
                p.setStock(50);
                p.setActive(true);
                Product saved = productRepo.save(p);
                sizeRepo.save(new ProductSize(saved, "Free Size", 50));
                colorRepo.save(new ProductColor(saved, "Cognac Brown", "#5d4037"));
                return saved;
            });

            Product sneakers = productRepo.findBySku("SH-FT-017").orElseGet(() -> {
                Product p = new Product();
                p.setName("Minimal Low-Top Leather Sneakers");
                p.setSku("SH-FT-017");
                p.setDescription("Italian calfskin leather tennis sneakers on a stitched Margom rubber sole.");
                p.setPrice(BigDecimal.valueOf(2299));
                p.setCategory(footCat);
                p.setCategoryName(footCat.getName());
                p.setImageUrl("https://images.unsplash.com/photo-1549298916-b41d501d3772?w=900");
                p.setStock(30);
                p.setActive(true);
                Product saved = productRepo.save(p);
                sizeRepo.save(new ProductSize(saved, "UK 8", 30));
                colorRepo.save(new ProductColor(saved, "All White", "#ffffff"));
                return saved;
            });

            Product kurti = productRepo.findBySku("SH-W-018").orElseGet(() -> {
                Product p = new Product();
                p.setName("Hand-Block Printed Cotton Kurti");
                p.setSku("SH-W-018");
                p.setDescription("Artisanal Jaipuri hand-block print kurti in breathable pure cotton.");
                p.setPrice(BigDecimal.valueOf(1199));
                p.setCategory(womenCat);
                p.setCategoryName(womenCat.getName());
                p.setImageUrl("https://images.unsplash.com/photo-1610030469983-98e550d6193c?w=900");
                p.setStock(35);
                p.setActive(true);
                Product saved = productRepo.save(p);
                sizeRepo.save(new ProductSize(saved, "M", 35));
                colorRepo.save(new ProductColor(saved, "Crimson Red", "#b71c1c"));
                return saved;
            });

            Product leggings = productRepo.findBySku("SH-W-019").orElseGet(() -> {
                Product p = new Product();
                p.setName("Stretch Comfort Cotton Leggings");
                p.setSku("SH-W-019");
                p.setDescription("High-waisted stretch cotton churidar leggings with 4-way flexibility.");
                p.setPrice(BigDecimal.valueOf(499));
                p.setCategory(womenCat);
                p.setCategoryName(womenCat.getName());
                p.setImageUrl("https://images.unsplash.com/photo-1509916796186-85dc916cb043?w=900");
                p.setStock(50);
                p.setActive(true);
                Product saved = productRepo.save(p);
                sizeRepo.save(new ProductSize(saved, "Free Size", 50));
                colorRepo.save(new ProductColor(saved, "Jet Black", "#000000"));
                return saved;
            });

            Product dupatta = productRepo.findBySku("SH-W-020").orElseGet(() -> {
                Product p = new Product();
                p.setName("Pure Chiffon Gold Border Dupatta");
                p.setSku("SH-W-020");
                p.setDescription("Lightweight sheer chiffon stole finished with delicate zari edging.");
                p.setPrice(BigDecimal.valueOf(599));
                p.setCategory(womenCat);
                p.setCategoryName(womenCat.getName());
                p.setImageUrl("https://images.unsplash.com/photo-1583391733956-3750e0ff4e8b?w=900");
                p.setStock(40);
                p.setActive(true);
                Product saved = productRepo.save(p);
                sizeRepo.save(new ProductSize(saved, "Free Size", 40));
                colorRepo.save(new ProductColor(saved, "Gold Beige", "#d7ccc8"));
                return saved;
            });

            Product handbag = productRepo.findBySku("SH-ACC-021").orElseGet(() -> {
                Product p = new Product();
                p.setName("Italian Leather Crossbody Bag");
                p.setSku("SH-ACC-021");
                p.setDescription("Structured saddle bag in smooth vegetable-tanned grain leather.");
                p.setPrice(BigDecimal.valueOf(1999));
                p.setCategory(accCat);
                p.setCategoryName(accCat.getName());
                p.setImageUrl("https://images.unsplash.com/photo-1584917865442-de89df76afd3?w=900");
                p.setStock(25);
                p.setActive(true);
                Product saved = productRepo.save(p);
                sizeRepo.save(new ProductSize(saved, "One Size", 25));
                colorRepo.save(new ProductColor(saved, "Cognac Brown", "#795548"));
                return saved;
            });

            Product blazer = productRepo.findBySku("SH-M-022").orElseGet(() -> {
                Product p = new Product();
                p.setName("Tailored Wool-Blend Evening Blazer");
                p.setSku("SH-M-022");
                p.setDescription("Single-breasted 2-button blazer with notch lapels and pick-stitch detailing.");
                p.setPrice(BigDecimal.valueOf(3999));
                p.setCategory(menCat);
                p.setCategoryName(menCat.getName());
                p.setImageUrl("https://images.unsplash.com/photo-1507679799987-c73779587ccf?w=900");
                p.setStock(20);
                p.setActive(true);
                Product saved = productRepo.save(p);
                sizeRepo.save(new ProductSize(saved, "40R", 20));
                colorRepo.save(new ProductColor(saved, "Charcoal Grey", "#37474f"));
                return saved;
            });

            Product kidsShorts = productRepo.findBySku("SH-K-023").orElseGet(() -> {
                Product p = new Product();
                p.setName("Kids Active Drawstring Shorts");
                p.setSku("SH-K-023");
                p.setDescription("Soft organic cotton french terry play shorts with elasticated waist.");
                p.setPrice(BigDecimal.valueOf(499));
                p.setCategory(kidsCat);
                p.setCategoryName(kidsCat.getName());
                p.setImageUrl("https://images.unsplash.com/photo-1519238263530-99bdd11df2ea?w=900");
                p.setStock(30);
                p.setActive(true);
                Product saved = productRepo.save(p);
                sizeRepo.save(new ProductSize(saved, "4-5Y", 30));
                colorRepo.save(new ProductColor(saved, "Heather Grey", "#9e9e9e"));
                return saved;
            });

            Product kidsDungarees = productRepo.findBySku("SH-K-024").orElseGet(() -> {
                Product p = new Product();
                p.setName("Kids Denim Dungarees & Striped Tee");
                p.setSku("SH-K-024");
                p.setDescription("Durable washed denim overalls paired with a breathable striped cotton t-shirt.");
                p.setPrice(BigDecimal.valueOf(1299));
                p.setCategory(kidsCat);
                p.setCategoryName(kidsCat.getName());
                p.setImageUrl("https://images.unsplash.com/photo-1519457431-44ccd64a579b?w=900");
                p.setStock(25);
                p.setActive(true);
                Product saved = productRepo.save(p);
                sizeRepo.save(new ProductSize(saved, "3-4Y", 25));
                colorRepo.save(new ProductColor(saved, "Classic Indigo", "#2b3e50"));
                return saved;
            });

            Product kidsDress = productRepo.findBySku("SH-K-025").orElseGet(() -> {
                Product p = new Product();
                p.setName("Kids Floral Organic Cotton Dress");
                p.setSku("SH-K-025");
                p.setDescription("Airy A-line summer dress crafted in pure organic cotton with hand-drawn florals.");
                p.setPrice(BigDecimal.valueOf(899));
                p.setCategory(kidsCat);
                p.setCategoryName(kidsCat.getName());
                p.setImageUrl("https://images.unsplash.com/photo-1518831959646-742c3a14ebf7?w=900");
                p.setStock(30);
                p.setActive(true);
                Product saved = productRepo.save(p);
                sizeRepo.save(new ProductSize(saved, "4-5Y", 30));
                colorRepo.save(new ProductColor(saved, "Pastel Floral", "#f8bbd0"));
                return saved;
            });

            Product kidsSweater = productRepo.findBySku("SH-K-026").orElseGet(() -> {
                Product p = new Product();
                p.setName("Kids Cozy Knit Colorblock Pullover");
                p.setSku("SH-K-026");
                p.setDescription("Chunky warm knit sweater with ribbed cuffs and contrast earthy colorblocking.");
                p.setPrice(BigDecimal.valueOf(999));
                p.setCategory(kidsCat);
                p.setCategoryName(kidsCat.getName());
                p.setImageUrl("https://images.unsplash.com/photo-1508214751196-bcfd4ca60f91?w=900");
                p.setStock(25);
                p.setActive(true);
                Product saved = productRepo.save(p);
                sizeRepo.save(new ProductSize(saved, "4-5Y", 25));
                colorRepo.save(new ProductColor(saved, "Mustard & Cream", "#e0a96d"));
                return saved;
            });

            // 5. Seed Discount Coupons
            if (couponRepo.findByCodeIgnoreCase("BLACKFRIDAY").isEmpty()) {
                couponRepo.save(new Coupon("BLACKFRIDAY", DiscountType.PERCENTAGE, BigDecimal.valueOf(30), BigDecimal.valueOf(999), LocalDate.now().plusYears(1)));
                couponRepo.save(new Coupon("BF500", DiscountType.FIXED_AMOUNT, BigDecimal.valueOf(500), BigDecimal.valueOf(1999), LocalDate.now().plusYears(1)));
            }
            if (couponRepo.count() <= 2) {
                couponRepo.save(new Coupon("STYLE20", DiscountType.PERCENTAGE, BigDecimal.valueOf(20), BigDecimal.valueOf(1200), LocalDate.now().plusMonths(6)));
                couponRepo.save(new Coupon("WELCOME100", DiscountType.FIXED_AMOUNT, BigDecimal.valueOf(100), BigDecimal.valueOf(999), LocalDate.now().plusMonths(6)));
                couponRepo.save(new Coupon("LUXURY500", DiscountType.FIXED_AMOUNT, BigDecimal.valueOf(500), BigDecimal.valueOf(2500), LocalDate.now().plusMonths(12)));
            }

            // 6. Seed the 12 Price-Wise Combo Packages (+ 1 Luxury Combo)
            if (comboRepo.count() == 0) {
                log.info("Seeding Price-Wise Clothing Combo Packages...");

                // 1. Budget Combos (₹299 – ₹999)
                seedCombo(comboRepo, comboItemRepo,
                        "Combo 1 – T-Shirt Pack", "combo-1-t-shirt-pack", ComboTier.BUDGET,
                        "Twin pack of 100% combed cotton crew-neck tees with choice of sizes and colours. Perfect everyday basic layering.",
                        "https://images.unsplash.com/photo-1581655353564-df123a1eb820?w=900",
                        499, 998, "Save 50% (Bestseller)",
                        List.of(new BundleItemSpec(tshirt, "1 Classic Crew T-Shirt", 1),
                                new BundleItemSpec(tshirt, "1 Contrast Crew T-Shirt", 1)));

                seedCombo(comboRepo, comboItemRepo,
                        "Combo 2 – Daily Wear", "combo-2-daily-wear", ComboTier.BUDGET,
                        "Essential daily rotation featuring 1 button-down casual shirt and 1 breathable cotton tee.",
                        "https://images.unsplash.com/photo-1489987707025-afc232f7ea0f?w=900",
                        699, 1499, "Essential Value",
                        List.of(new BundleItemSpec(shirt, "1 Oxford Casual Shirt", 1),
                                new BundleItemSpec(tshirt, "1 Pima Cotton T-Shirt", 1)));

                seedCombo(comboRepo, comboItemRepo,
                        "Combo 3 – Women's Daily Wear", "combo-3-womens-daily-wear", ComboTier.BUDGET,
                        "Effortless daily set pairing a soft relaxed top with high-stretch comfort cotton leggings.",
                        "https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?w=900",
                        799, 1599, "Popular Daily",
                        List.of(new BundleItemSpec(tshirt, "1 Relaxed Cotton Top", 1),
                                new BundleItemSpec(leggings, "1 Stretch Cotton Leggings", 1)));

                seedCombo(comboRepo, comboItemRepo,
                        "Combo 4 – Kids Wear", "combo-4-kids-wear", ComboTier.BUDGET,
                        "Comfort-first playwear pairing an organic cotton graphic tee with elasticated drawstring shorts.",
                        "https://images.unsplash.com/photo-1519238263530-99bdd11df2ea?w=900",
                        599, 1098, "Kids Favourite",
                        List.of(new BundleItemSpec(tshirt, "1 Organic Cotton T-Shirt", 1),
                                new BundleItemSpec(kidsShorts, "1 Active Drawstring Shorts", 1)));

                // 2. Standard Combos (₹1,000 – ₹2,499)
                seedCombo(comboRepo, comboItemRepo,
                        "Combo 5 – Men's Casual", "combo-5-mens-casual", ComboTier.STANDARD,
                        "Classic weekend trio combining a crisp button-down shirt, dark indigo jeans, and a full-grain leather belt.",
                        "https://images.unsplash.com/photo-1490114538077-0a7f8cb49891?w=900",
                        1499, 2999, "3-Piece Classic (Save 50%)",
                        List.of(new BundleItemSpec(shirt, "1 Oxford Cotton Shirt", 1, BigDecimal.valueOf(699)),
                                new BundleItemSpec(jeans, "1 Straight-Leg Denim Jeans", 1, BigDecimal.valueOf(500)),
                                new BundleItemSpec(belt, "1 Full-Grain Leather Belt", 1, BigDecimal.valueOf(300))));

                seedCombo(comboRepo, comboItemRepo,
                        "Combo 6 – Women's Ethnic", "combo-6-womens-ethnic", ComboTier.STANDARD,
                        "Graceful 3-piece ethnic set including a hand-block printed cotton kurti, ankle-length leggings, and gold-bordered chiffon dupatta.",
                        "https://images.unsplash.com/photo-1610030469983-98e550d6193c?w=900",
                        1799, 3299, "Festive Ready",
                        List.of(new BundleItemSpec(kurti, "1 Hand-Block Printed Kurti", 1),
                                new BundleItemSpec(leggings, "1 Ankle Leggings", 1),
                                new BundleItemSpec(dupatta, "1 Pure Chiffon Dupatta", 1)));

                seedCombo(comboRepo, comboItemRepo,
                        "Combo 7 – Smart Casual", "combo-7-smart-casual", ComboTier.STANDARD,
                        "Sharp smart-casual ensemble: structured polo shirt, raw selvedge denim jeans, and pristine white low-top leather sneakers.",
                        "https://images.unsplash.com/photo-1479064555552-3ef4979f8908?w=900",
                        2299, 4199, "Weekend Smart",
                        List.of(new BundleItemSpec(shirt, "1 Pique Cotton Polo", 1),
                                new BundleItemSpec(jeans, "1 Dark Indigo Jeans", 1),
                                new BundleItemSpec(sneakers, "1 Pair Minimal Leather Sneakers", 1)));

                seedCombo(comboRepo, comboItemRepo,
                        "Combo 8 – Fashion Pack", "combo-8-fashion-pack", ComboTier.STANDARD,
                        "Modern street style edit featuring a ribbed modal top, high-rise tailored jeans, and a premium leather crossbody handbag.",
                        "https://images.unsplash.com/photo-1483985988355-763728e1935b?w=900",
                        2199, 3999, "Editorial Pick",
                        List.of(new BundleItemSpec(tshirt, "1 Ribbed Modal Top", 1),
                                new BundleItemSpec(jeans, "1 Tapered Denim Jeans", 1),
                                new BundleItemSpec(handbag, "1 Leather Crossbody Bag", 1)));

                // 3. Premium Combos (₹2,500 – ₹5,999)
                seedCombo(comboRepo, comboItemRepo,
                        "Combo 9 – Men's Premium", "combo-9-mens-premium", ComboTier.PREMIUM,
                        "Executive 4-piece formal attire: wool-blend tailored blazer, crisp twill shirt, pleated trousers, and matching leather belt.",
                        "https://images.unsplash.com/photo-1507679799987-c73779587ccf?w=900",
                        3999, 6999, "Executive 4-Piece",
                        List.of(new BundleItemSpec(blazer, "1 Tailored Wool-Blend Blazer", 1),
                                new BundleItemSpec(shirt, "1 Formal Twill Shirt", 1),
                                new BundleItemSpec(trousers, "1 Pleated Chino Trousers", 1),
                                new BundleItemSpec(belt, "1 Classic Leather Belt", 1)));

                seedCombo(comboRepo, comboItemRepo,
                        "Combo 10 – Women's Premium", "combo-10-womens-premium", ComboTier.PREMIUM,
                        "Evening cocktail glamour: Mulberry silk satin midi dress paired with an Italian leather bag and footwear.",
                        "https://images.unsplash.com/photo-1595777457583-95e059d581b8?w=900",
                        4499, 7999, "Cocktail Gala",
                        List.of(new BundleItemSpec(dress, "1 Silk Satin Midi Slip Dress", 1),
                                new BundleItemSpec(handbag, "1 Italian Leather Shoulder Bag", 1),
                                new BundleItemSpec(sneakers, "1 Pair Designer Heels/Shoes", 1)));

                seedCombo(comboRepo, comboItemRepo,
                        "Combo 11 – Complete Look", "combo-11-complete-look", ComboTier.PREMIUM,
                        "Urban winter statement: Japanese selvedge denim jacket, Oxford shirt, straight jeans, and clean sneakers.",
                        "https://images.unsplash.com/photo-1576995853123-5a10305d93c0?w=900",
                        5499, 8999, "Urban Luxe 4-Piece",
                        List.of(new BundleItemSpec(jacket, "1 Vintage Denim Jacket", 1),
                                new BundleItemSpec(shirt, "1 Oxford Shirt", 1),
                                new BundleItemSpec(jeans, "1 Raw Denim Jeans", 1),
                                new BundleItemSpec(sneakers, "1 Pair Leather Sneakers", 1)));

                seedCombo(comboRepo, comboItemRepo,
                        "Combo 12 – Luxury Ethnic", "combo-12-luxury-ethnic", ComboTier.PREMIUM,
                        "Royal wedding festive suite: hand-embroidered Chanderi silk suit, zari dupatta, and embellished evening handbag.",
                        "https://images.unsplash.com/photo-1617627143750-d86bc21e42bb?w=900",
                        5999, 10999, "Royal Heritage",
                        List.of(new BundleItemSpec(kurti, "1 Chanderi Silk Suit", 1),
                                new BundleItemSpec(dupatta, "1 Zari Border Dupatta", 1),
                                new BundleItemSpec(handbag, "1 Embellished Handbag", 1)));

                // 4. Luxury Combos (Above ₹6,000)
                seedCombo(comboRepo, comboItemRepo,
                        "Combo 13 – Bespoke Wedding Ensemble", "combo-13-bespoke-wedding-ensemble", ComboTier.LUXURY,
                        "Bespoke ceremonial collection: Handcrafted silk sherwani jacket, matching silk trousers, pure velvet stole, and leather mojari.",
                        "https://images.unsplash.com/photo-1583391733956-3750e0ff4e8b?w=900",
                        7499, 13999, "Bespoke Luxury",
                        List.of(new BundleItemSpec(blazer, "1 Silk Ceremonial Sherwani Jacket", 1),
                                new BundleItemSpec(trousers, "1 Silk Blend Trousers", 1),
                                new BundleItemSpec(dupatta, "1 Velvet Embellished Stole", 1),
                                new BundleItemSpec(sneakers, "1 Pair Handcrafted Footwear", 1)));

                log.info("Successfully seeded {} Combo Packages across all budget tiers.", comboRepo.count());
            }

            // 7. Active Catalog Synchronization & Image Repair (Ensures zero mismatched photos and full combo bundles)
            syncProductVisuals(productRepo, imageRepo, colorRepo);
            syncComboPackages(comboRepo, comboItemRepo, productRepo);

            log.info("StyleHub database seeding complete. Active products: {}, Categories: {}, Combos: {}",
                    productRepo.count(), categoryRepo.count(), comboRepo.count());
        };
    }

    private void syncProductVisuals(ProductRepository productRepo, ProductImageRepository imageRepo, ProductColorRepository colorRepo) {
        log.info("Performing precise high-fashion photography synchronization for all products...");

        List<ProductVisualPreset> presets = List.of(
                // 1. Men's Oxford Shirt
                new ProductVisualPreset("SH-M-001",
                        "https://images.unsplash.com/photo-1596755094514-f87e34085b2c?w=900",
                        List.of("https://images.unsplash.com/photo-1602810318383-e386cc2a3ccf?w=900", "https://images.unsplash.com/photo-1603252109303-2751441dd157?w=900")),

                // 2. Men's Selvedge Denim Jacket
                new ProductVisualPreset("SH-M-002",
                        "https://images.unsplash.com/photo-1576995853123-5a10305d93c0?w=900",
                        List.of("https://images.unsplash.com/photo-1551028719-00167b16eac5?w=900", "https://images.unsplash.com/photo-1544441893-675973e31985?w=900")),

                // 3. Men's Turtleneck Knit
                new ProductVisualPreset("SH-M-003",
                        "https://images.unsplash.com/photo-1620799140408-edc6dcb6d633?w=900",
                        List.of("https://images.unsplash.com/photo-1614975058789-41316d0e2e9c?w=900")),

                // 4. Men's Pleated Chino Trousers
                new ProductVisualPreset("SH-M-004",
                        "https://images.unsplash.com/photo-1479064555552-3ef4979f8908?w=900",
                        List.of("https://images.unsplash.com/photo-1624378439575-d8705ad7ae80?w=900")),

                // 5. Women's Floral Midi Dress
                new ProductVisualPreset("SH-W-005",
                        "https://images.unsplash.com/photo-1572804013309-59a88b7e92f1?w=900",
                        List.of("https://images.unsplash.com/photo-1515372039744-b8f02a3ae446?w=900", "https://images.unsplash.com/photo-1496747611176-843222e1e57c?w=900")),

                // 6. Women's Oversized Wool Trench Coat (Elegant camel wool coat on model)
                new ProductVisualPreset("SH-W-006",
                        "https://images.unsplash.com/photo-1539533018447-63fcce2678e3?w=900",
                        List.of("https://images.unsplash.com/photo-1592327877233-90b9bfd92e48?w=900", "https://images.unsplash.com/photo-1617391258031-f8d80b22fb35?w=900")),

                // 7. Women's Cashmere Cardigan
                new ProductVisualPreset("SH-W-007",
                        "https://images.unsplash.com/photo-1434389677669-e08b4cac3105?w=900",
                        List.of("https://images.unsplash.com/photo-1608256246200-53e635b5b65f?w=900")),

                // 8. Women's High-Rise Crop Jeans
                new ProductVisualPreset("SH-W-008",
                        "https://images.unsplash.com/photo-1541099649105-f69ad21f3246?w=900",
                        List.of("https://images.unsplash.com/photo-1584370848010-d7fe6bc767ec?w=900")),

                // 9. Kids Pure Cotton 2-Piece Lounge Set
                new ProductVisualPreset("SH-K-009",
                        "https://images.unsplash.com/photo-1519238263530-99bdd11df2ea?w=900",
                        List.of("https://images.unsplash.com/photo-1622290291468-a28f7a7dc6a8?w=900")),

                // 10. Kids Hooded Windbreaker Jacket
                new ProductVisualPreset("SH-K-010",
                        "https://images.unsplash.com/photo-1503454537195-1dcabb73ffb9?w=900",
                        List.of("https://images.unsplash.com/photo-1518831959646-742c3a14ebf7?w=900")),

                // 11. Minimal Structured Canvas Tote (Structured minimalist cream canvas bag)
                new ProductVisualPreset("SH-ACC-011",
                        "https://images.unsplash.com/photo-1574365569389-a10d488ca3fb?w=900",
                        List.of("https://images.unsplash.com/photo-1544816155-12df9643f363?w=900", "https://images.unsplash.com/photo-1578237493287-8d4d2b03591a?w=900")),

                // 12. Classic Full-Grain Leather Belt
                new ProductVisualPreset("SH-ACC-012",
                        "https://images.unsplash.com/photo-1624222247344-550fb60583dc?w=900",
                        List.of()),

                // 13. Footwear: Monochrome Leather Court Sneakers (Pristine minimal white court sneaker)
                new ProductVisualPreset("SH-FT-013",
                        "https://images.unsplash.com/photo-1560769629-975ec94e6a86?w=900",
                        List.of("https://images.unsplash.com/photo-1595950653106-6c9ebd614d3a?w=900", "https://images.unsplash.com/photo-1549298916-b41d501d3772?w=900")),

                // 14. Footwear: Suede Chelsea Boots
                new ProductVisualPreset("SH-FT-014",
                        "https://images.unsplash.com/photo-1638247025967-b4e38f787b76?w=900",
                        List.of("https://images.unsplash.com/photo-1543163521-1bf539c55dd2?w=900")),

                // 16. Men's Straight-Leg Denim Jeans (Clean folded studio and on-model denim photography)
                new ProductVisualPreset("SH-M-015",
                        "https://images.unsplash.com/photo-1542272604-787c3835535d?w=900",
                        List.of("https://images.unsplash.com/photo-1541099649105-f69ad21f3246?w=900", "https://images.unsplash.com/photo-1584370848010-d7fe6bc767ec?w=900")),

                // 17. Handcrafted Leather Belt
                new ProductVisualPreset("SH-ACC-016",
                        "https://images.unsplash.com/photo-1624222247344-550fb60583dc?w=900",
                        List.of(),
                        List.of(
                                new ColorVariantPreset("Cognac Brown", "#795548", "https://images.unsplash.com/photo-1624222247344-550fb60583dc?w=900")
                        )),

                // 18. Minimal Low-Top Leather Sneakers (Pristine minimal white leather sneaker)
                new ProductVisualPreset("SH-FT-017",
                        "https://images.unsplash.com/photo-1549298916-b41d501d3772?w=900",
                        List.of("https://images.unsplash.com/photo-1595950653106-6c9ebd614d3a?w=900"),
                        List.of(
                                new ColorVariantPreset("Optic White", "#ffffff", "https://images.unsplash.com/photo-1549298916-b41d501d3772?w=900"),
                                new ColorVariantPreset("Triple Black", "#111111", "https://images.unsplash.com/photo-1551107696-a4b0c5a0d9a2?w=900")
                        )),

                // 19. Women's Hand-Block Printed Kurti
                new ProductVisualPreset("SH-W-018",
                        "https://images.unsplash.com/photo-1610030469983-98e550d6193c?w=900",
                        List.of("https://images.unsplash.com/photo-1583391733956-3750e0ff4e8b?w=900")),

                // 20. Women's Stretch Comfort Cotton Leggings (Pristine activewear leggings photography)
                new ProductVisualPreset("SH-W-019",
                        "https://images.unsplash.com/photo-1509916796186-85dc916cb043?w=900",
                        List.of("https://images.unsplash.com/photo-1518611012118-696072aa579a?w=900"),
                        List.of(
                                new ColorVariantPreset("Jet Black", "#111111", "https://images.unsplash.com/photo-1509916796186-85dc916cb043?w=900"),
                                new ColorVariantPreset("Charcoal Grey", "#424242", "https://images.unsplash.com/photo-1518611012118-696072aa579a?w=900")
                        )),

                // 21. Pure Chiffon Gold Border Dupatta (Elegant festive sheer stole)
                new ProductVisualPreset("SH-W-020",
                        "https://images.unsplash.com/photo-1583391733956-3750e0ff4e8b?w=900",
                        List.of("https://images.unsplash.com/photo-1617627143750-d86bc21e42bb?w=900"),
                        List.of(
                                new ColorVariantPreset("Gold Ivory", "#d4af37", "https://images.unsplash.com/photo-1583391733956-3750e0ff4e8b?w=900"),
                                new ColorVariantPreset("Rani Pink & Gold", "#c2185b", "https://images.unsplash.com/photo-1617627143750-d86bc21e42bb?w=900")
                        )),

                // 22. Italian Leather Crossbody Bag (ONLY authentic luxury leather crossbody bags - NO backpacks/wicker!)
                new ProductVisualPreset("SH-ACC-021",
                        "https://images.unsplash.com/photo-1584917865442-de89df76afd3?w=900",
                        List.of("https://images.unsplash.com/photo-1548036328-c9fa89d128fa?w=900", "https://images.unsplash.com/photo-1590874103328-eac38a683ce7?w=900"),
                        List.of(
                                new ColorVariantPreset("Cognac Brown", "#8d4925", "https://images.unsplash.com/photo-1584917865442-de89df76afd3?w=900"),
                                new ColorVariantPreset("Saddle Tan", "#c68642", "https://images.unsplash.com/photo-1548036328-c9fa89d128fa?w=900"),
                                new ColorVariantPreset("Noir Black", "#1a1a1a", "https://images.unsplash.com/photo-1590874103328-eac38a683ce7?w=900")
                        )),

                // 23. Tailored Wool-Blend Evening Blazer (Men)
                new ProductVisualPreset("SH-M-022",
                        "https://images.unsplash.com/photo-1507679799987-c73779587ccf?w=900",
                        List.of("https://images.unsplash.com/photo-1594938298603-c8148c4dae35?w=900"),
                        List.of(
                                new ColorVariantPreset("Charcoal Grey", "#37474f", "https://images.unsplash.com/photo-1507679799987-c73779587ccf?w=900"),
                                new ColorVariantPreset("Sky Blue Tweed", "#7ba2c2", "https://images.unsplash.com/photo-1594938298603-c8148c4dae35?w=900")
                        )),

                // 24. Kids Active Drawstring Shorts (Cute organic cotton playwear shorts)
                new ProductVisualPreset("SH-K-023",
                        "https://images.unsplash.com/photo-1622290291468-a28f7a7dc6a8?w=900",
                        List.of("https://images.unsplash.com/photo-1519238263530-99bdd11df2ea?w=900"),
                        List.of(
                                new ColorVariantPreset("Electric Blue", "#1976d2", "https://images.unsplash.com/photo-1622290291468-a28f7a7dc6a8?w=900"),
                                new ColorVariantPreset("Navy Blue", "#0d47a1", "https://images.unsplash.com/photo-1519238263530-99bdd11df2ea?w=900")
                        )),

                // 25. Classic Crew Neck Cotton T-Shirt (Crisp pure white combed cotton jersey tee on minimal hanger)
                new ProductVisualPreset("SH-M-007",
                        "https://images.unsplash.com/photo-1581655353564-df123a1eb820?w=900",
                        List.of("https://images.unsplash.com/photo-1583743814966-8936f5b7be1a?w=900", "https://images.unsplash.com/photo-1618354691373-d851c5c3a990?w=900"),
                        List.of(
                                new ColorVariantPreset("Optic White", "#ffffff", "https://images.unsplash.com/photo-1581655353564-df123a1eb820?w=900"),
                                new ColorVariantPreset("Charcoal Heather", "#333333", "https://images.unsplash.com/photo-1583743814966-8936f5b7be1a?w=900"),
                                new ColorVariantPreset("Classic Black", "#111111", "https://images.unsplash.com/photo-1618354691373-d851c5c3a990?w=900")
                        )),

                // 26. Round Toe Pivot Pro Sneakers (U.S. Polo Assn. high-tech lifestyle athletic footwear)
                new ProductVisualPreset("SH-FT-031",
                        "https://images.unsplash.com/photo-1595950653106-6c9ebd614d3a?w=900",
                        List.of("https://images.unsplash.com/photo-1551107696-a4b0c5a0d9a2?w=900", "https://images.unsplash.com/photo-1560769629-975ec94e6a86?w=900", "https://images.unsplash.com/photo-1600185365926-3a2ce3cdb9eb?w=900"),
                        List.of(
                                new ColorVariantPreset("Optic White", "#f4f4f4", "https://images.unsplash.com/photo-1595950653106-6c9ebd614d3a?w=900"),
                                new ColorVariantPreset("Triple Black", "#1a1a1a", "https://images.unsplash.com/photo-1551107696-a4b0c5a0d9a2?w=900"),
                                new ColorVariantPreset("Navy Tech", "#1f2a44", "https://images.unsplash.com/photo-1560769629-975ec94e6a86?w=900"),
                                new ColorVariantPreset("Solar Orange", "#e65100", "https://images.unsplash.com/photo-1600185365926-3a2ce3cdb9eb?w=900")
                        )),

                // 27. Kids Denim Dungarees & Striped Tee
                new ProductVisualPreset("SH-K-024",
                        "https://images.unsplash.com/photo-1519457431-44ccd64a579b?w=900",
                        List.of(),
                        List.of(
                                new ColorVariantPreset("Classic Indigo", "#2b3e50", "https://images.unsplash.com/photo-1519457431-44ccd64a579b?w=900")
                        )),

                // 28. Kids Floral Organic Cotton Dress
                new ProductVisualPreset("SH-K-025",
                        "https://images.unsplash.com/photo-1518831959646-742c3a14ebf7?w=900",
                        List.of(),
                        List.of(
                                new ColorVariantPreset("Pastel Floral", "#f8bbd0", "https://images.unsplash.com/photo-1518831959646-742c3a14ebf7?w=900")
                        )),

                // 29. Kids Cozy Knit Colorblock Pullover
                new ProductVisualPreset("SH-K-026",
                        "https://images.unsplash.com/photo-1508214751196-bcfd4ca60f91?w=900",
                        List.of(),
                        List.of(
                                new ColorVariantPreset("Mustard & Cream", "#e0a96d", "https://images.unsplash.com/photo-1508214751196-bcfd4ca60f91?w=900")
                        ))
        );

        for (ProductVisualPreset preset : presets) {
            Product p = productRepo.findBySku(preset.sku).orElse(null);
            if (p == null) {
                // Try to find by ID or name
                String searchPattern = preset.sku.toLowerCase();
                for (Product existing : productRepo.findAll()) {
                    String existingName = existing.getName() != null ? existing.getName().toLowerCase() : "";
                    if (searchPattern.contains("026") && existingName.contains("pullover")) { p = existing; break; }
                    if (searchPattern.contains("025") && existingName.contains("floral")) { p = existing; break; }
                    if (searchPattern.contains("024") && existingName.contains("dungarees")) { p = existing; break; }
                    if (searchPattern.contains("031") && existingName.contains("pivot pro")) { p = existing; break; }
                    if (searchPattern.contains("007") && existingName.contains("crew neck")) { p = existing; break; }
                    if (searchPattern.contains("023") && existingName.contains("drawstring shorts")) { p = existing; break; }
                    if (searchPattern.contains("022") && existingName.contains("blazer")) { p = existing; break; }
                    if (searchPattern.contains("021") && existingName.contains("crossbody bag")) { p = existing; break; }
                    if (searchPattern.contains("020") && existingName.contains("dupatta")) { p = existing; break; }
                    if (searchPattern.contains("019") && existingName.contains("leggings")) { p = existing; break; }
                    if (searchPattern.contains("018") && existingName.contains("kurti")) { p = existing; break; }
                    if (searchPattern.contains("017") && existingName.contains("low-top")) { p = existing; break; }
                    if (searchPattern.contains("016") && existingName.contains("handcrafted leather belt")) { p = existing; break; }
                    if (searchPattern.contains("015") && existingName.contains("straight-leg denim")) { p = existing; break; }
                    if (searchPattern.contains("014") && existingName.contains("chelsea boots")) { p = existing; break; }
                    if (searchPattern.contains("013") && existingName.contains("court sneakers")) { p = existing; break; }
                    if (searchPattern.contains("012") && existingName.contains("classic full-grain")) { p = existing; break; }
                    if (searchPattern.contains("011") && existingName.contains("canvas tote")) { p = existing; break; }
                    if (searchPattern.contains("010") && existingName.contains("windbreaker")) { p = existing; break; }
                    if (searchPattern.contains("009") && existingName.contains("lounge set")) { p = existing; break; }
                    if (searchPattern.contains("008") && existingName.contains("high-rise")) { p = existing; break; }
                    if (searchPattern.contains("007") && existingName.contains("cardigan")) { p = existing; break; }
                    if (searchPattern.contains("006") && existingName.contains("trench coat")) { p = existing; break; }
                    if (searchPattern.contains("005") && existingName.contains("slip dress")) { p = existing; break; }
                    if (searchPattern.contains("004") && existingName.contains("chino trousers")) { p = existing; break; }
                    if (searchPattern.contains("003") && existingName.contains("turtleneck")) { p = existing; break; }
                    if (searchPattern.contains("002") && existingName.contains("denim jacket")) { p = existing; break; }
                    if (searchPattern.contains("001") && existingName.contains("oxford")) { p = existing; break; }
                }
            }

            if (p != null) {
                p.setImageUrl(preset.primaryImg);
                p.setSku(preset.sku);
                productRepo.save(p);

                // Overwrite gallery images with only authentic angles
                List<ProductImage> imgs = imageRepo.findByProductIdOrderBySortOrderAsc(p.getId());
                imageRepo.deleteAll(imgs);

                imageRepo.save(new ProductImage(p, preset.primaryImg, p.getName() + " - Primary Profile", true, 0));
                int order = 1;
                for (String gUrl : preset.galleryImgs) {
                    imageRepo.save(new ProductImage(p, gUrl, p.getName() + " - Angle " + order, false, order++));
                }
            }
        }
    }

    private void syncComboPackages(ComboPackageRepository comboRepo, ComboItemRepository comboItemRepo, ProductRepository productRepo) {
        List<ComboPackage> allCombos = comboRepo.findAll();

        Product shirt = productRepo.findBySku("SH-M-001").orElseGet(() -> productRepo.findAll().stream().filter(p -> "men".equalsIgnoreCase(p.getCategoryName())).findFirst().orElse(null));
        Product jacket = productRepo.findBySku("SH-M-002").orElseGet(() -> shirt);
        Product trousers = productRepo.findBySku("SH-M-004").orElseGet(() -> shirt);
        Product dress = productRepo.findBySku("SH-W-005").orElseGet(() -> productRepo.findAll().stream().filter(p -> "women".equalsIgnoreCase(p.getCategoryName())).findFirst().orElse(null));
        Product tshirt = productRepo.findBySku("SH-M-007").orElseGet(() -> shirt);
        Product jeans = productRepo.findBySku("SH-M-015").orElseGet(() -> shirt);
        Product belt = productRepo.findBySku("SH-ACC-016").orElseGet(() -> shirt);
        Product sneakers = productRepo.findBySku("SH-FT-017").orElseGet(() -> productRepo.findBySku("SH-FT-031").orElseGet(() -> shirt));
        Product kurti = productRepo.findBySku("SH-W-018").orElseGet(() -> dress);
        Product leggings = productRepo.findBySku("SH-W-019").orElseGet(() -> dress);
        Product dupatta = productRepo.findBySku("SH-W-020").orElseGet(() -> dress);
        Product handbag = productRepo.findBySku("SH-ACC-021").orElseGet(() -> belt);
        Product blazer = productRepo.findBySku("SH-M-022").orElseGet(() -> jacket);
        Product kidsShorts = productRepo.findBySku("SH-K-023").orElseGet(() -> tshirt);

        for (ComboPackage cp : allCombos) {
            String slug = cp.getSlug() != null ? cp.getSlug() : "";
            String img = cp.getImageUrl();

            // Replace any expired or temporary openai links or mismatched placeholders with permanent curated flat-lay photography
            if (img == null || img.contains("openai.com") || img.contains("1506619216599") || img.contains("1542272604-780c96856592") || img.contains("1521572267360") || img.isBlank()) {
                if (slug.contains("t-shirt-pack") || slug.contains("combo-1")) {
                    cp.setImageUrl("https://images.unsplash.com/photo-1581655353564-df123a1eb820?w=900");
                } else if (slug.contains("daily-wear") && !slug.contains("women")) {
                    cp.setImageUrl("https://images.unsplash.com/photo-1489987707025-afc232f7ea0f?w=900");
                } else if (slug.contains("womens-daily-wear") || slug.contains("combo-3")) {
                    cp.setImageUrl("https://images.unsplash.com/photo-1789111161057-90c5ce12dc21?w=900");
                } else if (slug.contains("kids-wear") || slug.contains("combo-4")) {
                    cp.setImageUrl("https://images.unsplash.com/photo-1622290291468-a28f7a7dc6a8?w=900");
                } else if (slug.contains("mens-casual") || slug.contains("combo-5")) {
                    cp.setImageUrl("https://images.unsplash.com/photo-1714143136372-ddaf8b606da7?w=900");
                } else if (slug.contains("womens-ethnic") || slug.contains("combo-6")) {
                    cp.setImageUrl("https://images.unsplash.com/photo-1610030469983-98e550d6193c?w=900");
                } else if (slug.contains("smart-casual") || slug.contains("combo-7")) {
                    cp.setImageUrl("https://images.unsplash.com/photo-1479064555552-3ef4979f8908?w=900");
                } else if (slug.contains("fashion-pack") || slug.contains("combo-8")) {
                    cp.setImageUrl("https://images.unsplash.com/photo-1483985988355-763728e1935b?w=900");
                } else if (slug.contains("mens-premium") || slug.contains("combo-9")) {
                    cp.setImageUrl("https://images.unsplash.com/photo-1507679799987-c73779587ccf?w=900");
                } else if (slug.contains("womens-premium") || slug.contains("combo-10")) {
                    cp.setImageUrl("https://images.unsplash.com/photo-1595777457583-95e059d581b8?w=900");
                } else if (slug.contains("complete-look") || slug.contains("combo-11")) {
                    cp.setImageUrl("https://images.unsplash.com/photo-1576995853123-5a10305d93c0?w=900");
                } else if (slug.contains("luxury-ethnic") || slug.contains("combo-12")) {
                    cp.setImageUrl("https://images.unsplash.com/photo-1583391733956-3750e0ff4e8b?w=900");
                } else {
                    cp.setImageUrl("https://images.unsplash.com/photo-1490481651871-ab68de25d43d?w=900");
                }
                comboRepo.save(cp);
            }

            // Ensure constituent items exist and are not empty
            List<ComboItem> existingItems = comboItemRepo.findByComboPackageIdOrderByDisplayOrderAsc(cp.getId());
            int requiredCount = 2;
            if (slug.contains("combo-5") || slug.contains("combo-6") || slug.contains("combo-7") || slug.contains("combo-8") || slug.contains("combo-10") || slug.contains("combo-12")) {
                requiredCount = 3;
            } else if (slug.contains("combo-9") || slug.contains("combo-11") || slug.contains("combo-13")) {
                requiredCount = 4;
            }

            if (existingItems.size() < requiredCount) {
                // Re-seed constituent garments
                comboItemRepo.deleteAll(existingItems);
                List<BundleItemSpec> specs = new ArrayList<>();

                if (slug.contains("combo-1") || slug.contains("t-shirt-pack")) {
                    specs.add(new BundleItemSpec(tshirt, "1 Classic Crew T-Shirt", 1, BigDecimal.valueOf(249)));
                    specs.add(new BundleItemSpec(tshirt, "1 Contrast Crew T-Shirt", 1, BigDecimal.valueOf(250)));
                } else if (slug.contains("combo-2") || slug.contains("daily-wear")) {
                    specs.add(new BundleItemSpec(shirt, "1 Oxford Casual Shirt", 1, BigDecimal.valueOf(449)));
                    specs.add(new BundleItemSpec(tshirt, "1 Pima Cotton T-Shirt", 1, BigDecimal.valueOf(250)));
                } else if (slug.contains("combo-3") || slug.contains("womens-daily-wear")) {
                    specs.add(new BundleItemSpec(tshirt, "1 Relaxed Cotton Top", 1, BigDecimal.valueOf(399)));
                    specs.add(new BundleItemSpec(leggings, "1 Stretch Cotton Leggings", 1, BigDecimal.valueOf(400)));
                } else if (slug.contains("combo-4") || slug.contains("kids-wear")) {
                    specs.add(new BundleItemSpec(tshirt, "1 Organic Cotton T-Shirt", 1, BigDecimal.valueOf(299)));
                    specs.add(new BundleItemSpec(kidsShorts, "1 Active Drawstring Shorts", 1, BigDecimal.valueOf(300)));
                } else if (slug.contains("combo-5") || slug.contains("mens-casual")) {
                    specs.add(new BundleItemSpec(shirt, "1 Oxford Cotton Shirt", 1, BigDecimal.valueOf(699)));
                    specs.add(new BundleItemSpec(jeans, "1 Straight-Leg Denim Jeans", 1, BigDecimal.valueOf(500)));
                    specs.add(new BundleItemSpec(belt, "1 Full-Grain Leather Belt", 1, BigDecimal.valueOf(300)));
                } else if (slug.contains("combo-6") || slug.contains("womens-ethnic")) {
                    specs.add(new BundleItemSpec(kurti, "1 Hand-Block Printed Kurti", 1, BigDecimal.valueOf(899)));
                    specs.add(new BundleItemSpec(leggings, "1 Ankle Leggings", 1, BigDecimal.valueOf(400)));
                    specs.add(new BundleItemSpec(dupatta, "1 Pure Chiffon Dupatta", 1, BigDecimal.valueOf(500)));
                } else if (slug.contains("combo-7") || slug.contains("smart-casual")) {
                    specs.add(new BundleItemSpec(shirt, "1 Pique Cotton Polo", 1, BigDecimal.valueOf(799)));
                    specs.add(new BundleItemSpec(jeans, "1 Dark Indigo Jeans", 1, BigDecimal.valueOf(800)));
                    specs.add(new BundleItemSpec(sneakers, "1 Pair Minimal Leather Sneakers", 1, BigDecimal.valueOf(700)));
                } else if (slug.contains("combo-8") || slug.contains("fashion-pack")) {
                    specs.add(new BundleItemSpec(tshirt, "1 Ribbed Modal Top", 1, BigDecimal.valueOf(599)));
                    specs.add(new BundleItemSpec(jeans, "1 Tapered Denim Jeans", 1, BigDecimal.valueOf(800)));
                    specs.add(new BundleItemSpec(handbag, "1 Leather Crossbody Bag", 1, BigDecimal.valueOf(800)));
                } else if (slug.contains("combo-9") || slug.contains("mens-premium")) {
                    specs.add(new BundleItemSpec(blazer, "1 Tailored Wool-Blend Blazer", 1, BigDecimal.valueOf(1899)));
                    specs.add(new BundleItemSpec(shirt, "1 Formal Twill Shirt", 1, BigDecimal.valueOf(700)));
                    specs.add(new BundleItemSpec(trousers, "1 Pleated Chino Trousers", 1, BigDecimal.valueOf(900)));
                    specs.add(new BundleItemSpec(belt, "1 Classic Leather Belt", 1, BigDecimal.valueOf(500)));
                } else if (slug.contains("combo-10") || slug.contains("womens-premium")) {
                    specs.add(new BundleItemSpec(dress, "1 Silk Satin Midi Slip Dress", 1, BigDecimal.valueOf(2199)));
                    specs.add(new BundleItemSpec(handbag, "1 Italian Leather Shoulder Bag", 1, BigDecimal.valueOf(1300)));
                    specs.add(new BundleItemSpec(sneakers, "1 Pair Designer Heels/Shoes", 1, BigDecimal.valueOf(1000)));
                } else if (slug.contains("combo-11") || slug.contains("complete-look")) {
                    specs.add(new BundleItemSpec(jacket, "1 Vintage Denim Jacket", 1, BigDecimal.valueOf(1999)));
                    specs.add(new BundleItemSpec(shirt, "1 Oxford Shirt", 1, BigDecimal.valueOf(900)));
                    specs.add(new BundleItemSpec(jeans, "1 Raw Denim Jeans", 1, BigDecimal.valueOf(1300)));
                    specs.add(new BundleItemSpec(sneakers, "1 Pair Leather Sneakers", 1, BigDecimal.valueOf(1300)));
                } else if (slug.contains("combo-12") || slug.contains("luxury-ethnic")) {
                    specs.add(new BundleItemSpec(kurti, "1 Chanderi Silk Suit", 1, BigDecimal.valueOf(2599)));
                    specs.add(new BundleItemSpec(dupatta, "1 Zari Border Dupatta", 1, BigDecimal.valueOf(1400)));
                    specs.add(new BundleItemSpec(handbag, "1 Embellished Handbag", 1, BigDecimal.valueOf(2000)));
                } else if (slug.contains("combo-13") || slug.contains("wedding")) {
                    specs.add(new BundleItemSpec(blazer, "1 Silk Ceremonial Sherwani Jacket", 1, BigDecimal.valueOf(3299)));
                    specs.add(new BundleItemSpec(trousers, "1 Silk Blend Trousers", 1, BigDecimal.valueOf(1400)));
                    specs.add(new BundleItemSpec(dupatta, "1 Velvet Embellished Stole", 1, BigDecimal.valueOf(1300)));
                    specs.add(new BundleItemSpec(sneakers, "1 Pair Handcrafted Footwear", 1, BigDecimal.valueOf(1500)));
                }

                int order = 0;
                for (BundleItemSpec s : specs) {
                    if (s.product() != null) {
                        ComboItem ci = new ComboItem(s.product(), s.title(), s.qty(), s.itemPrice(), "M", "Default", order++);
                        ci.setComboPackage(cp);
                        comboItemRepo.save(ci);
                    }
                }
            }
        }
    }

    private record ColorVariantPreset(String colorName, String hexCode, String imageUrl) {}

    private record ProductVisualPreset(String sku, String primaryImg, List<String> galleryImgs, List<ColorVariantPreset> colors) {
        public ProductVisualPreset(String sku, String primaryImg, List<String> galleryImgs) {
            this(sku, primaryImg, galleryImgs, List.of());
        }
    }

    private record BundleItemSpec(Product product, String title, int qty, BigDecimal itemPrice) {
        public BundleItemSpec(Product product, String title, int qty) {
            this(product, title, qty, null);
        }
    }

    private void seedCombo(ComboPackageRepository repo, ComboItemRepository itemRepo,
                           String name, String slug, ComboTier tier, String desc, String img,
                           int price, int originalPrice, String badge, List<BundleItemSpec> items) {

        ComboPackage cp = new ComboPackage(name, slug, tier, desc, img,
                BigDecimal.valueOf(price), BigDecimal.valueOf(originalPrice), badge);
        cp.setActive(true);
        cp.setFeatured(true);

        ComboPackage saved = repo.save(cp);

        int order = 0;
        for (BundleItemSpec spec : items) {
            if (spec.product != null) {
                ComboItem ci = new ComboItem(spec.product, spec.title, spec.qty, spec.itemPrice, "M", "Default", order++);
                ci.setComboPackage(saved);
                itemRepo.save(ci);
            }
        }
    }

    private void createProduct(ProductRepository repo, ProductImageRepository imgRepo,
                               ProductSizeRepository sizeRepo, ProductColorRepository colRepo,
                               String name, String sku, String desc, int price, Integer discountPrice,
                               Category category, String primaryImg, List<String> additionalImgs,
                               List<String> sizes, List<String> colors, int stock, boolean featured) {

        Product p = new Product();
        p.setName(name);
        p.setSku(sku);
        p.setDescription(desc);
        p.setPrice(BigDecimal.valueOf(price));
        if (discountPrice != null) p.setDiscountPrice(BigDecimal.valueOf(discountPrice));
        p.setCategory(category);
        p.setCategoryName(category.getName());
        p.setImageUrl(primaryImg);
        p.setStock(stock);
        p.setFeatured(featured);
        p.setActive(true);

        Product saved = repo.save(p);

        imgRepo.save(new ProductImage(saved, primaryImg, name, true, 0));

        int order = 1;
        for (String url : additionalImgs) {
            imgRepo.save(new ProductImage(saved, url, name + " alternate view", false, order++));
        }

        for (String s : sizes) {
            sizeRepo.save(new ProductSize(saved, s, stock));
        }

        for (String c : colors) {
            colRepo.save(new ProductColor(saved, c, "#222222"));
        }
    }
}
