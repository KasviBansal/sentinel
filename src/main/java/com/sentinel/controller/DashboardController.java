package com.sentinel.controller;

import com.sentinel.Main;
import com.sentinel.model.*;
import com.sentinel.service.AppServices;
import com.sentinel.service.MonitoringService.Snapshot;
import com.sentinel.util.Formats;
import java.io.IOException;
import java.time.LocalDate;
import java.util.function.LongFunction;
import java.util.stream.IntStream;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.StringConverter;

public class DashboardController {
    @FXML private ComboBox<String> period;
    @FXML private HBox cards;
    @FXML private HBox pieLegend;
    @FXML private StackPane alertTable;
    @FXML private LineChart<Number,Number> activity;
    @FXML private NumberAxis activityDates;
    @FXML private PieChart distribution;
    @FXML private BarChart<String,Number> channels;
    @FXML private Button openAlert;

    private AppServices services;
    private Parent root;
    private TableView<FraudAlert> table;

    static DashboardController load(AppServices services) {
        var loader = new FXMLLoader(Main.class.getResource("/fxml/dashboard.fxml"));
        try {
            Parent view = loader.load();
            DashboardController controller = loader.getController();
            controller.root = view;
            controller.services = services;
            controller.setup();
            return controller;
        } catch (IOException error) {
            throw new IllegalStateException("Dashboard layout could not be loaded", error);
        }
    }

    Parent root() { return root; }

    private void setup() {
        period.getItems().addAll("Last 7 days", "Last 30 days", "Last 60 days", "All dates");
        table = AlertsController.table(true);
        table.setFixedCellSize(35);
        table.setMinHeight(117);
        table.setPrefHeight(151);
        alertTable.getChildren().add(table);
        openAlert.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        Ui.rowAction(table, alert -> AlertsController.details(alert, services, this::refresh));
        period.getSelectionModel().select("Last 30 days");
    }

    @FXML void refresh() {
        if (table == null || period.getValue() == null) return;
        Ui.safely(() -> {
            Snapshot all = services.monitoring().snapshot(null, null);
            LocalDate end = services.config().demoDate();
            if (!all.transactions().isEmpty()) {
                LocalDate latest = all.transactions().getFirst().transaction().time().toLocalDate();
                if (latest.isAfter(end)) end = latest;
            }
            int days = switch (period.getValue()) {
                case "Last 7 days" -> 7;
                case "Last 30 days" -> 30;
                case "Last 60 days" -> 60;
                default -> 0;
            };
            LocalDate start = days == 0 ? (all.daily().isEmpty() ? end : all.daily().keySet().iterator().next())
                    : end.minusDays(days - 1L);
            Snapshot selected = days == 0 ? all : services.monitoring().snapshot(start, end);
            updateCards(selected);
            updateCharts(selected, start, end, days);
            Ui.rows(table, selected.alerts().stream().limit(12).toList());
        });
    }

    private void updateCards(Snapshot data) {
        long count = data.transactions().size();
        LongFunction<String> percentage = value -> count == 0 ? "0%" : String.format(java.util.Locale.ROOT, "%.1f%%", 100.0 * value / count);
        cards.getChildren().setAll(
                metric("⇄", "Total Transactions", String.format("%,d", count), "Across all payment channels", "gold"),
                metric("₹", "Total Transaction Value", Formats.money(data.totalValue()), "Total transferred in INR", "gold"),
                metric("◇", "Suspicious Transactions", String.format("%,d", data.suspicious()),
                        percentage.apply(data.suspicious()) + " of transactions", "wine"),
                metric("△", "High-Risk Transactions", String.format("%,d", data.highRisk()), "High and critical risk", "navy"));
    }

    static VBox metric(String icon, String title, String value, String detail, String tone) {
        HBox top = new HBox(8, Ui.label(title, "metric-label"), new Region(), Ui.label(icon, "metric-icon"));
        HBox.setHgrow(top.getChildren().get(1), Priority.ALWAYS);
        top.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label number = Ui.label(value, value.length() > 13 ? "metric-value-compact" : "metric-value");
        number.setTooltip(new Tooltip(value));
        VBox card = new VBox(4, top, number, Ui.label(detail, "micro"));
        card.getStyleClass().addAll("metric", "metric-" + tone);
        card.setMinWidth(0);
        card.setPrefWidth(250);
        card.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private void updateCharts(Snapshot data, LocalDate from, LocalDate to, int days) {
        XYChart.Series<Number, Number> dates = new XYChart.Series<>();
        long last = java.time.temporal.ChronoUnit.DAYS.between(from, to);
        activityDates.setLowerBound(0);
        activityDates.setUpperBound(Math.max(1, last));
        activityDates.setTickUnit(Math.max(1, Math.ceil(last / 5.0)));
        activityDates.setTickLabelFormatter(new StringConverter<>() {
            @Override public String toString(Number day) {
                LocalDate date = from.plusDays(day.longValue());
                return date.isAfter(to) ? "" : date.format(last >= 365 ? Formats.SHORT_DATE_YEAR : Formats.SHORT_DATE);
            }

            @Override public Number fromString(String text) { throw new UnsupportedOperationException(); }
        });
        if (days != 0 || last < 90) {
            for (long offset = 0; offset <= last; offset++) {
                dates.getData().add(new XYChart.Data<>(offset, data.daily().getOrDefault(from.plusDays(offset), 0)));
            }
        } else {
            data.daily().forEach((date, count) -> dates.getData().add(
                    new XYChart.Data<>(java.time.temporal.ChronoUnit.DAYS.between(from, date), count)));
        }
        activity.getData().clear();
        activity.getData().add(dates);

        long normal = data.transactions().size() - data.suspicious();
        var normalSlice = new PieChart.Data("Normal", normal);
        var suspiciousSlice = new PieChart.Data("Suspicious", data.suspicious());
        distribution.getData().setAll(normalSlice, suspiciousSlice);
        normalSlice.getNode().getStyleClass().add("slice-normal");
        suspiciousSlice.getNode().getStyleClass().add("slice-suspicious");
        Tooltip.install(normalSlice.getNode(), new Tooltip("Normal · " + normal));
        Tooltip.install(suspiciousSlice.getNode(), new Tooltip("Suspicious · " + data.suspicious()));
        pieLegend.getChildren().setAll(Ui.label("● Normal " + normal, "pie-normal"),
                Ui.label("● Suspicious " + data.suspicious(), "pie-suspicious"));

        XYChart.Series<String, Number> types = new XYChart.Series<>();
        for (TransferType type : TransferType.values()) {
            types.getData().add(new XYChart.Data<>(type.name(), data.types().getOrDefault(type, 0)));
        }
        channels.getData().clear();
        channels.getData().add(types);
        String[] colors = {"channel-upi", "channel-neft", "channel-imps", "channel-rtgs", "channel-card"};
        IntStream.range(0, colors.length).forEach(i -> {
            var entry = types.getData().get(i);
            entry.getNode().getStyleClass().add(colors[i]);
            Tooltip.install(entry.getNode(), new Tooltip(entry.getXValue() + " · " + entry.getYValue()));
        });
    }

    @FXML private void openSelected() {
        var selected = table.getSelectionModel().getSelectedItem();
        if (selected != null) AlertsController.details(selected, services, this::refresh);
    }
}
