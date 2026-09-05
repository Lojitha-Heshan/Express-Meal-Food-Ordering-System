package com.foodorderingsystem.payment.service;

import com.foodorderingsystem.delivery.service.DeliveryService;
import com.foodorderingsystem.order.entity.Order;
import com.foodorderingsystem.order.service.OrderService;
import com.foodorderingsystem.payment.entity.Payment;
import com.foodorderingsystem.payment.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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

    // Revenue grouped by month (e.g. "Jan 2026"), sorted chronologically - used for the admin dashboard chart
    public Map<String, Double> getMonthlyRevenue() {
        DateTimeFormatter keyFmt = DateTimeFormatter.ofPattern("yyyy-MM");
        DateTimeFormatter labelFmt = DateTimeFormatter.ofPattern("MMM yyyy");

        Map<String, Double> sortedByKey = new TreeMap<>();
        for (Payment payment : paymentRepository.findByStatus("SUCCESS")) {
            String key = payment.getPaidAt().format(keyFmt);
            sortedByKey.merge(key, payment.getAmount(), Double::sum);
        }

        Map<String, Double> result = new LinkedHashMap<>();
        sortedByKey.forEach((key, total) -> {
            String label = java.time.YearMonth.parse(key).format(labelFmt);
            result.put(label, total);
        });
        return result;
    }
}
