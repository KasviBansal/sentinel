package com.sentinel;

import com.sentinel.model.*;
import com.sentinel.rules.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FraudRulesTest {
    private Transaction transfer(long paise, int hour, int minute) {
        return new Transaction(0, "A", "B", paise, TransferType.UPI,
                LocalDate.of(2026, 11, 18).atTime(LocalTime.of(hour, minute)), null);
    }

    @Test void highAmountUsesStrictThreshold() {
        FraudRule rule = new HighAmountRule(new BigDecimal("50000"));
        assertFalse(rule.check(transfer(1_000_000, 12, 0)));
        assertFalse(rule.check(transfer(5_000_000, 12, 0)));
        assertTrue(rule.check(transfer(5_000_001, 12, 0)));
    }

    @Test void frequencyCountsTheCurrentTransferOnce() {
        Transaction transfer = transfer(1000, 12, 0);
        assertFalse(new FrequentTransactionRule(4, 5).check(transfer));
        assertTrue(new FrequentTransactionRule(5, 5).check(transfer));
        assertFalse(new FrequentTransactionRule(5, 6).check(transfer));
    }

    @Test void blacklistChecksEitherAccount() {
        Transaction transfer = transfer(1000, 12, 0);
        assertTrue(new BlacklistRule(Set.of("A")).check(transfer));
        assertTrue(new BlacklistRule(Set.of("B")).check(transfer));
        assertFalse(new BlacklistRule(Set.of("C")).check(transfer));
    }

    @Test void nighttimeIncludesOneButExcludesFive() {
        FraudRule rule = new UnusualTimeRule();
        assertFalse(rule.check(transfer(1000, 0, 59)));
        assertTrue(rule.check(transfer(1000, 1, 0)));
        assertTrue(rule.check(transfer(1000, 4, 59)));
        assertFalse(rule.check(transfer(1000, 5, 0)));
    }

    @Test void riskBandsUseSpecifiedBoundaries() {
        assertEquals(RiskLevel.Low, RiskLevel.fromScore(29));
        assertEquals(RiskLevel.Medium, RiskLevel.fromScore(30));
        assertEquals(RiskLevel.Medium, RiskLevel.fromScore(59));
        assertEquals(RiskLevel.High, RiskLevel.fromScore(60));
        assertEquals(RiskLevel.High, RiskLevel.fromScore(79));
        assertEquals(RiskLevel.Critical, RiskLevel.fromScore(80));
    }
}
