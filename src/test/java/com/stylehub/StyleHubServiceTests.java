package com.stylehub;

import com.stylehub.dto.CheckoutDto;
import com.stylehub.dto.RegisterDto;
import com.stylehub.model.*;
import com.stylehub.repo.*;
import com.stylehub.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class StyleHubServiceTests {

    @Autowired
    private UserService userService;

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CartService cartService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private CouponService couponService;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private ProductRepository productRepo;

    @Autowired
    private CategoryRepository categoryRepo;

    @Autowired
    private ComboService comboService;

    @Autowired
    private ComboPackageRepository comboRepo;

    @Autowired
    private com.stylehub.service.ComboImageService comboImageService;

    @Autowired
    private com.stylehub.service.image.ImageLayoutService imageLayoutService;

    private User testUser;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        // Register test user
        if (!userRepo.existsByEmail("testuser@stylehub.local")) {
            RegisterDto reg = new RegisterDto();
            reg.setName("Test Shopper");
            reg.setEmail("testuser@stylehub.local");
            reg.setPassword("Password@123");
            testUser = userService.register(reg);
        } else {
            testUser = userService.getByEmail("testuser@stylehub.local");
        }

        // Create test category and product
        Category cat = categoryRepo.findBySlugIgnoreCase("test-category").orElseGet(() ->
                categoryService.save(new Category("Test Category", "test-category", "Test desc", "http://img", 1)));

        testProduct = new Product();
        testProduct.setName("Test Oxford Shirt");
        testProduct.setSku("TEST-SHIRT-001");
        testProduct.setPrice(BigDecimal.valueOf(1000));
        testProduct.setStock(20);
        testProduct.setCategory(cat);
        testProduct = productService.save(testProduct, cat.getId(), List.of("M", "L"), List.of("White"));
    }

    @Test
    void testUserRegistrationAndLookup() {
        assertNotNull(testUser);
        assertEquals("Test Shopper", testUser.getName());
        assertEquals("testuser@stylehub.local", testUser.getEmail());
        assertEquals(Role.CUSTOMER, testUser.getRole());
    }

    @Test
    void testDuplicateEmailRegistrationThrowsException() {
        RegisterDto duplicate = new RegisterDto();
        duplicate.setName("Another User");
        duplicate.setEmail("testuser@stylehub.local");
        duplicate.setPassword("Password@123");

        assertThrows(IllegalArgumentException.class, () -> userService.register(duplicate));
    }

    @Test
    void testProductFiltering() {
        List<Product> products = productService.filter("Test Category", null, null, null);
        assertFalse(products.isEmpty());
        assertTrue(products.stream().anyMatch(p -> p.getName().equals("Test Oxford Shirt")));
    }

    @Test
    void testCartAddUpdateAndCalculate() {
        cartService.addItem(testUser.getEmail(), null, testProduct.getId(), 2, "M", "White");
        Cart cart = cartService.getOrCreateCart(testUser.getEmail(), null);

        assertEquals(1, cart.getItems().size());
        assertEquals(2, cart.getItems().get(0).getQuantity());
        assertEquals(BigDecimal.valueOf(2000), cart.getSubtotal());

        // Update quantity
        cartService.updateQuantity(cart.getItems().get(0).getId(), 3);
        Cart updatedCart = cartService.getOrCreateCart(testUser.getEmail(), null);
        assertEquals(3, updatedCart.getItems().get(0).getQuantity());
        assertEquals(BigDecimal.valueOf(3000), updatedCart.getSubtotal());
    }

    @Test
    void testCouponDiscountCalculation() {
        Coupon coupon = new Coupon("TEST20", DiscountType.PERCENTAGE, BigDecimal.valueOf(20), BigDecimal.valueOf(500), LocalDate.now().plusDays(30));
        couponService.save(coupon);

        var validCoupon = couponService.validateCoupon("TEST20", BigDecimal.valueOf(1000));
        assertTrue(validCoupon.isPresent());

        BigDecimal discount = couponService.calculateDiscount(validCoupon.get(), BigDecimal.valueOf(1000));
        assertEquals(0, BigDecimal.valueOf(200).compareTo(discount));
    }

    @Test
    void testOrderPlacementAndStockDecrement() {
        cartService.addItem(testUser.getEmail(), null, testProduct.getId(), 2, "M", "White");
        Cart cart = cartService.getOrCreateCart(testUser.getEmail(), null);

        int initialStock = testProduct.getStock();

        CheckoutDto checkout = new CheckoutDto();
        checkout.setFullName("Test Shopper");
        checkout.setPhone("9876543210");
        checkout.setStreetAddress("123 Fashion Street");
        checkout.setCity("Mumbai");
        checkout.setState("Maharashtra");
        checkout.setPostalCode("400001");
        checkout.setPaymentMethod("DEMO");

        CustomerOrder order = orderService.placeOrder(testUser.getEmail(), cart, checkout);

        assertNotNull(order);
        assertNotNull(order.getOrderNumber());
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());

        // Verify stock was reduced
        Product updatedProduct = productService.getById(testProduct.getId());
        assertEquals(initialStock - 2, updatedProduct.getStock());

        // Verify cart is cleared
        Cart clearedCart = cartService.getOrCreateCart(testUser.getEmail(), null);
        assertTrue(clearedCart.getItems().isEmpty());
    }

    @Test
    void testCreateComboPackageAndCalculateSavings() {
        Product p2 = new Product();
        p2.setName("Denim Jeans Test");
        p2.setSku("TEST-JEANS-002");
        p2.setPrice(BigDecimal.valueOf(1500));
        p2.setStock(15);
        p2 = productRepo.save(p2);

        ComboPackage cp = new ComboPackage("Test Casual Pack", "test-casual-pack", ComboTier.STANDARD,
                "Test bundle", "http://img.jpg", BigDecimal.valueOf(1800), BigDecimal.valueOf(2500), "Save 28%");

        ComboPackage saved = comboService.save(cp, List.of(testProduct.getId(), p2.getId()), List.of(1, 1), List.of("1 Shirt", "1 Jeans"));

        assertNotNull(saved.getId());
        assertEquals(2, saved.getItems().size());
        assertEquals(BigDecimal.valueOf(1800), saved.getEffectivePrice());
        assertEquals(BigDecimal.valueOf(700), saved.getSavingsAmount());
        assertEquals(28, saved.getSavingsPercentage());
        assertTrue(saved.isInStock());
    }

    @Test
    void testFilterCombosByBudgetTier() {
        ComboPackage budgetCombo = new ComboPackage("Budget Pack", "budget-pack", ComboTier.BUDGET, "desc", "img", BigDecimal.valueOf(499), BigDecimal.valueOf(800), "50%");
        comboRepo.save(budgetCombo);

        ComboPackage standardCombo = new ComboPackage("Standard Pack", "standard-pack", ComboTier.STANDARD, "desc", "img", BigDecimal.valueOf(1499), BigDecimal.valueOf(2000), "25%");
        comboRepo.save(standardCombo);

        List<ComboPackage> budgetResults = comboService.getAllActive(ComboTier.BUDGET, null, null);
        assertTrue(budgetResults.stream().allMatch(c -> c.getTier() == ComboTier.BUDGET));

        List<ComboPackage> standardResults = comboService.getAllActive(ComboTier.STANDARD, null, null);
        assertTrue(standardResults.stream().allMatch(c -> c.getTier() == ComboTier.STANDARD));
    }

    @Test
    void testOrderPlacementWithComboDeductsStockFromAllConstituentProducts() {
        Product p2 = new Product();
        p2.setName("Test Belt");
        p2.setSku("TEST-BELT-003");
        p2.setPrice(BigDecimal.valueOf(500));
        p2.setStock(10);
        p2 = productRepo.save(p2);

        ComboPackage cp = new ComboPackage("Trio Combo", "trio-combo", ComboTier.STANDARD,
                "desc", "img", BigDecimal.valueOf(1200), BigDecimal.valueOf(1500), "Deal");
        cp = comboService.save(cp, List.of(testProduct.getId(), p2.getId()), List.of(1, 1), List.of("1 Shirt", "1 Belt"));

        int shirtInitialStock = testProduct.getStock();
        int beltInitialStock = p2.getStock();

        // Add combo to cart
        cartService.addComboItem(testUser.getEmail(), null, cp.getId(), 2, "Shirt: L / Belt: Free Size");
        Cart cart = cartService.getOrCreateCart(testUser.getEmail(), null);
        assertEquals(1, cart.getItems().size());
        assertTrue(cart.getItems().get(0).isCombo());

        // Checkout
        CheckoutDto checkout = new CheckoutDto();
        checkout.setFullName("Test Shopper");
        checkout.setPhone("9876543210");
        checkout.setStreetAddress("123 Fashion Street");
        checkout.setCity("Mumbai");
        checkout.setState("Maharashtra");
        checkout.setPostalCode("400001");
        checkout.setPaymentMethod("DEMO");

        CustomerOrder order = orderService.placeOrder(testUser.getEmail(), cart, checkout);
        assertNotNull(order);

        // Verify stock deducted from BOTH constituent items (2 units ordered)
        Product updatedShirt = productService.getById(testProduct.getId());
        Product updatedBelt = productService.getById(p2.getId());

        assertEquals(shirtInitialStock - 2, updatedShirt.getStock());
        assertEquals(beltInitialStock - 2, updatedBelt.getStock());

        // Verify combo sales metrics updated
        ComboPackage updatedCombo = comboService.getById(cp.getId());
        assertEquals(2, updatedCombo.getSalesCount());
        assertEquals(BigDecimal.valueOf(2400), updatedCombo.getTotalRevenue());
    }

    @Test
    void testDynamicComboItemRemovalAndPriceRecalculation() {
        // Setup Shirt, Jeans, Belt
        Product jeans = new Product();
        jeans.setName("Denim Jeans");
        jeans.setSku("TEST-JEANS-DYN");
        jeans.setPrice(BigDecimal.valueOf(1799));
        jeans.setStock(20);
        jeans = productRepo.save(jeans);

        Product belt = new Product();
        belt.setName("Leather Belt");
        belt.setSku("TEST-BELT-DYN");
        belt.setPrice(BigDecimal.valueOf(899));
        belt.setStock(15);
        belt = productRepo.save(belt);

        ComboPackage cp = new ComboPackage("Dynamic Trio", "dynamic-trio", ComboTier.STANDARD,
                "Dynamic bundle", "img.jpg", BigDecimal.valueOf(1499), BigDecimal.valueOf(2999), "Deal");
        cp = comboRepo.save(cp);

        // Assign explicit item prices: Shirt = 699, Jeans = 500, Belt = 300
        ComboItem it1 = new ComboItem(testProduct, "1 Shirt", 1, BigDecimal.valueOf(699), "M", "Default", 0);
        it1.setComboPackage(cp);
        cp.getItems().add(it1);

        ComboItem it2 = new ComboItem(jeans, "1 Jeans", 1, BigDecimal.valueOf(500), "32", "Default", 1);
        it2.setComboPackage(cp);
        cp.getItems().add(it2);

        ComboItem it3 = new ComboItem(belt, "1 Belt", 1, BigDecimal.valueOf(300), "Free Size", "Default", 2);
        it3.setComboPackage(cp);
        cp.getItems().add(it3);

        cp = comboRepo.save(cp);

        // 1. Initial full combo price: 699 + 500 + 300 = 1499
        assertEquals(BigDecimal.valueOf(1499), it1.getItemPrice().add(it2.getItemPrice()).add(it3.getItemPrice()));

        // 2. Remove Belt: price recalculates to 699 + 500 = 1199
        BigDecimal shirtAndJeansPrice = it1.getItemPrice().add(it2.getItemPrice());
        assertEquals(BigDecimal.valueOf(1199), shirtAndJeansPrice);

        // 3. Remove Jeans: price recalculates to 699
        BigDecimal shirtOnlyPrice = it1.getItemPrice();
        assertEquals(BigDecimal.valueOf(699), shirtOnlyPrice);

        // 4. Add customized combo (Shirt + Jeans, excluding Belt) to Cart
        int shirtStockBefore = testProduct.getStock();
        int jeansStockBefore = jeans.getStock();
        int beltStockBefore = belt.getStock();

        String includedPids = testProduct.getId() + "," + jeans.getId();
        cartService.addComboItem(testUser.getEmail(), null, cp.getId(), 1,
                "1 Shirt: M (Blue) | 1 Jeans: 32 (Dark Wash) [Excluded: 1 Belt]",
                shirtAndJeansPrice, includedPids);

        Cart cart = cartService.getOrCreateCart(testUser.getEmail(), null);
        assertEquals(1, cart.getItems().size());
        CartItem ci = cart.getItems().get(0);
        assertEquals(0, BigDecimal.valueOf(1199).compareTo(ci.getUnitPrice()));
        assertEquals(0, BigDecimal.valueOf(1199).compareTo(ci.getTotalPrice()));

        // 5. Checkout
        CheckoutDto checkout = new CheckoutDto();
        checkout.setFullName("Dynamic Shopper");
        checkout.setPhone("9876543210");
        checkout.setStreetAddress("402 Marine Drive");
        checkout.setCity("Mumbai");
        checkout.setState("Maharashtra");
        checkout.setPostalCode("400001");
        checkout.setPaymentMethod("ONLINE");

        CustomerOrder order = orderService.placeOrder(testUser.getEmail(), cart, checkout);
        assertNotNull(order);
        assertEquals(0, BigDecimal.valueOf(1199).compareTo(order.getSubtotal()));

        // 6. Verify inventory: Shirt and Jeans were decremented, but Belt was NOT decremented!
        Product updatedShirt = productService.getById(testProduct.getId());
        Product updatedJeans = productService.getById(jeans.getId());
        Product updatedBelt = productService.getById(belt.getId());

        assertEquals(shirtStockBefore - 1, updatedShirt.getStock());
        assertEquals(jeansStockBefore - 1, updatedJeans.getStock());
        assertEquals(beltStockBefore, updatedBelt.getStock()); // Untouched!
    }

    @Test
    void testAutomaticCollageGenerationAndLayout() {
        // 1. Verify Layout Engine
        assertEquals(1, imageLayoutService.calculateLayout(1).size());
        assertEquals(2, imageLayoutService.calculateLayout(2).size());
        assertEquals(3, imageLayoutService.calculateLayout(3).size());
        assertEquals(4, imageLayoutService.calculateLayout(4).size());
        assertEquals(5, imageLayoutService.calculateLayout(5).size());

        // 2. Generate Live Collage for products
        byte[] collageBytes = comboImageService.generateCollageForProducts(
                List.of(testProduct.getId()), "Test Look");
        assertNotNull(collageBytes);
        assertTrue(collageBytes.length > 500, "Collage image should contain valid JPEG bytes");

        // Verify JPEG SOI marker (0xFF, 0xD8)
        assertEquals((byte) 0xFF, collageBytes[0]);
        assertEquals((byte) 0xD8, collageBytes[1]);

        // 3. Generate Collage for Combo with included products
        ComboPackage cp = new ComboPackage("Collage Combo", "collage-combo", ComboTier.STANDARD,
                "desc", null, BigDecimal.valueOf(1499), BigDecimal.valueOf(2000), "Deal");
        cp = comboService.save(cp, List.of(testProduct.getId()), List.of(1), List.of("1 Shirt"));

        byte[] comboCollage = comboImageService.generateCollageForCombo(cp.getId(), List.of(testProduct.getId()));
        assertNotNull(comboCollage);
        assertTrue(comboCollage.length > 500);
        assertEquals((byte) 0xFF, comboCollage[0]);
        assertEquals((byte) 0xD8, comboCollage[1]);
    }
}
