package com.sentinel.dao;

import com.sentinel.model.Account;
import com.sentinel.model.Customer;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CustomerDAO {
    public List<Customer> findAll(Connection connection) throws SQLException {
        List<Customer> customers = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM customers ORDER BY name");
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                customers.add(new Customer(rows.getLong("customer_id"), rows.getString("name"),
                        rows.getString("email"), rows.getString("phone"), rows.getString("account_number"),
                        rows.getString("city"), rows.getBoolean("active")));
            }
        }
        return customers;
    }

    public List<Account> accounts(Connection connection) throws SQLException {
        List<Account> accounts = new ArrayList<>();
        String sql = """
                SELECT a.account_number, c.name, c.active FROM accounts a
                JOIN customers c ON c.customer_id=a.customer_id ORDER BY c.name, a.account_number
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) accounts.add(new Account(rows.getString(1), rows.getString(2), rows.getBoolean(3)));
        }
        return accounts;
    }

    public boolean isActiveAccount(Connection connection, String number) throws SQLException {
        String sql = """
                SELECT c.active FROM accounts a JOIN customers c ON a.customer_id=c.customer_id
                WHERE a.account_number=?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, number);
            try (ResultSet row = statement.executeQuery()) {
                return row.next() && row.getBoolean(1);
            }
        }
    }

    public Set<String> accountNumbers(Connection connection, long customerId) throws SQLException {
        Set<String> numbers = new HashSet<>();
        try (PreparedStatement statement = connection.prepareStatement("SELECT account_number FROM accounts WHERE customer_id=?")) {
            statement.setLong(1, customerId);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) numbers.add(rows.getString(1));
            }
        }
        return numbers;
    }

    public boolean exists(Connection connection, Customer customer) throws SQLException {
        String sql = """
                SELECT 1 FROM customers WHERE email=? AND customer_id<>?
                UNION ALL SELECT 1 FROM accounts WHERE account_number=? AND customer_id<>?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, customer.email());
            statement.setLong(2, customer.id());
            statement.setString(3, customer.account());
            statement.setLong(4, customer.id());
            try (ResultSet row = statement.executeQuery()) { return row.next(); }
        }
    }

    public long insert(Connection connection, Customer customer) throws SQLException {
        String sql = "INSERT INTO customers(name,email,phone,account_number,city,active) VALUES(?,?,?,?,?,?) RETURNING customer_id";
        long id;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, customer.name());
            statement.setString(2, customer.email());
            statement.setString(3, customer.phone());
            statement.setString(4, customer.account());
            statement.setString(5, customer.city());
            statement.setBoolean(6, customer.active());
            try (ResultSet row = statement.executeQuery()) { row.next(); id = row.getLong(1); }
        }
        insertAccount(connection, id, customer.account());
        return id;
    }

    public void insertAccount(Connection connection, long id, String number) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO accounts VALUES(?,?)")) {
            statement.setString(1, number);
            statement.setLong(2, id);
            statement.executeUpdate();
        }
    }

    public void update(Connection connection, Customer customer) throws SQLException {
        String sql = "UPDATE customers SET name=?,email=?,phone=?,city=?,active=? WHERE customer_id=?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, customer.name());
            statement.setString(2, customer.email());
            statement.setString(3, customer.phone());
            statement.setString(4, customer.city());
            statement.setBoolean(5, customer.active());
            statement.setLong(6, customer.id());
            statement.executeUpdate();
        }
    }
}
