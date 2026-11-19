package com.sentinel.dao;

import com.sentinel.model.RuleConfig;
import com.sentinel.model.RuleType;
import java.sql.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class FraudRuleDAO {
    public List<RuleConfig> findAll(Connection connection) throws SQLException {
        List<RuleConfig> rules = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM fraud_rules");
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                rules.add(new RuleConfig(RuleType.valueOf(rows.getString("rule_id")), rows.getLong("threshold_value"),
                        rows.getBoolean("enabled"), rows.getInt("risk_weight")));
            }
        }
        rules.sort(Comparator.comparing(RuleConfig::type));
        return rules;
    }

    public void insertDefaults(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO fraud_rules VALUES(?,?,?,?,?)")) {
            for (RuleType type : RuleType.values()) {
                statement.setString(1, type.name());
                statement.setString(2, type.label);
                statement.setLong(3, type.defaultThreshold);
                statement.setBoolean(4, true);
                statement.setInt(5, type.weight);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    public void update(Connection connection, RuleConfig rule) throws SQLException {
        String sql = "UPDATE fraud_rules SET enabled=?, threshold_value=? WHERE rule_id=?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBoolean(1, rule.enabled());
            statement.setLong(2, rule.threshold());
            statement.setString(3, rule.type().name());
            statement.executeUpdate();
        }
    }
}
