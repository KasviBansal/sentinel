package com.sentinel.model;

import com.sentinel.util.Formats;
import java.math.BigDecimal;

public record RuleConfig(RuleType type, long threshold, boolean enabled, int weight) {
    public String displayThreshold() {
        return switch (type) {
            case HIGH_AMOUNT -> Formats.money(BigDecimal.valueOf(threshold));
            case FREQUENCY -> threshold + " transfers / 10 min";
            case BLACKLIST -> "Any listed account";
            case UNUSUAL_TIME -> "01:00 to 05:00 IST";
        };
    }
}
