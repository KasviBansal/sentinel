package com.sentinel.service;

import com.sentinel.dao.*;
import com.sentinel.database.Database;
import com.sentinel.database.DemoData;
import com.sentinel.util.AppConfig;

public record AppServices(AppConfig config, AuthService auth, TransactionService transfers,
                          CustomerService customers, MonitoringService monitoring) {
    public static AppServices initialize(AppConfig config) {
        Database database = new Database(config.database());
        database.initialize();
        CustomerDAO customers = new CustomerDAO();
        TransactionDAO transactions = new TransactionDAO();
        FraudAlertDAO alerts = new FraudAlertDAO();
        FraudRuleDAO rules = new FraudRuleDAO();
        BlacklistDAO blacklist = new BlacklistDAO();
        UserDAO users = new UserDAO();
        var detector = new FraudDetectionService(rules, transactions, blacklist);
        var transfers = new TransactionService(database, transactions, customers, alerts, detector);
        DemoData.seed(database, customers, rules, blacklist, users, transfers, config.demoDate());
        return new AppServices(config, new AuthService(database, users), transfers,
                new CustomerService(database, customers, transfers), new MonitoringService(database, transactions, alerts, rules));
    }
}
