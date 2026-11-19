package com.sentinel.service;

import com.sentinel.dao.*;
import com.sentinel.model.*;
import com.sentinel.rules.*;
import com.sentinel.util.Formats;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;

public class FraudDetectionService {
    private final FraudRuleDAO rules;
    private final TransactionDAO transactions;
    private final BlacklistDAO blacklist;

    public FraudDetectionService(FraudRuleDAO rules, TransactionDAO transactions, BlacklistDAO blacklist) {
        this.rules = rules;
        this.transactions = transactions;
        this.blacklist = blacklist;
    }

    public Evaluation evaluate(Connection connection, Transaction transaction) throws SQLException {
        int score = 0;
        ArrayList<RuleMatch> matches = new ArrayList<>();
        for (RuleConfig config : rules.findAll(connection)) {
            if (!config.enabled()) continue;
            FraudRule rule;
            String reason;
            switch (config.type()) {
                case HIGH_AMOUNT -> {
                    BigDecimal threshold = BigDecimal.valueOf(config.threshold());
                    rule = new HighAmountRule(threshold);
                    reason = "Transaction amount " + Formats.money(transaction.amount())
                            + " exceeds the configured " + Formats.money(threshold) + " threshold.";
                }
                case FREQUENCY -> {
                    int previous = transactions.countOutgoing(connection, transaction.sender(), transaction.time());
                    rule = new FrequentTransactionRule(previous, config.threshold());
                    reason = (previous + 1L) + " outgoing transfers in ten minutes exceeds the limit of " + config.threshold() + ".";
                }
                case BLACKLIST -> {
                    var accounts = blacklist.findAll(connection);
                    rule = new BlacklistRule(accounts);
                    boolean sender = accounts.contains(transaction.sender());
                    boolean receiver = accounts.contains(transaction.receiver());
                    String parties = sender && receiver ? "Both accounts are" : sender ? "The sender is" : "The receiver is";
                    reason = parties + " on the account blacklist.";
                }
                case UNUSUAL_TIME -> {
                    rule = new UnusualTimeRule();
                    reason = "The transfer occurred within the 01:00 to 05:00 IST monitoring window.";
                }
                default -> throw new IllegalStateException("Unknown fraud rule");
            }
            if (rule.check(transaction)) {
                score += config.weight();
                matches.add(new RuleMatch(config.type(), reason));
            }
        }
        score = Math.min(100, score);
        return new Evaluation(score, RiskLevel.fromScore(score), matches);
    }
}
