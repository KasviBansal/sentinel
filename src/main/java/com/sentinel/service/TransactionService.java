package com.sentinel.service;

import com.sentinel.dao.*;
import com.sentinel.database.Database;
import com.sentinel.exception.InvalidTransactionException;
import com.sentinel.model.*;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class TransactionService {
    private final Database database;
    private final TransactionDAO transactions;
    private final CustomerDAO customers;
    private final FraudAlertDAO alerts;
    private final FraudDetectionService detector;

    public TransactionService(Database database, TransactionDAO transactions, CustomerDAO customers,
                              FraudAlertDAO alerts, FraudDetectionService detector) {
        this.database = database;
        this.transactions = transactions;
        this.customers = customers;
        this.alerts = alerts;
        this.detector = detector;
    }

    public List<TransactionRow> findAll() { return database.read(transactions::findAll); }

    public Transaction create(String sender, String receiver, BigDecimal amount, TransferType type, LocalDateTime time)
            throws InvalidTransactionException {
        if (sender == null || sender.isBlank() || receiver == null || receiver.isBlank()
                || amount == null || type == null || time == null) {
            throw new InvalidTransactionException("Complete all transaction fields.");
        }
        if (sender.equals(receiver)) throw new InvalidTransactionException("Choose two different accounts.");
        if (amount.signum() <= 0) throw new InvalidTransactionException("Enter an amount greater than zero.");
        long paise;
        try {
            paise = amount.movePointRight(2).longValueExact();
        } catch (ArithmeticException error) {
            throw new InvalidTransactionException("Use at most two decimal places and an amount within the supported range.");
        }
        if (time.getYear() < 1 || time.getYear() > 9999) {
            throw new InvalidTransactionException("Enter a date with a year from 0001 to 9999.");
        }
        return database.write(connection -> {
            if (!customers.isActiveAccount(connection, sender) || !customers.isActiveAccount(connection, receiver)) {
                throw new InvalidTransactionException("Both accounts must belong to active customers.");
            }
            return evaluateAndSave(connection, new Transaction(0, sender, receiver, paise, type, time.withNano(0), null));
        });
    }

    public Transaction evaluateAndSave(Connection connection, Transaction draft) throws SQLException {
        Evaluation evaluation = detector.evaluate(connection, draft);
        long id = transactions.insert(connection, draft, evaluation);
        if (evaluation.suspicious()) {
            String reason = String.join(" ", evaluation.matches().stream().map(RuleMatch::reason).toList());
            alerts.insert(connection, id, reason, evaluation.score());
        }
        return new Transaction(id, draft.sender(), draft.receiver(), draft.amountPaise(), draft.type(), draft.time(), evaluation);
    }
}
