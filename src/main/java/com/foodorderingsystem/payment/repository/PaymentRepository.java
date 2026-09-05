package com.foodorderingsystem.payment.repository;

import com.foodorderingsystem.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrder_OrderId(Long orderId);
    List<Payment> findByStatus(String status);
}
