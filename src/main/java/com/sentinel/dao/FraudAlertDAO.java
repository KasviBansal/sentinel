package com.sentinel.dao;

import com.sentinel.model.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FraudAlertDAO {
    public List<FraudAlert> findAll(Connection connection, Map<Long, TransactionRow> transactions) throws SQLException {
        List<FraudAlert> alerts = new ArrayList<>();
        String sql = """
                SELECT a.* FROM fraud_alerts a JOIN transactions t ON t.transaction_id=a.transaction_id
                ORDER BY t.transaction_time DESC, a.alert_id DESC
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                TransactionRow transaction = transactions.get(rows.getLong("transaction_id"));
                if (transaction != null) {
                    alerts.add(new FraudAlert(rows.getLong("alert_id"), transaction, rows.getString("reason"),
                            rows.getInt("risk_score"), AlertStatus.valueOf(rows.getString("status")), rows.getString("review_note")));
                }
            }
        }
        return alerts;
    }

    public void insert(Connection connection, long transactionId, String reason, int score) throws SQLException {
        String sql = "INSERT INTO fraud_alerts(transaction_id,reason,risk_score,status) VALUES(?,?,?,?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, transactionId);
            statement.setString(2, reason);
            statement.setInt(3, score);
            statement.setString(4, AlertStatus.NEW.name());
            statement.executeUpdate();
        }
    }

    public void update(Connection connection, long id, AlertStatus status, String note) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("UPDATE fraud_alerts SET status=?,review_note=? WHERE alert_id=?")) {
            statement.setString(1, status.name());
            statement.setString(2, note);
            statement.setLong(3, id);
            statement.executeUpdate();
        }
    }
}
