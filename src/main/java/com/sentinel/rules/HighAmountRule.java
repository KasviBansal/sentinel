package com.sentinel.rules;

import com.sentinel.model.Transaction;
import java.math.BigDecimal;

public class HighAmountRule implements FraudRule {
    private final BigDecimal threshold;

    public HighAmountRule(BigDecimal threshold) { this.threshold = threshold; }

    @Override public boolean check(Transaction transaction) {
        return transaction.amount().compareTo(threshold) > 0;
    }
}
