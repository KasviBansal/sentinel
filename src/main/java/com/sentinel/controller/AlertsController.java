package com.sentinel.controller;

import com.sentinel.model.*;
import com.sentinel.service.AppServices;
import com.sentinel.util.Formats;
import java.math.BigDecimal;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.*;
import javafx.scene.Node;
import javafx.scene.control.*;

class AlertsController {
    private final AppServices services;
    private final ObservableList<FraudAlert> rows = FXCollections.observableArrayList();

    AlertsController(AppServices services) { this.services = services; }

    Node view() {
        var table = table(false);
        var search = Ui.search("Search ID, customer or reason");
        var status = Ui.select("All alert statuses", List.of(AlertStatus.values()));
        var filtered = new FilteredList<>(rows);
        var sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);
        Runnable filter = () -> filtered.setPredicate(alert -> (status.getValue() == null || status.getValue() == alert.status())
                && Ui.contains(search.getText(), alert.displayId(), alert.row().transaction().displayId(),
                alert.row().senderName(), alert.reason()));
        search.textProperty().addListener((obs, old, value) -> filter.run());
        status.setOnAction(event -> filter.run());
        Button inspect = Ui.button("View / review alert", "primary", () -> details(table.getSelectionModel().getSelectedItem(), services, this::refresh));
        inspect.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        Ui.rowAction(table, alert -> details(alert, services, this::refresh));
        var page = Ui.page("Fraud Alerts", "Investigate suspicious transactions and record the outcome of each review.",
                Ui.button("↻  Refresh", "secondary", () -> Ui.safely(this::refresh)));
        page.getChildren().addAll(Ui.toolbar(search, status, Ui.button("Clear filters", "ghost", () -> {
            search.clear(); status.getSelectionModel().clearSelection();
        })), Ui.panel("Investigation queue", "Double-click an alert to view evidence and update its review status.", table),
                Ui.toolbar(inspect));
        refresh();
        return page;
    }

    private void refresh() { rows.setAll(services.monitoring().snapshot(null, null).alerts()); }

    static TableView<FraudAlert> table(boolean recent) {
        TableView<FraudAlert> table = Ui.table("No fraud alerts match this view.");
        if (!recent) Ui.column(table, "Alert ID", 82, FraudAlert::displayId);
        Ui.column(table, "Transaction", 104, alert -> alert.row().transaction().displayId());
        Ui.column(table, recent ? "Sender" : "Customer", 135, alert -> alert.row().senderName());
        var amount = Ui.column(table, "Amount", 105, alert -> alert.row().transaction().amount());
        amount.setCellFactory(ignored -> new TableCell<>() {
            @Override protected void updateItem(BigDecimal value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : Formats.money(value));
            }
        });
        Ui.column(table, "Risk score", 85, FraudAlert::score);
        var reason = Ui.column(table, "Fraud reason", 355, FraudAlert::reason);
        reason.setMinWidth(155);
        reason.setCellFactory(ignored -> new TableCell<>() {
            @Override protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty ? null : value);
                setTooltip(empty || value == null ? null : new Tooltip(value));
            }
        });
        Ui.badges(Ui.column(table, "Status", 115, FraudAlert::status), value -> switch (value) {
            case NEW -> "alert-new";
            case UNDER_REVIEW -> "alert-review";
            case RESOLVED -> "alert-resolved";
        });
        return table;
    }

    static void details(FraudAlert alert, AppServices services, Runnable refresh) {
        var facts = Ui.transactionFacts(alert.row());
        var status = Ui.select("Set review status", List.of(AlertStatus.values()));
        status.setValue(alert.status());
        var note = new TextArea(alert.note());
        note.setWrapText(true);
        note.setPrefRowCount(3);
        note.setPromptText("Record the evidence or resolution.");
        Ui.field(facts, 9, "Alert status", status);
        Ui.field(facts, 10, "Review note", note);
        boolean[] saved = {false};
        Ui.submit("Review " + alert.displayId(), Ui.scrollContent(facts, 395), "Save review", () -> {
            services.monitoring().review(alert.id(), status.getValue(), note.getText());
            saved[0] = true;
        });
        if (saved[0]) Ui.safely(() -> {
            refresh.run();
            Ui.notice("Review saved", alert.displayId() + " is now " + status.getValue() + ".");
        });
    }
}
