package com.sentinel.controller;

import com.sentinel.model.Customer;
import com.sentinel.service.AppServices;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

class CustomersController {
    private final AppServices services;
    private final ObservableList<Customer> rows = FXCollections.observableArrayList();

    CustomersController(AppServices services) { this.services = services; }

    Node view() {
        var search = Ui.search("Search customer, email, city or account");
        search.setPrefWidth(330);
        TableView<Customer> table = Ui.table("No customers match this search.");
        Ui.column(table, "Customer ID", 93, Customer::displayId);
        Ui.column(table, "Name", 145, Customer::name);
        Ui.column(table, "Email", 205, Customer::email);
        Ui.column(table, "Phone", 130, Customer::phone);
        Ui.column(table, "Account number", 127, Customer::account);
        Ui.column(table, "City", 100, Customer::city);
        Ui.badges(Ui.column(table, "State", 87, customer -> customer.active() ? "Active" : "Inactive"),
                state -> state.equals("Active") ? "alert-review" : "alert-resolved");
        var filtered = new FilteredList<>(rows);
        var sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);
        search.textProperty().addListener((obs, old, query) -> filtered.setPredicate(customer ->
                Ui.contains(query, customer.displayId(), customer.name(), customer.email(), customer.account(), customer.city())));
        var selected = table.getSelectionModel().selectedItemProperty();
        var edit = Ui.button("Edit customer", "secondary", () -> edit(selected.get()));
        var history = Ui.button("View transactions", "secondary", () -> Ui.safely(() -> history(selected.get())));
        var deactivate = Ui.button("Deactivate", "ghost", () -> Ui.safely(() -> deactivate(selected.get())));
        edit.disableProperty().bind(selected.isNull());
        history.disableProperty().bind(selected.isNull());
        deactivate.disableProperty().bind(javafx.beans.binding.Bindings.createBooleanBinding(
                () -> selected.get() == null || !selected.get().active(), selected));
        Ui.rowAction(table, this::edit);

        var page = Ui.page("Customers", "Manage customer profiles and review account transaction history.",
                Ui.button("↻  Refresh", "secondary", () -> Ui.safely(this::refresh)),
                Ui.button("+  Add customer", "primary", () -> edit(null)));
        page.getChildren().addAll(Ui.toolbar(search), Ui.panel("Customer directory",
                "Select a customer to edit their profile or review their transfers.", table), Ui.toolbar(edit, history, deactivate));
        refresh();
        return page;
    }

    private void refresh() { rows.setAll(services.customers().findAll()); }

    private void edit(Customer existing) {
        var name = new TextField(existing == null ? "" : existing.name());
        var email = new TextField(existing == null ? "" : existing.email());
        var phone = new TextField(existing == null ? "" : existing.phone());
        var account = new TextField(existing == null ? "" : existing.account());
        var city = new TextField(existing == null ? "" : existing.city());
        name.setPromptText("Full name");
        email.setPromptText("name@example.com");
        phone.setPromptText("+91 9000012345");
        account.setPromptText("9 to 18 digits");
        city.setPromptText("City");
        account.setEditable(existing == null);
        var form = Ui.form();
        Ui.field(form, 0, "Name", name);
        Ui.field(form, 1, "Email", email);
        Ui.field(form, 2, "Phone", phone);
        Ui.field(form, 3, "Account number", account);
        Ui.field(form, 4, "City", city);
        boolean[] saved = {false};
        Ui.submit(existing == null ? "Add customer" : "Edit " + existing.displayId(), form, "Save customer", () -> {
            services.customers().save(new Customer(existing == null ? 0 : existing.id(), name.getText().strip(),
                    email.getText().strip(), phone.getText().strip(), account.getText().strip(),
                    city.getText().strip(), existing == null || existing.active()));
            saved[0] = true;
        });
        if (saved[0]) Ui.safely(this::refresh);
    }

    private void history(Customer customer) {
        var table = TransactionsController.table();
        table.setPrefHeight(325);
        Ui.rows(table, services.customers().history(customer));
        var dialog = Ui.dialog(customer.name() + " · Transaction history",
                new VBox(10, Ui.label("Incoming and outgoing transfers across the customer's accounts", "muted"), table), ButtonType.CLOSE);
        dialog.getDialogPane().setPrefWidth(960);
        dialog.showAndWait();
    }

    private void deactivate(Customer customer) {
        var confirm = new ButtonType("Deactivate customer", ButtonBar.ButtonData.OK_DONE);
        var dialog = Ui.dialog("Deactivate " + customer.name(),
                Ui.label("The customer's accounts will be unavailable for new transfers.", "muted"), ButtonType.CANCEL, confirm);
        if (dialog.showAndWait().orElse(ButtonType.CANCEL) == confirm) {
            services.customers().save(new Customer(customer.id(), customer.name(), customer.email(), customer.phone(),
                    customer.account(), customer.city(), false));
            refresh();
        }
    }
}
