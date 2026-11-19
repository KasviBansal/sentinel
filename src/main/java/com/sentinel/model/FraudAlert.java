package com.sentinel.model;

public record FraudAlert(long id, TransactionRow row, String reason, int score, AlertStatus status, String note) {
    public String displayId() { return "ALT-%04d".formatted(id); }
}
