package com.sentinel.controller;

import com.sentinel.Main;
import com.sentinel.model.*;
import com.sentinel.util.Formats;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Locale;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

final class Ui {
    private Ui() { }

    static Label label(String text, String style) {
        Label label = new Label(text);
        label.getStyleClass().add(style);
        return label;
    }

    static VBox page(String title, String subtitle) {
        return page(title, subtitle, new Node[0]);
    }

    static VBox page(String title, String subtitle, Node... actions) {
        Label detail = label(subtitle, "muted");
        detail.setWrapText(true);
        VBox introduction = new VBox(5, label(title, "page-title"), detail);
        Region space = new Region();
        HBox.setHgrow(space, Priority.ALWAYS);
        HBox heading = new HBox(12, introduction, space);
        heading.setAlignment(Pos.CENTER_LEFT);
        heading.getChildren().addAll(actions);
        VBox page = new VBox(16, heading);
        page.getStyleClass().add("page");
        return page;
    }

    static HBox toolbar(Node... nodes) {
        HBox box = new HBox(10, nodes);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("toolbar");
        return box;
    }

    static VBox panel(String title, String description, Node node) {
        VBox panel = new VBox(6, label(title, "section-title"), label(description, "micro"), node);
        panel.getStyleClass().add("surface");
        VBox.setVgrow(node, Priority.ALWAYS);
        return panel;
    }

    static Button button(String label, String style, Runnable action) {
        Button button = new Button(label);
        button.getStyleClass().add(style);
        button.setOnAction(event -> action.run());
        return button;
    }

    static <T> TableView<T> table(String empty) {
        TableView<T> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(label(empty, "muted"));
        table.setPrefHeight(445);
        table.setFixedCellSize(39);
        return table;
    }

