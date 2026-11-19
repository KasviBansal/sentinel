package com.sentinel.controller;

import com.sentinel.service.AppServices;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class ShellController {
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("EEE, dd MMM uuuu  ·  HH:mm 'IST'", java.util.Locale.ENGLISH);

    @FXML private Label breadcrumb;
    @FXML private Label clockLabel;
    @FXML private Label profileEmail;
    @FXML private VBox navigation;
    @FXML private ScrollPane viewport;
    private AppServices services;
    private Runnable onLogout;
    private Timeline clock;
    private DashboardController dashboard;

    public void initialize(AppServices services, String email, Runnable onLogout) {
        this.services = services;
        this.onLogout = onLogout;
        profileEmail.setText(email);
        clock = new Timeline(new KeyFrame(Duration.seconds(1), event -> updateClock()));
        clock.setCycleCount(Timeline.INDEFINITE);
        updateClock();
        clock.play();

        ToggleGroup group = new ToggleGroup();
        nav(group, "▦", "Dashboard", () -> {
            if (dashboard == null) dashboard = DashboardController.load(services);
            dashboard.refresh();
            return dashboard.root();
        });
        nav(group, "⇄", "Transactions", () -> new TransactionsController(services).view());
        nav(group, "◇", "Fraud Alerts", () -> new AlertsController(services).view());
        nav(group, "≡", "Fraud Rules", () -> new RulesController(services).view());
        nav(group, "♙", "Customers", () -> new CustomersController(services).view());
        nav(group, "▥", "Reports", () -> new ReportsController(services).view());
        ((ToggleButton) navigation.getChildren().getFirst()).fire();
    }

    private void nav(ToggleGroup group, String icon, String title, java.util.function.Supplier<Node> page) {
        ToggleButton button = new ToggleButton(title);
        Label glyph = new Label(icon);
        glyph.getStyleClass().add("nav-icon");
        button.setGraphic(glyph);
        button.setToggleGroup(group);
        button.setMaxWidth(Double.MAX_VALUE);
        button.getStyleClass().add("nav-button");
        button.setOnAction(event -> {
            button.setSelected(true);
            breadcrumb.setText(title);
            try {
                Node view = page.get();
                viewport.setContent(view);
                viewport.setVvalue(0);
                FadeTransition fade = new FadeTransition(Duration.millis(140), view);
                fade.setFromValue(0.76);
                fade.setToValue(1);
                fade.play();
            } catch (RuntimeException error) {
                viewport.setContent(Ui.page("Page unavailable", "Try refreshing by opening this page again."));
                Ui.error(error);
            }
        });
        navigation.getChildren().add(button);
    }

    private void updateClock() {
        clockLabel.setText(CLOCK.format(ZonedDateTime.now(ZoneId.of("Asia/Kolkata"))));
    }

    @FXML private void logout() {
        clock.stop();
        onLogout.run();
    }
}
