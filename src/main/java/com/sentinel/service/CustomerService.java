package com.sentinel.service;

import com.sentinel.dao.CustomerDAO;
import com.sentinel.database.Database;
import com.sentinel.model.*;
import java.util.List;

public class CustomerService {
    private final Database database;
    private final CustomerDAO customers;
    private final TransactionService transactions;

    public CustomerService(Database database, CustomerDAO customers, TransactionService transactions) {
        this.database = database;
        this.customers = customers;
        this.transactions = transactions;
    }

    public List<Customer> findAll() { return database.read(customers::findAll); }

    public List<Account> activeAccounts() {
        return database.read(customers::accounts).stream().filter(Account::active).toList();
    }

    public List<TransactionRow> history(Customer customer) {
        var accounts = database.read(connection -> customers.accountNumbers(connection, customer.id()));
        return transactions.findAll().stream().filter(row -> accounts.contains(row.transaction().sender())
                || accounts.contains(row.transaction().receiver())).toList();
    }

    public void save(Customer customer) {
        if (customer.name().isBlank() || customer.city().isBlank()) {
            throw new IllegalArgumentException("Enter a customer name and city.");
        }
        if (!customer.email().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }
        if (!customer.phone().matches("(?:\\+91[ -]?)?[6-9][0-9]{9}")) {
            throw new IllegalArgumentException("Enter a valid 10-digit Indian mobile number.");
        }
        if (!customer.account().matches("[0-9]{9,18}")) {
            throw new IllegalArgumentException("Enter an account number with 9 to 18 digits.");
        }
        database.write(connection -> {
            if (customers.exists(connection, customer)) {
                throw new IllegalArgumentException("This email address or account number is already registered.");
            }
            if (customer.id() == 0) customers.insert(connection, customer);
            else customers.update(connection, customer);
            return null;
        });
    }
}
