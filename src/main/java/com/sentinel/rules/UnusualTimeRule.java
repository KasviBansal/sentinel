package com.sentinel.rules;

import com.sentinel.model.Transaction;
import java.time.LocalTime;

public class UnusualTimeRule implements FraudRule {
    @Override public boolean check(Transaction transaction) {
        LocalTime time = transaction.time().toLocalTime();
        return !time.isBefore(LocalTime.of(1, 0)) && time.isBefore(LocalTime.of(5, 0));
    }
}
