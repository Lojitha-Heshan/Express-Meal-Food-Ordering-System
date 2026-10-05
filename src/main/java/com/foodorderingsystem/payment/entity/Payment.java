package com.foodorderingsystem.payment.entity;

import com.foodorderingsystem.order.entity.Order;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;

    @OneToOne
    @JoinColumn(name = "order_id")
    private Order order;

    private Double amount;

    private String method; // CARD, CASH_ON_DELIVERY

    // extra fee charged for the chosen payment method (e.g. a card gateway fee) -
    // decided by PaymentStrategy, see payment.strategy package
    private Double processingFee = 0.0;

    // flat delivery fee charged on every order, regardless of payment method
    private Double deliveryFee = 0.0;

    private String status = "PENDING"; // PENDING, SUCCESS, FAILED

    private LocalDateTime paidAt = LocalDateTime.now();
}
