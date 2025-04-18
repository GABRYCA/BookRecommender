package me.labb.bookrecommender.client;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import me.labb.bookrecommender.server.*;

public class ClientMain extends Application {

    private Stage primaryStage;
    private Label statusLabel;
    private Button connectButton;
    private Button disconnectButton;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        createAndShowGUI();
    }

    private void createAndShowGUI() {
        VBox root = createMainLayout();
        addUIElements(root);
        setupScene(root);
    }

    private VBox createMainLayout() {
        VBox root = new VBox(10);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));
        return root;
    }

    private void addUIElements(VBox root) {
        Label titleLabel = new Label("Book Recommender");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        statusLabel = new Label("Non connesso");
        statusLabel.setStyle("-fx-text-fill: red;");

        connectButton = new Button("Connetti");
        connectButton.setOnAction(e -> handleConnect());

        disconnectButton = new Button("Disconnetti");
        disconnectButton.setOnAction(e -> handleDisconnect());
        disconnectButton.setDisable(true);

        root.getChildren().addAll(titleLabel, statusLabel, connectButton, disconnectButton);
    }

    private void handleConnect() {
        statusLabel.setText("Connesso");
        statusLabel.setStyle("-fx-text-fill: green;");
        connectButton.setDisable(true);
        disconnectButton.setDisable(false);
    }

    private void handleDisconnect() {
        statusLabel.setText("Non connesso");
        statusLabel.setStyle("-fx-text-fill: red;");
        connectButton.setDisable(false);
        disconnectButton.setDisable(true);
    }

    private void setupScene(VBox root) {
        Scene scene = new Scene(root, 300, 200);
        primaryStage.setTitle("Book Recommender Client");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Errore");
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}