    static <T,V> TableColumn<T,V> column(TableView<T> table, String title, double width, Function<T,V> value) {
        TableColumn<T,V> column = new TableColumn<>(title);
        column.setPrefWidth(width);
        column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(value.apply(cell.getValue())));
        table.getColumns().add(column);
        return column;
    }

    static <T,V> void badges(TableColumn<T,V> column, Function<V,String> style) {
        column.setCellFactory(ignored -> new TableCell<>() {
            @Override protected void updateItem(V item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label badge = label(item.toString(), "badge");
                    badge.getStyleClass().add(style.apply(item));
                    setGraphic(badge);
                }
            }
        });
    }

    static <T> TableView<T> rows(TableView<T> table, Collection<T> data) {
        table.getItems().setAll(data);
        return table;
    }

    static TextField search(String prompt) {
        TextField input = new TextField();
        input.setPromptText(prompt);
        input.setPrefWidth(272);
        return input;
    }

    static <T> ComboBox<T> select(String prompt, Collection<T> values) {
        ComboBox<T> combo = new ComboBox<>();
        combo.setPromptText(prompt);
        combo.getItems().addAll(values);
        return combo;
    }

    static boolean contains(String query, Object... terms) {
        String lower = query.strip().toLowerCase(Locale.ROOT);
        for (Object term : terms) {
            if (String.valueOf(term).toLowerCase(Locale.ROOT).contains(lower)) return true;
        }
        return false;
    }

    static <T> void rowAction(TableView<T> table, java.util.function.Consumer<T> open) {
        table.setRowFactory(ignored -> {
            TableRow<T> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) open.accept(row.getItem());
            });
            return row;
        });
        table.setOnKeyPressed(event -> {
            T selected = table.getSelectionModel().getSelectedItem();
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER && selected != null) open.accept(selected);
        });
    }

    static Dialog<ButtonType> dialog(String title, Node content, ButtonType... choices) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("SENTINEL · " + title);
        for (var window : javafx.stage.Window.getWindows()) {
            if (window instanceof javafx.stage.Stage stage && stage.isShowing() && stage.isFocused()) {
                dialog.initOwner(stage);
                break;
            }
        }
        DialogPane pane = dialog.getDialogPane();
        pane.getStylesheets().add(Main.resource("/css/sentinel.css"));
        pane.getStyleClass().add("sentinel-dialog");
        pane.setHeaderText(title);
        pane.setContent(content);
        pane.getButtonTypes().addAll(choices);
        pane.setPrefWidth(555);
        return dialog;
    }

    static GridPane form() {
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(10);
        return grid;
    }

    static void field(GridPane grid, int row, String title, Node input) {
        grid.add(label(title, "field-label"), 0, row);
        grid.add(input, 1, row);
        if (input instanceof Region region) region.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(input, Priority.ALWAYS);
    }

    static void fact(GridPane grid, int row, String title, String value) {
        Label label = new Label(value);
        label.setWrapText(true);
        field(grid, row, title, label);
    }

    static void submit(String title, Node content, String action, CheckedAction save) {
        Label error = label("", "validation");
        error.setWrapText(true);
        VBox body = new VBox(12, content, error);
        ButtonType confirm = new ButtonType(action, ButtonBar.ButtonData.OK_DONE);
        Dialog<ButtonType> dialog = dialog(title, body, ButtonType.CANCEL, confirm);
        Button button = (Button) dialog.getDialogPane().lookupButton(confirm);
        button.getStyleClass().add("primary");
        button.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            try {
                save.run();
            } catch (Exception failure) {
                error.setText(message(failure));
                event.consume();
            }
        });
        dialog.showAndWait();
    }

    interface CheckedAction { void run() throws Exception; }

    private static String message(Throwable failure) {
        if (failure instanceof IllegalArgumentException || failure instanceof com.sentinel.exception.InvalidTransactionException) {
            return failure.getMessage();
        }
        Logger.getLogger("com.sentinel").log(Level.WARNING, "Workspace action failed", failure);
        if (failure instanceof com.sentinel.exception.DatabaseException) {
            return "The database operation failed. Check the application log.";
        }
        if (failure instanceof java.io.IOException) return "Could not write the file. Check its location and permissions.";
        return "The operation could not be completed. Check the application log.";
    }

    static void error(Throwable failure) { notice("Action unavailable", message(failure)); }

    static void notice(String title, String message) {
        Label text = label(message, "muted");
        text.setWrapText(true);
        dialog(title, text, ButtonType.OK).showAndWait();
    }

    static void safely(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException error) {
            error(error);
        }
    }

    static DatePicker date(LocalDate date) {
        DatePicker picker = new DatePicker(date);
        picker.setEditable(false);
        picker.setPrefWidth(133);
        return picker;
    }

    static String transactionStatus(String status) { return status.equals("Normal") ? "normal" : "suspicious"; }

    static String reasons(Transaction transaction) {
        if (transaction.evaluation().matches().isEmpty()) return "No fraud rules were triggered.";
        return String.join("\n", transaction.evaluation().matches().stream()
                .map(match -> "• " + match.type().label + " · " + match.reason()).toList());
    }

    static GridPane transactionFacts(TransactionRow row) {
        Transaction t = row.transaction();
        GridPane grid = form();
        fact(grid, 0, "Transaction", t.displayId());
        fact(grid, 1, "Sender", row.senderName() + " · " + t.sender());
        fact(grid, 2, "Receiver", row.receiverName() + " · " + t.receiver());
        fact(grid, 3, "Amount", Formats.money(t.amount()));
        fact(grid, 4, "Transfer type", t.type().toString());
        fact(grid, 5, "Date and time", t.time().format(Formats.DISPLAY_TIME) + " IST");
        fact(grid, 6, "Risk", t.evaluation().score() + " / 100 · " + t.evaluation().level());
        fact(grid, 7, "Status", t.evaluation().status());
        fact(grid, 8, "Rule evaluation", reasons(t));
        return grid;
    }

    static VBox scrollContent(Node content, double height) {
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setPrefViewportHeight(height);
        VBox box = new VBox(scroll);
        box.setPadding(new Insets(0, 1, 0, 0));
        return box;
    }
}
