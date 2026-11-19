package com.sentinel.controller;

import com.sentinel.model.RuleType;
import com.sentinel.service.AppServices;
import com.sentinel.service.MonitoringService.Snapshot;
import com.sentinel.util.*;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

class ReportsController {
    private record RuleCount(String name, int matches) { }

    private final AppServices services;
    private final DatePicker from;
    private final DatePicker to;
    private final HBox cards = new HBox(12);
    private final TableView<RuleCount> rules = Ui.table("No rule data is available.");
    private final Label range = Ui.label("", "micro");

    ReportsController(AppServices services) {
        this.services = services;
        from = Ui.date(services.config().demoDate().minusDays(59));
        to = Ui.date(services.config().demoDate());
        try {
            SettingsBackup saved = SettingsBackup.load(services.config().directory());
            if (saved != null) {
                LocalDate start = LocalDate.parse(saved.reportFrom());
                LocalDate end = LocalDate.parse(saved.reportTo());
                if (!start.isAfter(end)) { from.setValue(start); to.setValue(end); }
            }
        } catch (IOException | DateTimeParseException error) {
            Logger.getLogger(getClass().getName()).log(Level.WARNING, "Could not load saved report dates", error);
        }
    }

    Node view() {
        Ui.column(rules, "Fraud rule", 380, RuleCount::name);
        Ui.column(rules, "Triggered transactions", 195, RuleCount::matches);
        rules.setPrefHeight(209);
        var page = Ui.page("Reports", "Summarize transfer activity and export the selected date range.",
                Ui.button("Export transactions ↓", "secondary", () -> Ui.safely(() -> export(pageOwner(), false))),
                Ui.button("Export alerts ↓", "primary", () -> Ui.safely(() -> export(pageOwner(), true))));
        var toolbar = Ui.toolbar(Ui.label("From", "field-label"), from, Ui.label("To", "field-label"), to,
                Ui.button("↻  Refresh report", "secondary", () -> Ui.safely(this::refresh)), range);
        page.getChildren().addAll(toolbar, cards, Ui.panel("Rule Trigger Counts",
                "Includes all recorded rule matches, including matches on low-risk transfers.", rules));
        refresh();
        return page;
    }

    private javafx.stage.Window pageOwner() { return from.getScene().getWindow(); }

    private void refresh() {
        display(selected());
    }

    private void display(Snapshot data) {
        cards.getChildren().setAll(
                DashboardController.metric("⇄", "Total Transactions", Integer.toString(data.transactions().size()), "Selected date range", "gold"),
                DashboardController.metric("₹", "Total Transferred", Formats.money(data.totalValue()), "Total transferred in INR", "gold"),
                DashboardController.metric("◇", "Suspicious Transactions", Long.toString(data.suspicious()), "Risk score 30 or higher", "wine"),
                DashboardController.metric("△", "High-Risk Transactions", Long.toString(data.highRisk()), "High and critical risk", "navy"));
        rules.getItems().clear();
        for (RuleType type : RuleType.values()) {
            rules.getItems().add(new RuleCount(type.label, data.ruleCounts().getOrDefault(type, 0)));
        }
        range.setText(from.getValue().format(Formats.SHORT_DATE) + " – " + to.getValue().format(Formats.SHORT_DATE) + " inclusive");
    }

    private Snapshot selected() {
        if (from.getValue() == null || to.getValue() == null) throw new IllegalArgumentException("Choose both report dates.");
        Snapshot data = services.monitoring().snapshot(from.getValue(), to.getValue());
        try {
            new SettingsBackup(from.getValue().toString(), to.getValue().toString()).save(services.config().directory());
        } catch (IOException error) {
            Logger.getLogger(getClass().getName()).log(Level.WARNING, "Could not save report date preferences", error);
        }
        return data;
    }

    private void export(javafx.stage.Window owner, boolean alerts) {
        Snapshot data = selected();
        display(data);
        String type = alerts ? "alerts" : "transactions";
        var chooser = new FileChooser();
        chooser.setTitle("Export " + type);
        chooser.setInitialFileName("sentinel-" + type + "-" + from.getValue() + "-to-" + to.getValue() + ".csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
        var file = chooser.showSaveDialog(owner);
        if (file == null) return;
        try {
            if (alerts) CsvExport.alerts(file.toPath(), data.alerts());
            else CsvExport.transactions(file.toPath(), data.transactions());
            int size = alerts ? data.alerts().size() : data.transactions().size();
            Ui.notice("CSV exported", size + " " + type + " saved to " + file.getName() + ".");
        } catch (IOException error) {
            Ui.error(error);
        }
    }
}
