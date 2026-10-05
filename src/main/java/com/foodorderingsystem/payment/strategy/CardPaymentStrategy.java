package com.foodorderingsystem.payment.strategy;

import org.springframework.stereotype.Component;

/** Card payment (simulated) - a small 2% gateway processing fee is added, like a real payment gateway would charge. */
@Component
public class CardPaymentStrategy implements PaymentStrategy {

    public static final String METHOD_CODE = "CARD";

    private static final double GATEWAY_FEE_RATE = 0.02; // 2%

    @Override
    public double calculateFee(double orderAmount) {
        return Math.round(orderAmount * GATEWAY_FEE_RATE * 100) / 100.0;
    }

    @Override
    public String getLabel() {
        return "Card Payment";
    }
}
