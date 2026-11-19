package com.sentinel;

import com.sentinel.controller.LoginController;
import com.sentinel.controller.ShellController;
import com.sentinel.service.AppServices;
import com.sentinel.util.AppConfig;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class Main extends Application {
    private Stage stage;
    private AppServices services;
    private Scene scene;

    @Override public void start(Stage primaryStage) {
        stage = primaryStage;
        stage.setTitle("SENTINEL | Financial Security");
        stage.setMinWidth(1080);
        stage.setMinHeight(680);
        stage.getIcons().add(new javafx.scene.image.Image(resource("/icons/sentinel.png")));
        try {
            services = AppServices.initialize(AppConfig.load());
            showLogin();
            stage.show();
        } catch (Exception error) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, "Could not start SENTINEL", error);
            new Alert(Alert.AlertType.ERROR, "SENTINEL could not start. Check the application log or terminal.").showAndWait();
            javafx.application.Platform.exit();
        }
    }

    public static String resource(String path) { return Main.class.getResource(path).toExternalForm(); }

    private void showLogin() {
        try {
            var loader = new FXMLLoader(Main.class.getResource("/fxml/login.fxml"));
            var root = (javafx.scene.Parent) loader.load();
            loader.<LoginController>getController().initialize(services.auth(), this::showShell);
            if (scene == null) {
                var bounds = javafx.stage.Screen.getPrimary().getVisualBounds();
                scene = new Scene(root, Math.min(1440, bounds.getWidth() - 32), Math.min(860, bounds.getHeight() - 58));
                scene.getStylesheets().add(resource("/css/sentinel.css"));
                stage.setScene(scene);
            } else scene.setRoot(root);
        } catch (java.io.IOException error) {
            throw new IllegalStateException("Login could not be opened", error);
        }
    }

    private void showShell(String email) {
        try {
            var loader = new FXMLLoader(Main.class.getResource("/fxml/shell.fxml"));
            var root = (javafx.scene.Parent) loader.load();
            loader.<ShellController>getController().initialize(services, email, this::showLogin);
            scene.setRoot(root);
        } catch (java.io.IOException error) {
            throw new IllegalStateException("Workspace could not be opened", error);
        }
    }

    public static void main(String[] args) { launch(args); }
}
