package com.sentinel.dao;

import java.sql.*;

public class UserDAO {
    public record Credentials(byte[] hash, byte[] salt) { }

    public Credentials findByEmail(Connection connection, String email) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT password_hash,password_salt FROM users WHERE email=?")) {
            statement.setString(1, email);
            try (ResultSet row = statement.executeQuery()) {
                return row.next() ? new Credentials(row.getBytes(1), row.getBytes(2)) : null;
            }
        }
    }

    public void insert(Connection connection, String email, Credentials credentials) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO users(email,password_hash,password_salt) VALUES(?,?,?)")) {
            statement.setString(1, email);
            statement.setBytes(2, credentials.hash());
            statement.setBytes(3, credentials.salt());
            statement.executeUpdate();
        }
    }
}
