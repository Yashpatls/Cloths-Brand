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

            // 4. Seed Rich Product Catalog
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
                        "https://images.unsplash.com/photo-1529139574466-a303027c1d8b?w=900",
                        List.of(),
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
                        "https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=900",
                        List.of(),
                        List.of("S", "M", "L", "XL"),
                        List.of("Optic White", "Heather Grey", "Black"),
                        60, false);
            }

            // Ensure we have helper products for bundles
            Product shirt = productRepo.findBySku("SH-M-001").orElse(null);
            Product jacket = productRepo.findBySku("SH-M-002").orElse(null);
            Product trousers = productRepo.findBySku("SH-M-004").orElse(null);
            Product dress = productRepo.findBySku("SH-W-001").orElse(null);
            Product tshirt = productRepo.findBySku("SH-M-007").orElseGet(() -> {
                Product p = new Product();
                p.setName("Classic Crew Neck Cotton T-Shirt");
                p.setSku("SH-M-007");
                p.setDescription("Premium 100% combed cotton jersey t-shirt with ribbed crew neck. Essential everyday basic.");
                p.setPrice(BigDecimal.valueOf(499));
                p.setCategory(menCat);
                p.setCategoryName(menCat.getName());
                p.setImageUrl("https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=900");
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
                p.setImageUrl("https://images.unsplash.com/photo-1542272604-780c96856592?w=900");
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
                p.setImageUrl("https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=900");
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
                p.setImageUrl("https://images.unsplash.com/photo-1525966222134-fcfa99b8ae77?w=900");
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
                p.setImageUrl("https://images.unsplash.com/photo-1506619216599-9d16d0903dfd?w=900");
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
                        "https://images.openai.com/static-rsc-4/cW_Vp0ejCEu0gPRfgCd8GTRktsLrhUJJDoZErLi_MBL_bB6drClprhajaurFhVQRYVTNRSII9h8BjXDfQfKmx8T89-4KYvzLWBVeOKHxojo3iknWDCg8BzgZ8NmSf9jZgHmHSSX0dHpwyG8lFkaFaPuTJdCfj-MfEANGgnc8AfM?purpose=inline",
                        499, 998, "Save 50% (Bestseller)",
                        List.of(new BundleItemSpec(tshirt, "1 Classic Crew T-Shirt", 1),
                                new BundleItemSpec(tshirt, "1 Contrast Crew T-Shirt", 1)));

                seedCombo(comboRepo, comboItemRepo,
                        "Combo 2 – Daily Wear", "combo-2-daily-wear", ComboTier.BUDGET,
                        "Essential daily rotation featuring 1 button-down casual shirt and 1 breathable cotton tee.",
                        "https://images.openai.com/static-rsc-4/SXkm0-9HpJucAxICTZPrDtMrE7NGboAkZW4cYsb_ARuW6Qj6QEXSVcTrzHb-nNH1lgAn7RfWAh6bpb3ov6G9U5a-P0tK8mGf9-tzKDnCstLiyFnIOFOzEg38TTxgarw9IhvRF4Yoc1F4KPGhc9if7LN7AxDPUwo9XuJRTDMSsnQ?purpose=inline",
                        699, 1499, "Essential Value",
                        List.of(new BundleItemSpec(shirt, "1 Oxford Casual Shirt", 1),
                                new BundleItemSpec(tshirt, "1 Pima Cotton T-Shirt", 1)));

                seedCombo(comboRepo, comboItemRepo,
                        "Combo 3 – Women's Daily Wear", "combo-3-womens-daily-wear", ComboTier.BUDGET,
                        "Effortless daily set pairing a soft relaxed top with high-stretch comfort cotton leggings.",
                        "https://images.openai.com/static-rsc-4/rSJxM6HL4OFS3-ts7wi98quL2BGONI-VjLIe8Lp2YJYzdPNx4fe2ydLzW10tGR68_m2L3ZtGCNKy0Jz3wPdNxUq_TfqfQSp_ufIgaDiEuJKUG_OWWuPsPVX9Nl8NnwI08ZSDE41BkWKcI32hi9ihHaHLryMZkmExfSIQYQvOj80?purpose=inline",
                        799, 1599, "Popular Daily",
                        List.of(new BundleItemSpec(tshirt, "1 Relaxed Cotton Top", 1),
                                new BundleItemSpec(leggings, "1 Stretch Cotton Leggings", 1)));

                seedCombo(comboRepo, comboItemRepo,
                        "Combo 4 – Kids Wear", "combo-4-kids-wear", ComboTier.BUDGET,
                        "Comfort-first playwear pairing an organic cotton graphic tee with elasticated drawstring shorts.",
                        "https://images.openai.com/static-rsc-4/dWT7vK5vZbXGSlosOE5t7oT0wC_q-bfsskVnBQOZ3ftBFP1MbyS5cgyGX4wGTdqXZApU0moK-DKWn0-Dul0-6dKNEJpuvIpxmok48Qdl6oTYm8buqDD8G5x6T-yzfeBln5KspaqMBa_NgYYUiP8jmc1HIJlCh1uaibQuQ2pTLiw?purpose=inline",
                        599, 1098, "Kids Favourite",
                        List.of(new BundleItemSpec(tshirt, "1 Organic Cotton T-Shirt", 1),
                                new BundleItemSpec(kidsShorts, "1 Active Drawstring Shorts", 1)));

                // 2. Standard Combos (₹1,000 – ₹2,499)
                seedCombo(comboRepo, comboItemRepo,
                        "Combo 5 – Men's Casual", "combo-5-mens-casual", ComboTier.STANDARD,
                        "Classic weekend trio combining a crisp button-down shirt, dark indigo jeans, and a full-grain leather belt.",
                        "https://images.openai.com/static-rsc-4/mSWD5lE9rZ8BYLWwI56tUK5DFxfSot5Noqt71Xs7GxT0UduQXwrbVcRZyCZBDUO8ctIvSTpd0Ia3nppnJb2vDswDTzQePywOWvnSH9Gg5Nleb4XA9g28tKWHXzfvyzt4I_nCuIg2MD6J3N5i5onI-dGg8Z6C9_yp_-GG3V5QwDc?purpose=inline",
                        1499, 2999, "3-Piece Classic (Save 50%)",
                        List.of(new BundleItemSpec(shirt, "1 Oxford Cotton Shirt", 1, BigDecimal.valueOf(699)),
                                new BundleItemSpec(jeans, "1 Straight-Leg Denim Jeans", 1, BigDecimal.valueOf(500)),
                                new BundleItemSpec(belt, "1 Full-Grain Leather Belt", 1, BigDecimal.valueOf(300))));

                seedCombo(comboRepo, comboItemRepo,
                        "Combo 6 – Women's Ethnic", "combo-6-womens-ethnic", ComboTier.STANDARD,
                        "Graceful 3-piece ethnic set including a hand-block printed cotton kurti, ankle-length leggings, and gold-bordered chiffon dupatta.",
                        "https://images.openai.com/static-rsc-4/LpLZ8QP6I4Ee0R-DW22m1sNHnswYIfzhxYiAsmIhcUR1xPNiGiXAt9PmOVh2bu4h8uHNNtSWs6VKeIUXXT9pFADiTnobuu5nca_V8HSzvTJj2CczT8BOfsdyDOv5Q_4d2_KehYEj8uLyDC0bt8Taq0-LS-l6VAJmsiV48eZNx4E?purpose=inline",
                        1799, 3299, "Festive Ready",
                        List.of(new BundleItemSpec(kurti, "1 Hand-Block Printed Kurti", 1),
                                new BundleItemSpec(leggings, "1 Ankle Leggings", 1),
                                new BundleItemSpec(dupatta, "1 Pure Chiffon Dupatta", 1)));

                seedCombo(comboRepo, comboItemRepo,
                        "Combo 7 – Smart Casual", "combo-7-smart-casual", ComboTier.STANDARD,
                        "Sharp smart-casual ensemble: structured polo shirt, raw selvedge denim jeans, and pristine white low-top leather sneakers.",
                        "https://images.openai.com/static-rsc-4/JgOdzsuU0YLu1_L08nj5VCDRh4e5AFJjUdGfLMUiMt_GZbq2RAc1cYKewNlpxVNLeRH3ZbfxMzRW6yoJ_K3Gd7h_slVaN9xHLVBfptg74QkP3zoLb07hssKo-D4NhT4D59vlhdc7kvuFX4L2B_KwtA4JOu8cGiRXum5I1rAUhzg?purpose=inline",
                        2299, 4199, "Weekend Smart",
                        List.of(new BundleItemSpec(shirt, "1 Pique Cotton Polo", 1),
                                new BundleItemSpec(jeans, "1 Dark Indigo Jeans", 1),
                                new BundleItemSpec(sneakers, "1 Pair Minimal Leather Sneakers", 1)));

                seedCombo(comboRepo, comboItemRepo,
                        "Combo 8 – Fashion Pack", "combo-8-fashion-pack", ComboTier.STANDARD,
                        "Modern street style edit featuring a ribbed modal top, high-rise tailored jeans, and a premium leather crossbody handbag.",
                        "https://images.openai.com/static-rsc-4/CwP7vrET7FoXBh_ZWUG2rm-PT7tFMQloCx52NsmHaeWDHBTwYqxdVQjKXrUF3ayy1Kk1naT-rV2-8btZ8cjGgSIkuMv3MaQasf6s2QeKgq4s1dAAWXIAQ1j_2zEdCoiA3CIDZMu9-egKzavTFA-9L0y3_KXfJOmCjB_lr73nE6S1587aioSy-OdEuRzMeD_w?purpose=inline",
                        2199, 3999, "Editorial Pick",
                        List.of(new BundleItemSpec(tshirt, "1 Ribbed Modal Top", 1),
                                new BundleItemSpec(jeans, "1 Tapered Denim Jeans", 1),
                                new BundleItemSpec(handbag, "1 Leather Crossbody Bag", 1)));

                // 3. Premium Combos (₹2,500 – ₹5,999)
                seedCombo(comboRepo, comboItemRepo,
                        "Combo 9 – Men's Premium", "combo-9-mens-premium", ComboTier.PREMIUM,
                        "Executive 4-piece formal attire: wool-blend tailored blazer, crisp twill shirt, pleated trousers, and matching leather belt.",
                        "https://images.openai.com/static-rsc-4/z1TRYosBc_gfWbOf39AIsb8uX4Wuy9IpswbFnd23lE9RUUX557Aa21bXw4NbQU6qFBtom_HMoQHR7tyd825l4O6c5CkPOCGrZyRNr5q2Nmf84bBeLc1ae1G5_R78g9scOTPx81tTQfv9xNitSnK--5hYfJi53T2-Yn-BkF1QuEw?purpose=inline",
                        3999, 6999, "Executive 4-Piece",
                        List.of(new BundleItemSpec(blazer, "1 Tailored Wool-Blend Blazer", 1),
                                new BundleItemSpec(shirt, "1 Formal Twill Shirt", 1),
                                new BundleItemSpec(trousers, "1 Pleated Chino Trousers", 1),
                                new BundleItemSpec(belt, "1 Classic Leather Belt", 1)));

                seedCombo(comboRepo, comboItemRepo,
                        "Combo 10 – Women's Premium", "combo-10-womens-premium", ComboTier.PREMIUM,
                        "Evening cocktail glamour: Mulberry silk satin midi dress paired with an Italian leather bag and footwear.",
                        "https://images.openai.com/static-rsc-4/N1ut-DIT6jC-LTg14OjQshEqXSrngMB--jmpsqlZrDluQ5Xsb3du38nwrAkl_qaLEyTfpnhS2YBvp8LDkJvgwyH4IsV9VGjgfAFxBQKEcGMvUJJQluI1yvfpOxdlxibSkdqgQX6nnAbJJzUGJ3KGdM3MEHpZYEWYK9xOlbWg0jU?purpose=inline",
                        4499, 7999, "Cocktail Gala",
                        List.of(new BundleItemSpec(dress, "1 Silk Satin Midi Slip Dress", 1),
                                new BundleItemSpec(handbag, "1 Italian Leather Shoulder Bag", 1),
                                new BundleItemSpec(sneakers, "1 Pair Designer Heels/Shoes", 1)));

                seedCombo(comboRepo, comboItemRepo,
                        "Combo 11 – Complete Look", "combo-11-complete-look", ComboTier.PREMIUM,
                        "Urban winter statement: Japanese selvedge denim jacket, Oxford shirt, straight jeans, and clean sneakers.",
                        "https://images.openai.com/static-rsc-4/tyN8UGHu46nhXgYd4sobS5gnI-HB5yRh2Jel8-S_EbFT-O7q5jYh0d25oFHsYS1Sc6grCw4eWtl7CaYxktfSkbzz5pz3H_kmmdVZCWr4lwwLh89ofa9kT-fiuIj6F5eIJF5fPrQSBz2S-io913fe0yjEwPiIrlN2VOK86U1SH_U?purpose=inline",
                        5499, 8999, "Urban Luxe 4-Piece",
                        List.of(new BundleItemSpec(jacket, "1 Vintage Denim Jacket", 1),
                                new BundleItemSpec(shirt, "1 Oxford Shirt", 1),
                                new BundleItemSpec(jeans, "1 Raw Denim Jeans", 1),
                                new BundleItemSpec(sneakers, "1 Pair Leather Sneakers", 1)));

                seedCombo(comboRepo, comboItemRepo,
                        "Combo 12 – Luxury Ethnic", "combo-12-luxury-ethnic", ComboTier.PREMIUM,
                        "Royal wedding festive suite: hand-embroidered Chanderi silk suit, zari dupatta, and embellished evening handbag.",
                        "https://images.openai.com/static-rsc-4/UIZ8VAlQqjoLjyVbHArqL3K-BJfW4HInnbZM-wbnZS7IY-Po_qh6FwuG5jnJKar3ITYSm_S3pF8M3rkXFDnLulMhtHs91l21UnY1_FPv8DLPqQgP7BVccZLEwZGbVE8c0upd5HUVxPnput50Ac8LiMzdYs-uk1ZiYNhUPwglFjc?purpose=inline",
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

            log.info("StyleHub database seeding complete. Active products: {}, Categories: {}, Combos: {}",
                    productRepo.count(), categoryRepo.count(), comboRepo.count());
        };
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
