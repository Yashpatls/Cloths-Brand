package com.stylehub.controller;

import com.stylehub.model.Category;
import com.stylehub.model.Product;
import com.stylehub.service.CategoryService;
import com.stylehub.service.ProductService;
import com.stylehub.service.ReviewService;
import com.stylehub.service.WishlistService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;

@Controller
public class ProductController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final ReviewService reviewService;
    private final WishlistService wishlistService;

    public ProductController(ProductService productService, CategoryService categoryService,
                             ReviewService reviewService, WishlistService wishlistService) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.reviewService = reviewService;
        this.wishlistService = wishlistService;
    }

    @GetMapping({"/shop", "/products"})
    public String shop(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            Model model) {

        List<Product> products = productService.filter(category, q, minPrice, maxPrice);
        model.addAttribute("products", products);
        model.addAttribute("categories", categoryService.getAllActive());
        model.addAttribute("selectedCategory", category);
        model.addAttribute("query", q);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        return "products";
    }

    @GetMapping("/category/{slug}")
    public String collection(@PathVariable String slug, Model model) {
        Category cat = categoryService.getBySlug(slug);
        List<Product> products = productService.getByCategory(slug);
        model.addAttribute("category", cat);
        model.addAttribute("products", products);
        model.addAttribute("categories", categoryService.getAllActive());
        model.addAttribute("selectedCategory", cat.getName());
        return "products";
    }

    @GetMapping("/products/{id}")
    public String productDetail(@PathVariable String id, Authentication auth, Model model) {
        if (id == null || !id.matches("\\d+")) {
            return "redirect:/shop";
        }
        Long productId = Long.parseLong(id);
        Product product = productService.getById(productId);
        if (product == null) {
            return "redirect:/shop";
        }
        boolean isWishlisted = false;
        if (auth != null && auth.isAuthenticated()) {
            isWishlisted = wishlistService.isWishlisted(auth.getName(), productId);
        }

        List<Product> related = productService.getByCategory(product.getCategoryName()).stream()
                .filter(p -> !p.getId().equals(productId))
                .limit(4)
                .toList();

        model.addAttribute("product", product);
        model.addAttribute("reviews", reviewService.getApprovedReviews(productId));
        model.addAttribute("isWishlisted", isWishlisted);
        model.addAttribute("relatedProducts", related);
        return "product";
    }

    @GetMapping("/search")
    public String search(@RequestParam(required = false, defaultValue = "") String q, Model model) {
        List<Product> results = productService.filter(null, q, null, null);
        model.addAttribute("products", results);
        model.addAttribute("query", q);
        model.addAttribute("categories", categoryService.getAllActive());
        return "products";
    }
}
