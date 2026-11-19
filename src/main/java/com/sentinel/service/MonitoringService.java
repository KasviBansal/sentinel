package com.sentinel.service;

import com.sentinel.dao.*;
import com.sentinel.database.Database;
import com.sentinel.model.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

public class MonitoringService {
    public record Snapshot(List<TransactionRow> transactions, List<FraudAlert> alerts, BigDecimal totalValue,
                           long suspicious, long highRisk, Map<LocalDate, Integer> daily,
                           Map<TransferType, Integer> types, Map<RuleType, Integer> ruleCounts) { }

    private final Database database;
    private final TransactionDAO transactions;
    private final FraudAlertDAO alerts;
    private final FraudRuleDAO rules;

    public MonitoringService(Database database, TransactionDAO transactions, FraudAlertDAO alerts, FraudRuleDAO rules) {
        this.database = database;
        this.transactions = transactions;
        this.alerts = alerts;
        this.rules = rules;
    }

    public Snapshot snapshot(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("The start date must be on or before the end date.");
        }
        return database.read(connection -> {
            List<TransactionRow> selected = new ArrayList<>();
            Map<Long, TransactionRow> indexed = new HashMap<>();
            Map<LocalDate, Integer> daily = new TreeMap<>();
            Map<TransferType, Integer> types = new EnumMap<>(TransferType.class);
            Map<RuleType, Integer> counts = new EnumMap<>(RuleType.class);
            BigDecimal value = BigDecimal.ZERO;
            long suspicious = 0;
            long highRisk = 0;
            for (TransactionRow row : transactions.findAll(connection)) {
                Transaction transfer = row.transaction();
                LocalDate date = transfer.time().toLocalDate();
                if ((from != null && date.isBefore(from)) || (to != null && date.isAfter(to))) continue;
                selected.add(row);
                indexed.put(transfer.id(), row);
                value = value.add(transfer.amount());
                if (transfer.evaluation().suspicious()) suspicious++;
                if (transfer.evaluation().score() >= 60) highRisk++;
                daily.merge(date, 1, Integer::sum);
                types.merge(transfer.type(), 1, Integer::sum);
                for (RuleMatch match : transfer.evaluation().matches()) counts.merge(match.type(), 1, Integer::sum);
            }
            return new Snapshot(selected, alerts.findAll(connection, indexed), value, suspicious, highRisk, daily, types, counts);
        });
    }

    public void review(long id, AlertStatus status, String note) {
        if (status == null) throw new IllegalArgumentException("Choose a review status.");
        if (note.length() > 2000) throw new IllegalArgumentException("Use 2,000 characters or fewer for the review note.");
        database.write(connection -> { alerts.update(connection, id, status, note.strip()); return null; });
    }

    public List<RuleConfig> rules() { return database.read(rules::findAll); }

    public void saveRule(RuleConfig updated) {
        if (updated.threshold() <= 0 && (updated.type() == RuleType.HIGH_AMOUNT || updated.type() == RuleType.FREQUENCY)) {
            throw new IllegalArgumentException("Enter a positive whole-number threshold.");
        }
        database.write(connection -> { rules.update(connection, updated); return null; });
    }
}
