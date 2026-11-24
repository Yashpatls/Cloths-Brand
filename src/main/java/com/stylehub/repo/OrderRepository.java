package com.stylehub.repo;

import com.stylehub.model.CustomerOrder;
import com.stylehub.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {
    List<CustomerOrder> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<CustomerOrder> findAllByOrderByCreatedAtDesc();

    List<CustomerOrder> findByStatusOrderByCreatedAtDesc(OrderStatus status);

    Optional<CustomerOrder> findByOrderNumber(String orderNumber);

    long countByStatus(OrderStatus status);

    @Query("SELECT COALESCE(SUM(o.total), 0) FROM CustomerOrder o WHERE o.status != 'CANCELLED'")
    BigDecimal sumTotalRevenue();
}
