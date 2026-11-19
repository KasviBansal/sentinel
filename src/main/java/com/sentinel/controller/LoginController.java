package com.sentinel.controller;

import com.sentinel.service.AuthService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoginController {
    @FXML private TextField email;
    @FXML private PasswordField password;
    @FXML private TextField visiblePassword;
    @FXML private CheckBox showPassword;
    @FXML private Label message;
    private AuthService auth;
    private Consumer<String> onLogin;

    public void initialize(AuthService auth, Consumer<String> onLogin) {
        this.auth = auth;
        this.onLogin = onLogin;
        visiblePassword.textProperty().bindBidirectional(password.textProperty());
        visiblePassword.visibleProperty().bind(showPassword.selectedProperty());
        visiblePassword.managedProperty().bind(visiblePassword.visibleProperty());
        password.visibleProperty().bind(showPassword.selectedProperty().not());
        password.managedProperty().bind(password.visibleProperty());
        email.textProperty().addListener((observable, previous, current) -> message.setText(""));
        password.textProperty().addListener((observable, previous, current) -> message.setText(""));
    }

    @FXML private void login() {
        message.setText("");
        if (email.getText().isBlank() || password.getText().isBlank()) {
            message.setText("Enter your email address and password.");
            return;
        }
        try {
            if (auth.login(email.getText(), password.getText())) {
                password.clear();
                onLogin.accept(email.getText().strip().toLowerCase(java.util.Locale.ROOT));
            } else message.setText("The email address or password is incorrect.");
        } catch (RuntimeException error) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Login failed", error);
            message.setText("The sign-in service is unavailable. Check the application log.");
        }
    }
}
