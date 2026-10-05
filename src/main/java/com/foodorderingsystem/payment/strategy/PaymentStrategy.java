package com.foodorderingsystem.payment.strategy;

/**
 * Strategy pattern: each payment method (Cash on Delivery, Card, ...) has its own
 * rule for calculating any processing fee and for describing itself on the invoice.
 * PaymentService doesn't need to know HOW each method behaves - it just asks
 * whichever strategy PaymentStrategyFactory hands it.
 */
public interface PaymentStrategy {

    /**
     * Any extra fee charged on top of the order total for using this payment method.
     * Returns 0 when the method has no extra charge.
     */
    double calculateFee(double orderAmount);

    /** Human-readable label shown on the invoice/receipt. */
    String getLabel();
}
