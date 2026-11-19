package com.sentinel.controller;

import com.sentinel.exception.InvalidTransactionException;
import com.sentinel.model.*;
import com.sentinel.service.AppServices;
import com.sentinel.util.Formats;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.*;
import javafx.scene.Node;
import javafx.scene.control.*;

class TransactionsController {
    private final AppServices services;
    private final ObservableList<TransactionRow> rows = FXCollections.observableArrayList();

    TransactionsController(AppServices services) { this.services = services; }

    Node view() {
        var table = table();
        var search = Ui.search("Search ID, name or account");
        var risk = Ui.select("All risk levels", List.of(RiskLevel.values()));
        var channel = Ui.select("All transfer types", List.of(TransferType.values()));
        var filtered = new FilteredList<>(rows);
        var sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);
        Runnable filter = () -> filtered.setPredicate(row -> {
            Transaction t = row.transaction();
            return (risk.getValue() == null || risk.getValue() == t.evaluation().level())
                    && (channel.getValue() == null || channel.getValue() == t.type())
                    && Ui.contains(search.getText(), t.displayId(), row.senderName(), row.receiverName(), t.sender(), t.receiver());
        });
        search.textProperty().addListener((obs, old, text) -> filter.run());
        risk.setOnAction(event -> filter.run());
        channel.setOnAction(event -> filter.run());

        Button inspect = Ui.button("View details", "secondary", () -> details(table.getSelectionModel().getSelectedItem()));
        inspect.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        var clear = Ui.button("Clear filters", "ghost", () -> {
            search.clear();
            risk.getSelectionModel().clearSelection();
            channel.getSelectionModel().clearSelection();
        });
        var page = Ui.page("Transactions", "Search transfers, inspect risk decisions, or submit a new transaction.",
                Ui.button("↻  Refresh", "secondary", () -> Ui.safely(this::refresh)),
                Ui.button("+  Add transaction", "primary", () -> Ui.safely(this::add)));
        page.getChildren().addAll(Ui.toolbar(search, risk, channel, clear),
                Ui.panel("Transaction ledger", "Sort by clicking a column heading. Double-click a transfer to inspect it.", table),
                Ui.toolbar(inspect));
        refresh();
        return page;
    }

    static TableView<TransactionRow> table() {
        TableView<TransactionRow> table = Ui.table("No transactions match these filters.");
        Ui.column(table, "Transaction", 104, row -> row.transaction().displayId());
        var time = Ui.column(table, "Date & time (IST)", 143, row -> row.transaction().time());
        time.setCellFactory(ignored -> new TableCell<>() {
            @Override protected void updateItem(LocalDateTime value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : Formats.DISPLAY_TIME.format(value));
            }
        });
        Ui.column(table, "Sender", 118, TransactionRow::senderName);
        Ui.column(table, "Receiver", 118, TransactionRow::receiverName);
        var amount = Ui.column(table, "Amount", 100, row -> row.transaction().amount());
        amount.setCellFactory(ignored -> new TableCell<>() {
            @Override protected void updateItem(BigDecimal value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : Formats.money(value));
            }
        });
        Ui.column(table, "Type", 58, row -> row.transaction().type());
        Ui.column(table, "Score", 61, row -> row.transaction().evaluation().score());
        Ui.column(table, "Risk level", 79, row -> row.transaction().evaluation().level());
        Ui.badges(Ui.column(table, "Status", 100, row -> row.transaction().evaluation().status()), Ui::transactionStatus);
        Ui.rowAction(table, TransactionsController::details);
        return table;
    }

    private void refresh() { rows.setAll(services.transfers().findAll()); }

    private void add() {
        var accounts = services.customers().activeAccounts();
        var sender = Ui.select("Choose sender account", accounts);
        var receiver = Ui.select("Choose receiver account", accounts);
        var type = Ui.select("Choose transfer type", List.of(TransferType.values()));
        var amount = new TextField();
        amount.setPromptText("e.g. 75000.00");
        var date = Ui.date(LocalDate.now(ZoneId.of("Asia/Kolkata")));
        var time = new TextField("12:00");
        time.setPromptText("HH:mm");
        var form = Ui.form();
        Ui.field(form, 0, "Sender account", sender);
        Ui.field(form, 1, "Receiver account", receiver);
        Ui.field(form, 2, "Amount (INR)", amount);
        Ui.field(form, 3, "Transfer type", type);
        Ui.field(form, 4, "Transaction date", date);
        Ui.field(form, 5, "Time (IST)", time);
        var saved = new ArrayList<TransactionRow>();
        Ui.submit("Add transaction", form, "Evaluate and save", () -> {
            if (sender.getValue() == null || receiver.getValue() == null || date.getValue() == null || type.getValue() == null
                    || time.getText().isBlank() || amount.getText().isBlank()) {
                throw new InvalidTransactionException("Complete all transaction fields.");
            }
            BigDecimal value;
            LocalTime clock;
            try {
                value = new BigDecimal(amount.getText().strip());
            } catch (NumberFormatException error) {
                throw new InvalidTransactionException("Enter a valid amount in rupees, such as 75000.00.");
            }
            try {
                if (!time.getText().strip().matches("[0-9]{2}:[0-9]{2}")) throw new DateTimeParseException("HH:mm", time.getText(), 0);
                clock = LocalTime.parse(time.getText().strip());
            } catch (DateTimeParseException error) {
                throw new InvalidTransactionException("Enter a valid time in 24-hour HH:mm format.");
            }
            Transaction transaction = services.transfers().create(sender.getValue().number(), receiver.getValue().number(),
                    value, type.getValue(), date.getValue().atTime(clock));
            saved.add(new TransactionRow(transaction, sender.getValue().customer(), receiver.getValue().customer()));
        });
        if (!saved.isEmpty()) {
            refresh();
            Ui.dialog("Transaction evaluated", Ui.transactionFacts(saved.getFirst()), ButtonType.OK).showAndWait();
        }
    }

    private static void details(TransactionRow row) {
        Ui.dialog("Transaction details", Ui.transactionFacts(row), ButtonType.CLOSE).showAndWait();
    }
}
