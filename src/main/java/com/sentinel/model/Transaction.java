package com.sentinel.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Transaction(long id, String sender, String receiver, long amountPaise,
                          TransferType type, LocalDateTime time, Evaluation evaluation) {
    public BigDecimal amount() { return BigDecimal.valueOf(amountPaise, 2); }
    public String displayId() { return "TXN-%05d".formatted(id); }
}
