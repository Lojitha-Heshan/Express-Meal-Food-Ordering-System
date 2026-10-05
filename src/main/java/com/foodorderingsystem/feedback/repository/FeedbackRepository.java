package com.foodorderingsystem.feedback.repository;

import com.foodorderingsystem.feedback.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    List<Feedback> findAllByOrderByCreatedAtDesc();
    List<Feedback> findByCustomer_CustomerIdOrderByCreatedAtDesc(Long customerId);

    Optional<Feedback> findByOrder_OrderId(Long orderId);
    List<Feedback> findByOrder_OrderIdIn(List<Long> orderIds);
}
