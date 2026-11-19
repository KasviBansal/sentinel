package com.sentinel.dao;

import com.sentinel.model.*;
import com.sentinel.util.Formats;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class TransactionDAO {
    private static final String QUERY = """
            SELECT t.*, sender.name AS sender_name, receiver.name AS receiver_name
            FROM transactions t
            JOIN accounts sa ON sa.account_number=t.sender_account
            JOIN accounts ra ON ra.account_number=t.receiver_account
            JOIN customers sender ON sender.customer_id=sa.customer_id
            JOIN customers receiver ON receiver.customer_id=ra.customer_id
            ORDER BY t.transaction_time DESC, t.transaction_id DESC
            """;

    public List<TransactionRow> findAll(Connection connection) throws SQLException {
        Map<Long, List<RuleMatch>> matches = findMatches(connection);
        List<TransactionRow> transactions = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(QUERY);
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                long id = rows.getLong("transaction_id");
                Evaluation evaluation = new Evaluation(rows.getInt("risk_score"),
                        RiskLevel.valueOf(rows.getString("risk_level")), matches.getOrDefault(id, List.of()));
                Transaction transaction = new Transaction(id, rows.getString("sender_account"),
                        rows.getString("receiver_account"), rows.getLong("amount_paise"),
                        TransferType.valueOf(rows.getString("transaction_type")),
                        LocalDateTime.parse(rows.getString("transaction_time"), Formats.SQL_TIME), evaluation);
                transactions.add(new TransactionRow(transaction, rows.getString("sender_name"), rows.getString("receiver_name")));
            }
        }
        return transactions;
    }

    private Map<Long, List<RuleMatch>> findMatches(Connection connection) throws SQLException {
        Map<Long, List<RuleMatch>> matches = new HashMap<>();
        String sql = "SELECT transaction_id,rule_id,reason FROM transaction_rule_matches ORDER BY rowid";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                matches.computeIfAbsent(rows.getLong(1), id -> new ArrayList<>())
                        .add(new RuleMatch(RuleType.valueOf(rows.getString(2)), rows.getString(3)));
            }
        }
        return matches;
    }

    public long insert(Connection connection, Transaction transaction, Evaluation evaluation) throws SQLException {
        String sql = """
                INSERT INTO transactions(sender_account,receiver_account,amount_paise,transaction_type,
                    transaction_time,risk_score,risk_level,status) VALUES(?,?,?,?,?,?,?,?) RETURNING transaction_id
                """;
        long id;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, transaction.sender());
            statement.setString(2, transaction.receiver());
            statement.setLong(3, transaction.amountPaise());
            statement.setString(4, transaction.type().name());
            statement.setString(5, transaction.time().format(Formats.SQL_TIME));
            statement.setInt(6, evaluation.score());
            statement.setString(7, evaluation.level().name());
            statement.setString(8, evaluation.status());
            try (ResultSet row = statement.executeQuery()) { row.next(); id = row.getLong(1); }
        }
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO transaction_rule_matches VALUES(?,?,?)")) {
            for (RuleMatch match : evaluation.matches()) {
                statement.setLong(1, id);
                statement.setString(2, match.type().name());
                statement.setString(3, match.reason());
                statement.addBatch();
            }
            statement.executeBatch();
        }
        return id;
    }

    public int countOutgoing(Connection connection, String sender, LocalDateTime time) throws SQLException {
        String sql = """
                SELECT COUNT(*) FROM transactions WHERE sender_account=? AND transaction_time BETWEEN ? AND ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, sender);
            statement.setString(2, time.minusMinutes(10).format(Formats.SQL_TIME));
            statement.setString(3, time.format(Formats.SQL_TIME));
            try (ResultSet row = statement.executeQuery()) { row.next(); return row.getInt(1); }
        }
    }
}
