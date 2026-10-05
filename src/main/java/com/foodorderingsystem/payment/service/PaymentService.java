package com.foodorderingsystem.payment.service;

import com.foodorderingsystem.delivery.service.DeliveryService;
import com.foodorderingsystem.order.entity.Order;
import com.foodorderingsystem.order.service.OrderService;
import com.foodorderingsystem.payment.entity.Payment;
import com.foodorderingsystem.payment.repository.PaymentRepository;
import com.foodorderingsystem.payment.strategy.PaymentStrategy;
import com.foodorderingsystem.payment.strategy.PaymentStrategyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class PaymentService {

    // flat delivery fee added to every order's total, regardless of payment method -
    // exposed so the checkout page can show the full total before paying
    public static final double DELIVERY_FEE = 250.0;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private DeliveryService deliveryService;

    @Autowired
    private PaymentStrategyFactory paymentStrategyFactory;

    // Simulated payment processing (no real gateway)
    public Payment processPayment(Long orderId, String method) {
        Order order = orderService.getById(orderId);

        // Strategy pattern: the fee rule for this payment method is decided by
        // whichever PaymentStrategy the factory hands back - PaymentService
        // doesn't need an if/else on the method string to know the fee.
        PaymentStrategy strategy = paymentStrategyFactory.getStrategy(method);
        double fee = strategy.calculateFee(order.getTotalAmount());

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(order.getTotalAmount());
        payment.setMethod(method);
        payment.setProcessingFee(fee);
        payment.setDeliveryFee(DELIVERY_FEE);
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

    // Filter successful payments between two dates (inclusive) - both optional
    public List<Payment> getPaymentsBetween(LocalDate startDate, LocalDate endDate) {
        List<Payment> payments = paymentRepository.findByStatus("SUCCESS");
        if (startDate != null) {
            LocalDateTime start = startDate.atStartOfDay();
            payments = payments.stream().filter(p -> !p.getPaidAt().isBefore(start)).toList();
        }
        if (endDate != null) {
            LocalDateTime end = endDate.plusDays(1).atStartOfDay();
            payments = payments.stream().filter(p -> p.getPaidAt().isBefore(end)).toList();
        }
        return payments;
    }

    public double getTotalRevenue() {
        return paymentRepository.findByStatus("SUCCESS").stream()
                .mapToDouble(Payment::getAmount)
                .sum();
    }

    // Breaks total revenue down by what it's actually made of - order subtotal, the flat
    // delivery fee, and the 2% card processing fee - and separately by payment method
    // (Cash on Delivery vs Card), so the admin dashboard doesn't lump everything into one number.
    public Map<String, Object> getRevenueBreakdown() {
        return computeRevenueBreakdown(paymentRepository.findByStatus("SUCCESS"));
    }

    // Same breakdown, but over a given list of payments - used by the (date-filtered) sales report.
    public Map<String, Object> computeRevenueBreakdown(List<Payment> successful) {
        double orderSubtotal = successful.stream().mapToDouble(Payment::getAmount).sum();
        double deliveryFeeTotal = successful.stream()
                .mapToDouble(p -> p.getDeliveryFee() != null ? p.getDeliveryFee() : 0.0)
                .sum();
        double cardFeeTotal = successful.stream()
                .mapToDouble(p -> p.getProcessingFee() != null ? p.getProcessingFee() : 0.0)
                .sum();
        double grandTotal = orderSubtotal + deliveryFeeTotal + cardFeeTotal;

        List<Payment> codPayments = successful.stream()
                .filter(p -> "CASH_ON_DELIVERY".equals(p.getMethod()))
                .toList();
        List<Payment> cardPayments = successful.stream()
                .filter(p -> "CARD".equals(p.getMethod()))
                .toList();

        double codTotal = codPayments.stream()
                .mapToDouble(p -> p.getAmount() + (p.getDeliveryFee() != null ? p.getDeliveryFee() : 0.0))
                .sum();
        double cardTotal = cardPayments.stream()
                .mapToDouble(p -> p.getAmount() + (p.getDeliveryFee() != null ? p.getDeliveryFee() : 0.0)
                        + (p.getProcessingFee() != null ? p.getProcessingFee() : 0.0))
                .sum();

        Map<String, Object> breakdown = new LinkedHashMap<>();
        breakdown.put("orderSubtotal", orderSubtotal);
        breakdown.put("deliveryFeeTotal", deliveryFeeTotal);
        breakdown.put("cardFeeTotal", cardFeeTotal);
        breakdown.put("grandTotal", grandTotal);
        breakdown.put("codCount", codPayments.size());
        breakdown.put("codTotal", codTotal);
        breakdown.put("cardCount", cardPayments.size());
        breakdown.put("cardTotal", cardTotal);
        return breakdown;
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
