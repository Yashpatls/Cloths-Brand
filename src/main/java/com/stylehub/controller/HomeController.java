package com.stylehub.controller;

import com.stylehub.model.Product;
import com.stylehub.service.CategoryService;
import com.stylehub.service.ComboService;
import com.stylehub.service.ProductService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class HomeController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final ComboService comboService;

    public HomeController(ProductService productService, CategoryService categoryService, ComboService comboService) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.comboService = comboService;
    }

    @GetMapping("/")
    public String home(Model model) {
        List<Product> featured = productService.getFeatured();
        if (featured.isEmpty()) {
            featured = productService.getAllActive().stream().limit(8).toList();
        }
        model.addAttribute("featuredProducts", featured);
        model.addAttribute("allProducts", productService.getAllActive());
        model.addAttribute("categories", categoryService.getAllActive());
        model.addAttribute("newArrivals", productService.getAllActive().stream().limit(4).toList());
        model.addAttribute("featuredCombos", comboService.getAllActive(null, null, null).stream().limit(4).toList());
        return "index";
    }

    @GetMapping("/account")
    public String account(Authentication auth) {
        if (auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return "redirect:/admin";
        }
        return "redirect:/profile";
    }
}
