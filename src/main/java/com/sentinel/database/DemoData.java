package com.sentinel.database;

import com.sentinel.dao.*;
import com.sentinel.model.*;
import com.sentinel.service.AuthService;
import com.sentinel.service.TransactionService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class DemoData {
    private static final String[] FIRST = {"Aarav", "Priya", "Rohan", "Simran", "Kabir", "Ananya", "Dev", "Meera",
            "Arjun", "Neha", "Aditya", "Ishita", "Vihaan", "Kavya", "Dhruv", "Aditi", "Nikhil", "Riya", "Ishan", "Tara",
            "Rahul", "Sanya", "Varun", "Avni", "Karan", "Diya", "Yash", "Maya", "Arnav", "Shreya", "Rajat", "Tanvi",
            "Sameer", "Naina", "Vikram", "Sneha", "Siddharth", "Reva", "Manav", "Janhavi", "Akash", "Nidhi", "Aman",
            "Pooja", "Harsh", "Leela", "Kunal", "Kriti", "Rishi", "Veda"};
    private static final String[] LAST = {"Sharma", "Nair", "Kapoor", "Kaur", "Sethi", "Mehta", "Gupta", "Joshi", "Verma", "Iyer"};
    private static final String[] CITIES = {"Delhi", "Kochi", "Mumbai", "Chandigarh", "Jaipur", "Ahmedabad",
            "Lucknow", "Pune", "Bengaluru", "Chennai"};
    private static final TransferType[] MIX = {TransferType.UPI, TransferType.UPI, TransferType.UPI, TransferType.NEFT,
            TransferType.NEFT, TransferType.IMPS, TransferType.IMPS, TransferType.RTGS, TransferType.Card, TransferType.Card};

    private DemoData() { }

    public static void seed(Database database, CustomerDAO customers, FraudRuleDAO rules, BlacklistDAO blacklist,
                            UserDAO users, TransactionService transactions, LocalDate date) {
        database.write(connection -> {
            if (users.findByEmail(connection, AuthService.DEMO_EMAIL) != null) return null;
            rules.insertDefaults(connection);
            for (int i = 0; i < FIRST.length; i++) {
                String surname = LAST[i % LAST.length];
                String email = (FIRST[i] + "." + surname + "@example.test").toLowerCase(Locale.ROOT);
                long id = customers.insert(connection, new Customer(0, FIRST[i] + " " + surname, email,
                        "+91 90000%05d".formatted(i + 1), account(i), CITIES[i % CITIES.length], true));
                if (i < 10) customers.insertAccount(connection, id, account(50 + i));
            }
            String[] reasons = {"Confirmed account compromise", "Identity verification mismatch",
                    "Repeat chargeback activity", "Linked to a reported payment scam", "Unverified account ownership"};
            for (int i = 50; i < 60; i++) blacklist.insert(connection, account(i), reasons[(i - 50) % reasons.length]);

            List<Transaction> drafts = new ArrayList<>();
            Random random = new Random(553);
            for (int i = 0; i < 494; i++) {
                int sender = random.nextInt(i % 17 == 0 ? 60 : 49);
                int receiver = random.nextInt(i % 11 == 0 ? 60 : 49);
                if (receiver == sender) receiver = (receiver + 1) % 49;
                long amount = i % 9 == 0 ? 52_000 + random.nextInt(108_000) : 180 + random.nextInt(22_000);
                int hour = i % 15 == 0 ? 1 + random.nextInt(4) : 8 + random.nextInt(13);
                LocalDateTime time = date.minusDays(i >= 480 ? 0 : random.nextInt(60)).atTime(hour, random.nextInt(60));
                drafts.add(new Transaction(0, account(sender), account(receiver), amount * 100,
                        MIX[random.nextInt(MIX.length)], time, null));
            }
            for (int i = 0; i < 6; i++) {
                drafts.add(new Transaction(0, account(49), account(i), 85_000 + i * 10_000L,
                        TransferType.UPI, date.atTime(14, i), null));
            }
            drafts.sort(Comparator.comparing(Transaction::time));
            for (Transaction draft : drafts) transactions.evaluateAndSave(connection, draft);
            users.insert(connection, AuthService.DEMO_EMAIL, AuthService.createCredentials("Admin@123"));
            return null;
        });
    }

    private static String account(int index) { return "701809%06d".formatted(index + 1); }
}
