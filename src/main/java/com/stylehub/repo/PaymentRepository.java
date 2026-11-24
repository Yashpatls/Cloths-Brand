package com.stylehub.repo;

import com.stylehub.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByOrderIdOrderByPaymentDateDesc(Long orderId);
    Optional<Payment> findByTransactionId(String transactionId);
    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);
    List<Payment> findAllByOrderByPaymentDateDesc();
}
