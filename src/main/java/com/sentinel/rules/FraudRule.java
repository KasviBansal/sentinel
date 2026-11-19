package com.sentinel.rules;

import com.sentinel.model.Transaction;

public interface FraudRule {
    boolean check(Transaction transaction);
}
