package com.sentinel.controller;

import com.sentinel.model.RuleConfig;
import com.sentinel.model.RuleType;
import com.sentinel.service.AppServices;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

class RulesController {
    private final AppServices services;
    private final GridPane cards = new GridPane();

    RulesController(AppServices services) { this.services = services; }

    Node view() {
        var page = Ui.page("Fraud Rules", "Configure the rules applied to new transactions.",
                Ui.button("↻  Refresh", "secondary", () -> Ui.safely(this::refresh)));
        cards.setHgap(14);
        cards.setVgap(14);
        for (int i = 0; i < 2; i++) {
            var column = new ColumnConstraints();
            column.setPercentWidth(50);
            column.setHgrow(Priority.ALWAYS);
            cards.getColumnConstraints().add(column);
        }
        page.getChildren().add(cards);
        refresh();
        return page;
    }

    private void refresh() {
        cards.getChildren().clear();
        int i = 0;
        for (RuleConfig rule : services.monitoring().rules()) {
            cards.add(card(rule), i % 2, i / 2);
            i++;
        }
    }

    private Node card(RuleConfig rule) {
        var state = Ui.label(rule.enabled() ? "Enabled" : "Disabled", "badge");
        state.getStyleClass().add(rule.enabled() ? "alert-review" : "alert-resolved");
        var spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        var header = new HBox(8, Ui.label(rule.type().label, "section-title"), spacer, state);
        header.setAlignment(Pos.CENTER_LEFT);
        var description = Ui.label(rule.type().description, "muted");
        description.setWrapText(true);
        var facts = Ui.form();
        facts.getStyleClass().add("rule-facts");
        Ui.fact(facts, 0, "Threshold", rule.displayThreshold());
        Ui.fact(facts, 1, "Risk weight", rule.weight() + " points");
        var card = new VBox(12, header, description, facts, Ui.button("Edit rule  ↗", "secondary", () -> edit(rule)));
        card.getStyleClass().addAll("surface", "rule-card");
        return card;
    }

    private void edit(RuleConfig rule) {
        var form = Ui.form();
        var toggle = new CheckBox("Enabled");
        toggle.setSelected(rule.enabled());
        Ui.field(form, 0, "Evaluation state", toggle);
        TextField threshold = new TextField(Long.toString(rule.threshold()));
        boolean editable = rule.type() == RuleType.HIGH_AMOUNT || rule.type() == RuleType.FREQUENCY;
        if (editable) Ui.field(form, 1, rule.type() == RuleType.HIGH_AMOUNT ? "Threshold (whole INR)" : "Transfers per 10 min", threshold);
        else Ui.fact(form, 1, "Threshold", rule.displayThreshold());
        Ui.fact(form, 2, "Risk weight", rule.weight() + " points");
        boolean[] saved = {false};
        Ui.submit("Edit " + rule.type().label, form, "Save configuration", () -> {
            long value;
            try {
                value = editable ? Long.parseLong(threshold.getText().strip()) : rule.threshold();
            } catch (NumberFormatException error) {
                throw new IllegalArgumentException("Enter a positive whole-number threshold.");
            }
            services.monitoring().saveRule(new RuleConfig(rule.type(), value, toggle.isSelected(), rule.weight()));
            saved[0] = true;
        });
        if (saved[0]) Ui.safely(this::refresh);
    }
}
