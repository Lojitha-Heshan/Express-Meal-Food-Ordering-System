package com.foodorderingsystem.payment.service;

import com.foodorderingsystem.delivery.service.DeliveryService;
import com.foodorderingsystem.order.entity.Order;
import com.foodorderingsystem.order.service.OrderService;
import com.foodorderingsystem.payment.entity.Payment;
import com.foodorderingsystem.payment.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private DeliveryService deliveryService;

    // Simulated payment processing (no real gateway)
    public Payment processPayment(Long orderId, String method) {
        Order order = orderService.getById(orderId);

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(order.getTotalAmount());
        payment.setMethod(method);
        payment.setStatus("SUCCESS"); // simulated - always succeeds

        Payment saved = paymentRepository.save(payment);

        orderService.updatePaymentStatus(orderId, "PAID");
        orderService.updateStatus(orderId, "CONFIRMED");

        // Create the delivery record now that payment is confirmed,
        // so it shows up in the admin's Delivery list ready to be assigned to a rider.
        deliveryService.createDeliveryForOrder(order);

        return saved;
    }

    public Payment getByOrderId(Long orderId) {
        return paymentRepository.findByOrder_OrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Payment not found for order: " + orderId));
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    public double getTotalRevenue() {
        return paymentRepository.findByStatus("SUCCESS").stream()
                .mapToDouble(Payment::getAmount)
                .sum();
    }
}
