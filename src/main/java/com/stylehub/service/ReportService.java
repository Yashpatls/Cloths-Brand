package com.stylehub.service;

import com.stylehub.model.OrderStatus;
import com.stylehub.repo.OrderRepository;
import com.stylehub.repo.ProductRepository;
import com.stylehub.repo.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class ReportService {

    private final OrderRepository orderRepo;
    private final ProductRepository productRepo;
    private final UserRepository userRepo;

    public ReportService(OrderRepository orderRepo, ProductRepository productRepo, UserRepository userRepo) {
        this.orderRepo = orderRepo;
        this.productRepo = productRepo;
        this.userRepo = userRepo;
    }

    public Map<String, Object> getDashboardKPIs() {
        Map<String, Object> kpis = new HashMap<>();
        BigDecimal revenue = orderRepo.sumTotalRevenue();
        long totalOrders = orderRepo.count();
        long pendingOrders = orderRepo.countByStatus(OrderStatus.PLACED) + orderRepo.countByStatus(OrderStatus.PROCESSING);
        long totalProducts = productRepo.countByActiveTrue();
        long totalCustomers = userRepo.count();
        long lowStockCount = productRepo.findLowStockProducts(5).size();

        kpis.put("totalRevenue", revenue != null ? revenue : BigDecimal.ZERO);
        kpis.put("totalOrders", totalOrders);
        kpis.put("pendingOrders", pendingOrders);
        kpis.put("totalProducts", totalProducts);
        kpis.put("totalCustomers", totalCustomers);
        kpis.put("lowStockCount", lowStockCount);
        return kpis;
    }
}
