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

    private String status = "PENDING"; // PENDING, SUCCESS, FAILED

    private LocalDateTime paidAt = LocalDateTime.now();
}
