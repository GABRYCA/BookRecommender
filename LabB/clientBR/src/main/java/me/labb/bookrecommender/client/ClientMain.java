package me.labb.bookrecommender.client;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.net.Socket;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;

import me.labb.bookrecommender.server.*;
import me.labb.bookrecommender.server.db.DatabaseManager;
import me.labb.bookrecommender.server.oggetti.Libro;

public class ClientMain extends Application {

    private Stage primaryStage;
    private Label statusLabel;
    private Button connectButton;
    private Button disconnectButton;
    private final DatabaseManager dbManager = DatabaseManager.getInstance();
    private Connection dbConnection;
    private TableView<Libro> tableView;
    private Socket socket;
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 8080;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        createAndShowGUI();

        primaryStage.setOnCloseRequest(event -> {
            handleDisconnect();
            System.exit(0);
        });
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

        // Creazione TableView
        tableView = new TableView<>();

        TableColumn<Libro, String> titleColumn = new TableColumn<>("Titolo");
        titleColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getTitolo()));

        TableColumn<Libro, String> authorColumn = new TableColumn<>("Autori");
        authorColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getAutore()));

        TableColumn<Libro, String> Desc = new TableColumn<>("Descrizione");
        Desc.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getDescrizione()));

        TableColumn<Libro, String> categoryColumn = new TableColumn<>("Categoria");
        categoryColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getCategoria()));

        TableColumn<Libro, String> publisherColumn = new TableColumn<>("Editore");
        publisherColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getEditore()));

        TableColumn<Libro, Number> yearColumn = new TableColumn<>("Anno Pubblicazione");
        yearColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getAnnoPubblicazione()));

        tableView.getColumns().addAll(titleColumn, authorColumn, Desc, categoryColumn, publisherColumn, yearColumn);
        tableView.setVisible(false);

        root.getChildren().addAll(titleLabel, statusLabel, connectButton, disconnectButton, tableView);
    }

    private void handleConnect() {
        try {
            // Connessione al server
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);

            // Connessione al database
            dbConnection = dbManager.getConnection();

            statusLabel.setText("Connesso");
            statusLabel.setStyle("-fx-text-fill: green;");
            connectButton.setDisable(true);
            disconnectButton.setDisable(false);
            loadBooks();
            tableView.setVisible(true);
            primaryStage.setHeight(600);
            primaryStage.setWidth(800);
        } catch (IOException e) {
            showError("Errore di connessione al server: " + e.getMessage());
            statusLabel.setText("Errore connessione server");
            statusLabel.setStyle("-fx-text-fill: red;");
        } catch (SQLException e) {
            showError("Errore di connessione al database: " + e.getMessage());
            statusLabel.setText("Errore connessione database");
            statusLabel.setStyle("-fx-text-fill: red;");
        }
    }

    private void loadBooks() {
        ObservableList<Libro> books = FXCollections.observableArrayList();
        try {
            if (dbConnection == null || dbConnection.isClosed()) {
                showError("Connessione al database non disponibile");
                return;
            }

            System.out.println("Esecuzione query su database: " + dbManager.getDbUrl());

            Statement stmt = dbConnection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            ResultSet rs = stmt.executeQuery("SELECT \"Titolo\", \"Autori\", \"Descrizione\", \"Categoria\", \"Editore\", \"AnnoPubblicazione\" FROM \"Libri\"");

            int count = 0;
            while (rs.next()) {
                books.add(new Libro(
                        rs.getString("Titolo"),
                        rs.getString("Autori"),
                        rs.getString("Descrizione"),
                        rs.getString("Categoria"),
                        rs.getString("Editore"),
                        rs.getInt("AnnoPubblicazione")

                ));
                count++;
            }
            System.out.println("Caricati " + count + " libri dal database");

            tableView.setItems(books);

            rs.close();
            stmt.close();

        } catch (SQLException e) {
            System.err.println("Errore SQL: " + e.getMessage());
            showError("Errore durante il caricamento dei libri: " + e.getMessage());
        }
    }

    private void handleDisconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }

            if (dbConnection != null && !dbConnection.isClosed()) {
                dbConnection.close();
                dbConnection = null;
                showInfo("Disconnessione avvenuta correttamente");
            }

            statusLabel.setText("Non connesso");
            statusLabel.setStyle("-fx-text-fill: red;");
            connectButton.setDisable(false);
            disconnectButton.setDisable(true);
            tableView.setItems(null);
            tableView.setVisible(false);
            primaryStage.setHeight(200);
            primaryStage.setWidth(300);
        } catch (IOException e) {
            showError("Errore durante la chiusura del socket: " + e.getMessage());
        } catch (SQLException e) {
            showError("Errore durante la disconnessione dal database: " + e.getMessage());
        }
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

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Informazione");
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}