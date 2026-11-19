package com.sentinel.dao;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;

public class BlacklistDAO {
    public Set<String> findAll(Connection connection) throws SQLException {
        Set<String> accounts = new HashSet<>();
        try (PreparedStatement statement = connection.prepareStatement("SELECT account_number FROM blacklist");
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) accounts.add(rows.getString(1));
        }
        return accounts;
    }

    public void insert(Connection connection, String account, String reason) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO blacklist(account_number,reason) VALUES(?,?)")) {
            statement.setString(1, account);
            statement.setString(2, reason);
            statement.executeUpdate();
        }
    }
}
