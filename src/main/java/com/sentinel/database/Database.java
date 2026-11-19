package com.sentinel.database;

import com.sentinel.exception.DatabaseException;
import java.nio.file.Path;
import java.sql.*;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Database {
    private static final Logger LOG = Logger.getLogger(Database.class.getName());
    private final String url;

    public Database(Path file) { url = "jdbc:sqlite:" + file.toAbsolutePath(); }

    private Connection connect() throws SQLException {
        Properties options = new Properties();
        options.setProperty("foreign_keys", "true");
        options.setProperty("busy_timeout", "5000");
        options.setProperty("transaction_mode", "IMMEDIATE");
        return DriverManager.getConnection(url, options);
    }

    @FunctionalInterface
    public interface Work<T, E extends Exception> {
        T run(Connection connection) throws SQLException, E;
    }

    public <T, E extends Exception> T read(Work<T, E> work) throws E {
        try (Connection connection = connect()) {
            return work.run(connection);
        } catch (SQLException error) {
            throw failure(error);
        }
    }

    public <T, E extends Exception> T write(Work<T, E> work) throws E {
        try (Connection connection = connect()) {
            connection.setAutoCommit(false);
            boolean committed = false;
            try {
                T result = work.run(connection);
                connection.commit();
                committed = true;
                return result;
            } finally {
                if (!committed) {
                    try {
                        connection.rollback();
                    } catch (SQLException rollbackError) {
                        LOG.log(Level.SEVERE, "Could not roll back SQLite transaction", rollbackError);
                    }
                }
            }
        } catch (SQLException error) {
            throw failure(error);
        }
    }

    private DatabaseException failure(SQLException error) {
        LOG.log(Level.SEVERE, "SQLite operation failed", error);
        return new DatabaseException("The database operation failed. Check the application log.", error);
    }

    public void initialize() {
        write(connection -> {
            String[] statements = {
                """
                CREATE TABLE IF NOT EXISTS customers (
                    customer_id INTEGER PRIMARY KEY, name TEXT NOT NULL, email TEXT NOT NULL COLLATE NOCASE UNIQUE,
                    phone TEXT NOT NULL, account_number TEXT NOT NULL UNIQUE, city TEXT NOT NULL,
                    active INTEGER NOT NULL CHECK(active IN (0,1))
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS accounts (
                    account_number TEXT PRIMARY KEY, customer_id INTEGER NOT NULL REFERENCES customers(customer_id)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS transactions (
                    transaction_id INTEGER PRIMARY KEY, sender_account TEXT NOT NULL REFERENCES accounts(account_number),
                    receiver_account TEXT NOT NULL REFERENCES accounts(account_number),
                    amount_paise INTEGER NOT NULL CHECK(amount_paise > 0), transaction_type TEXT NOT NULL,
                    transaction_time TEXT NOT NULL, risk_score INTEGER NOT NULL CHECK(risk_score BETWEEN 0 AND 100),
                    risk_level TEXT NOT NULL, status TEXT NOT NULL, CHECK(sender_account <> receiver_account)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS fraud_rules (
                    rule_id TEXT PRIMARY KEY, rule_name TEXT NOT NULL, threshold_value INTEGER NOT NULL,
                    enabled INTEGER NOT NULL CHECK(enabled IN (0,1)), risk_weight INTEGER NOT NULL
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS transaction_rule_matches (
                    transaction_id INTEGER NOT NULL REFERENCES transactions(transaction_id),
                    rule_id TEXT NOT NULL REFERENCES fraud_rules(rule_id), reason TEXT NOT NULL,
                    PRIMARY KEY(transaction_id, rule_id)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS blacklist (
                    blacklist_id INTEGER PRIMARY KEY, account_number TEXT NOT NULL UNIQUE REFERENCES accounts(account_number),
                    reason TEXT NOT NULL
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS fraud_alerts (
                    alert_id INTEGER PRIMARY KEY, transaction_id INTEGER NOT NULL UNIQUE REFERENCES transactions(transaction_id),
                    reason TEXT NOT NULL, risk_score INTEGER NOT NULL, status TEXT NOT NULL, review_note TEXT NOT NULL DEFAULT ''
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS users (
                    user_id INTEGER PRIMARY KEY, email TEXT NOT NULL COLLATE NOCASE UNIQUE,
                    password_hash BLOB NOT NULL, password_salt BLOB NOT NULL
                )
                """,
                "CREATE INDEX IF NOT EXISTS idx_sender_time ON transactions(sender_account, transaction_time)",
                "CREATE INDEX IF NOT EXISTS idx_transaction_time ON transactions(transaction_time)"
            };
            try (Statement statement = connection.createStatement()) {
                for (String sql : statements) statement.executeUpdate(sql);
            }
            return null;
        });
    }
}
