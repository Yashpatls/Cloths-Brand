package com.stylehub.controller;

import com.stylehub.model.*;
import com.stylehub.repo.PaymentRepository;
import com.stylehub.repo.ProductRepository;
import com.stylehub.repo.StoreSettingRepository;
import com.stylehub.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final OrderService orderService;
    private final UserService userService;
    private final CouponService couponService;
    private final ReportService reportService;
    private final StorageService storageService;
    private final PaymentRepository paymentRepo;
    private final StoreSettingRepository settingRepo;
    private final ProductRepository productRepo;
    private final ComboService comboService;

    public AdminController(ProductService productService, CategoryService categoryService,
                           OrderService orderService, UserService userService,
                           CouponService couponService, ReportService reportService,
                           StorageService storageService, PaymentRepository paymentRepo,
                           StoreSettingRepository settingRepo, ProductRepository productRepo,
                           ComboService comboService) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.orderService = orderService;
        this.userService = userService;
        this.couponService = couponService;
        this.reportService = reportService;
        this.storageService = storageService;
        this.paymentRepo = paymentRepo;
        this.settingRepo = settingRepo;
        this.productRepo = productRepo;
        this.comboService = comboService;
    }

    // 1. Dashboard
    @GetMapping
    public String dashboard(Model model) {
        model.addAllAttributes(reportService.getDashboardKPIs());
        model.addAttribute("recentOrders", orderService.getAllOrders().stream().limit(8).toList());
        model.addAttribute("lowStockItems", productService.getLowStockProducts(5));
        return "admin/dashboard";
    }

    // 2. Products
    @GetMapping("/products")
    public String products(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String q,
            Model model) {
        model.addAttribute("products", productService.filter(category, q, null, null));
        model.addAttribute("categories", categoryService.getAllActive());
        return "admin/products";
    }

    @GetMapping("/products/new")
    public String newProduct(Model model) {
        model.addAttribute("product", new Product());
        model.addAttribute("categories", categoryService.getAllActive());
        return "admin/product-form";
    }

    @GetMapping("/products/{id}/edit")
    public String editProduct(@PathVariable Long id, Model model) {
        Product p = productService.getById(id);
        model.addAttribute("product", p);
        model.addAttribute("categories", categoryService.getAllActive());
        return "admin/product-form";
    }

    @PostMapping("/products/save")
    public String saveProduct(
            @ModelAttribute Product product,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String sizesInput,
            @RequestParam(required = false) String colorsInput,
            @RequestParam(required = false) MultipartFile imageFile,
            RedirectAttributes redirectAttributes) {

        try {
            // Upload to S3/local storage if a new image file was provided
            if (imageFile != null && !imageFile.isEmpty()) {
                String uploadedUrl = storageService.store(imageFile);
                if (uploadedUrl != null) {
                    product.setImageUrl(uploadedUrl);
                }
            }

            List<String> sizes = (sizesInput != null && !sizesInput.isBlank())
                    ? Arrays.asList(sizesInput.split(",")) : null;
            List<String> colors = (colorsInput != null && !colorsInput.isBlank())
                    ? Arrays.asList(colorsInput.split(",")) : null;

            productService.save(product, categoryId, sizes, colors);
            redirectAttributes.addFlashAttribute("successMessage", "Product saved successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/products";
    }

    @PostMapping("/products/{id}/delete")
    public String deleteProduct(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        productService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Product archived.");
        return "redirect:/admin/products";
    }

    // 3. Categories
    @GetMapping("/categories")
    public String categories(Model model) {
        model.addAttribute("categories", categoryService.getAll());
        model.addAttribute("newCategory", new Category());
        return "admin/categories";
    }

    @PostMapping("/categories/save")
    public String saveCategory(
            @ModelAttribute Category category,
            @RequestParam(required = false) MultipartFile imageFile,
            RedirectAttributes redirectAttributes) {

        if (imageFile != null && !imageFile.isEmpty()) {
            String uploadedUrl = storageService.store(imageFile);
            if (uploadedUrl != null) category.setImageUrl(uploadedUrl);
        }
        categoryService.save(category);
        redirectAttributes.addFlashAttribute("successMessage", "Category updated.");
        return "redirect:/admin/categories";
    }

    @PostMapping("/categories/{id}/delete")
    public String deleteCategory(@PathVariable Long id) {
        categoryService.delete(id);
        return "redirect:/admin/categories";
    }

    // 4. Inventory
    @GetMapping("/inventory")
    public String inventory(Model model) {
        model.addAttribute("products", productService.getAllActive());
        return "admin/inventory";
    }

    @PostMapping("/inventory/update")
    public String updateStock(@RequestParam Long productId, @RequestParam int stock, RedirectAttributes redirectAttributes) {
        Product p = productService.getById(productId);
        p.setStock(Math.max(0, stock));
        productRepo.save(p);
        redirectAttributes.addFlashAttribute("successMessage", "Stock updated for " + p.getName());
        return "redirect:/admin/inventory";
    }

    // 5. Orders
    @GetMapping("/orders")
    public String orders(@RequestParam(required = false) OrderStatus status, Model model) {
        if (status != null) {
            model.addAttribute("orders", orderService.getOrdersByStatus(status));
            model.addAttribute("selectedStatus", status);
        } else {
            model.addAttribute("orders", orderService.getAllOrders());
        }
        model.addAttribute("orderStatuses", OrderStatus.values());
        return "admin/orders";
    }

    @GetMapping("/orders/{id}")
    public String adminOrderDetail(@PathVariable Long id, Model model) {
        model.addAttribute("order", orderService.getById(id));
        model.addAttribute("orderStatuses", OrderStatus.values());
        return "admin/order-detail";
    }

    @PostMapping("/orders/{id}/status")
    public String updateOrderStatus(@PathVariable Long id, @RequestParam OrderStatus status, RedirectAttributes redirectAttributes) {
        orderService.updateOrderStatus(id, status);
        redirectAttributes.addFlashAttribute("successMessage", "Order #" + id + " status updated to " + status);
        return "redirect:/admin/orders";
    }

    // 6. Customers
    @GetMapping("/customers")
    public String customers(Model model) {
        model.addAttribute("customers", userService.getAllCustomers());
        return "admin/customers";
    }

    // 7. Payments
    @GetMapping("/payments")
    public String payments(Model model) {
        model.addAttribute("payments", paymentRepo.findAllByOrderByPaymentDateDesc());
        return "admin/payments";
    }

    // 8. Coupons
    @GetMapping("/coupons")
    public String coupons(Model model) {
        model.addAttribute("coupons", couponService.getAll());
        model.addAttribute("newCoupon", new Coupon());
        return "admin/coupons";
    }

    @PostMapping("/coupons/save")
    public String saveCoupon(@ModelAttribute Coupon coupon, RedirectAttributes redirectAttributes) {
        couponService.save(coupon);
        redirectAttributes.addFlashAttribute("successMessage", "Coupon saved successfully.");
        return "redirect:/admin/coupons";
    }

    @PostMapping("/coupons/{id}/delete")
    public String deleteCoupon(@PathVariable Long id) {
        couponService.delete(id);
        return "redirect:/admin/coupons";
    }

    // 9. Reports
    @GetMapping("/reports")
    public String reports(Model model) {
        model.addAllAttributes(reportService.getDashboardKPIs());
        model.addAttribute("orders", orderService.getAllOrders().stream().limit(25).toList());
        return "admin/reports";
    }

    // 10. Settings
    @GetMapping("/settings")
    public String settings(Model model) {
        model.addAttribute("settings", settingRepo.findAll());
        return "admin/settings";
    }

    @PostMapping("/settings/save")
    public String saveSetting(@RequestParam String key, @RequestParam String value, @RequestParam(required = false) String description, RedirectAttributes redirectAttributes) {
        StoreSetting s = settingRepo.findBySettingKey(key).orElse(new StoreSetting());
        s.setSettingKey(key);
        s.setSettingValue(value);
        if (description != null) s.setDescription(description);
        settingRepo.save(s);
        redirectAttributes.addFlashAttribute("successMessage", "Store configuration updated.");
        return "redirect:/admin/settings";
    }

    // 11. Combo Packages Management
    @GetMapping("/combos")
    public String combos(Model model) {
        List<ComboPackage> allCombos = comboService.getAllCombos();
        model.addAttribute("combos", allCombos);
        model.addAttribute("tiers", ComboTier.values());
        model.addAttribute("totalCombos", allCombos.size());
        model.addAttribute("totalRevenue", allCombos.stream().map(ComboPackage::getTotalRevenue).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));
        model.addAttribute("totalSalesCount", allCombos.stream().mapToInt(ComboPackage::getSalesCount).sum());
        return "admin/combos";
    }

    @GetMapping("/combos/new")
    public String newCombo(Model model) {
        model.addAttribute("combo", new ComboPackage());
        model.addAttribute("tiers", ComboTier.values());
        model.addAttribute("products", productService.getAllActive());
        return "admin/combo-form";
    }

    @GetMapping("/combos/{id}/edit")
    public String editCombo(@PathVariable Long id, Model model) {
        ComboPackage combo = comboService.getById(id);
        model.addAttribute("combo", combo);
        model.addAttribute("tiers", ComboTier.values());
        model.addAttribute("products", productService.getAllActive());
        return "admin/combo-form";
    }

    @PostMapping("/combos/save")
    public String saveCombo(
            @ModelAttribute ComboPackage combo,
            @RequestParam(required = false) List<Long> productIds,
            @RequestParam(required = false) List<Integer> quantities,
            @RequestParam(required = false) List<String> customTitles,
            @RequestParam(required = false) MultipartFile imageFile,
            RedirectAttributes redirectAttributes) {

        try {
            if (imageFile != null && !imageFile.isEmpty()) {
                String uploadedUrl = storageService.store(imageFile);
                if (uploadedUrl != null) {
                    combo.setImageUrl(uploadedUrl);
                }
            }

            comboService.save(combo, productIds, quantities, customTitles);
            redirectAttributes.addFlashAttribute("successMessage", "Combo package saved successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error saving combo package: " + e.getMessage());
        }

        return "redirect:/admin/combos";
    }

    @PostMapping("/combos/{id}/toggle")
    public String toggleCombo(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        comboService.toggleActive(id);
        redirectAttributes.addFlashAttribute("successMessage", "Combo package status updated.");
        return "redirect:/admin/combos";
    }

    @PostMapping("/combos/{id}/delete")
    public String deleteCombo(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        comboService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Combo package deleted.");
        return "redirect:/admin/combos";
    }
}
