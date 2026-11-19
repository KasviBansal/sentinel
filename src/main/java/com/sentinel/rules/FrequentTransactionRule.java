package com.sentinel.rules;

import com.sentinel.model.Transaction;

public class FrequentTransactionRule implements FraudRule {
    private final int previousCount;
    private final long limit;

    public FrequentTransactionRule(int previousCount, long limit) {
        this.previousCount = previousCount;
        this.limit = limit;
    }

    @Override public boolean check(Transaction transaction) {
        return previousCount + 1L > limit;
    }
}
