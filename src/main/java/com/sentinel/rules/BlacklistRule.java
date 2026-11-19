package com.sentinel.rules;

import com.sentinel.model.Transaction;
import java.util.Set;

public class BlacklistRule implements FraudRule {
    private final Set<String> accounts;

    public BlacklistRule(Set<String> accounts) { this.accounts = accounts; }

    @Override public boolean check(Transaction transaction) {
        return accounts.contains(transaction.sender()) || accounts.contains(transaction.receiver());
    }
}
