package com.foodorderingsystem.payment.strategy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Factory pattern: hides the "which PaymentStrategy class do I need" decision
 * behind one method, so PaymentService just asks for a strategy by method code
 * instead of instantiating CashOnDeliveryStrategy/CardPaymentStrategy itself.
 */
@Component
public class PaymentStrategyFactory {

    @Autowired
    private CashOnDeliveryStrategy cashOnDeliveryStrategy;

    @Autowired
    private CardPaymentStrategy cardPaymentStrategy;

    public PaymentStrategy getStrategy(String methodCode) {
        if (CardPaymentStrategy.METHOD_CODE.equals(methodCode)) {
            return cardPaymentStrategy;
        }
        // default to Cash on Delivery for CASH_ON_DELIVERY or any unrecognized value
        return cashOnDeliveryStrategy;
    }
}
