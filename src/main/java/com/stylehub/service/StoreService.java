package com.stylehub.service;

import com.stylehub.dto.RegisterDto;
import com.stylehub.model.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Facade store service preserving backward compatibility with starter controllers while delegating
 * to dedicated modular domain services.
 */
@Service
public class StoreService {

    public record CartLine(Product product, int quantity, String size, String color) {}

    private final UserService userService;
    private final ProductService productService;
    private final OrderService orderService;
    private final CategoryService categoryService;

    public StoreService(UserService userService, ProductService productService,
                        OrderService orderService, CategoryService categoryService) {
        this.userService = userService;
        this.productService = productService;
        this.orderService = orderService;
        this.categoryService = categoryService;
    }

    public void register(String name, String email, String password) {
        RegisterDto dto = new RegisterDto();
        dto.setName(name);
        dto.setEmail(email);
        dto.setPassword(password);
        userService.register(dto);
    }

    public User user(String email) {
        return userService.getByEmail(email);
    }

    public void updateProfile(String email, String phone, String address) {
        userService.updateProfile(email, null, phone, address);
    }

    public List<Product> products(String q, String category) {
        return productService.filter(category, q, null, null);
    }

    public Product product(Long id) {
        return productService.getById(id);
    }

    public Product save(Product p) {
        return productService.save(p, p.getCategory() != null ? p.getCategory().getId() : null, null, null);
    }

    public void deleteProduct(Long id) {
        productService.delete(id);
    }

    public List<CustomerOrder> ordersFor(String email) {
        return orderService.getCustomerOrders(email);
    }

    public List<CustomerOrder> allOrders() {
        return orderService.getAllOrders();
    }

    public void updateStatus(Long id, OrderStatus status) {
        orderService.updateOrderStatus(id, status);
    }

    public long customerCount() {
        return userService.getCustomerCount();
    }

    public long productCount() {
        return productService.getProductCount();
    }

    public long orderCount() {
        return (long) orderService.getAllOrders().size();
    }
}
