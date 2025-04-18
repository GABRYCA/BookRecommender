package me.labb.bookrecommender.client;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Pair;
import me.labb.bookrecommender.client.comunicazione.ClientOperazioni;
import me.labb.bookrecommender.client.oggetti.Libro;
import me.labb.bookrecommender.client.oggetti.Utente;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class ClientMain extends Application {

    private ClientOperazioni client;
    private TextArea output;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        client = new ClientOperazioni("localhost", 8080);

        VBox root = new VBox(10);
        root.setPadding(new Insets(20));

        Label titolo = new Label("📚 Book Recommender Client");
        titolo.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Button connettiBtn = new Button("Connetti al server");
        Button loginBtn = new Button("Login");
        Button registratiBtn = new Button("Registrati");
        Button cercaLibriBtn = new Button("Cerca Libri");
        Button consigliaLibriBtn = new Button("Consiglia Libri");
        Button profiloBtn = new Button("Visualizza Profilo");
        Button logoutBtn = new Button("Logout");
        Button disconnettiBtn = new Button("Disconnetti");

        output = new TextArea();
        output.setEditable(false);
        output.setPrefHeight(300);

        // Aggiungi lo ScrollPane alla TextArea
        ScrollPane scrollPane = new ScrollPane(output);
        scrollPane.setFitToWidth(true);  // Imposta il pannello di scroll per adattarsi alla larghezza

        connettiBtn.setOnAction(e -> connetti());
        disconnettiBtn.setOnAction(e -> disconnetti());
        loginBtn.setOnAction(e -> login());
        registratiBtn.setOnAction(e -> registrati());
        cercaLibriBtn.setOnAction(e -> cercaLibri());
        consigliaLibriBtn.setOnAction(e -> consigliaLibri());
        profiloBtn.setOnAction(e -> visualizzaProfilo());
        logoutBtn.setOnAction(e -> logout());

        HBox buttonsTop = new HBox(10, connettiBtn, disconnettiBtn, loginBtn, registratiBtn);
        HBox buttonsBottom = new HBox(10, cercaLibriBtn, consigliaLibriBtn, profiloBtn, logoutBtn);

        root.getChildren().addAll(titolo, buttonsTop, buttonsBottom, scrollPane);

        Scene scene = new Scene(root, 700, 450);
        stage.setTitle("Book Recommender");
        stage.setScene(scene);
        stage.show();
    }

    private void connetti() {
        try {
            if (client.connetti()) {
                stampa("✅ Connesso al server.");
            } else {
                stampa("❌ Connessione fallita.");
            }
        } catch (IOException e) {
            stampa("Errore connessione: " + e.getMessage());
        }
    }

    private void disconnetti() {
        client.chiudi();
        stampa("🔌 Disconnesso dal server.");
    }

    private void login() {
        Dialog<Pair<String, String>> dialog = creaDialogoLogin();
        Optional<Pair<String, String>> result = dialog.showAndWait();

        result.ifPresent(creds -> {
            try {
                if (client.login(creds.getKey(), creds.getValue())) {
                    stampa("👤 Login effettuato.");
                } else {
                    stampa("❌ Login fallito.");
                }
            } catch (IOException e) {
                stampa("Errore login: " + e.getMessage());
            }
        });
    }

    private void registrati() {
        Dialog<List<String>> dialog = creaDialogoRegistrazione();
        Optional<List<String>> result = dialog.showAndWait();

        result.ifPresent(data -> {
            try {
                int userID;
                if (data.get(4).isEmpty()) {
                    userID = client.registra(data.get(0), data.get(1), data.get(2), data.get(3));
                } else {
                    userID = client.registra(data.get(0), data.get(1), data.get(2), data.get(3), data.get(4));
                }
                if (userID > 0) {
                    stampa("✅ Registrazione completata. UserID: " + userID);
                } else {
                    stampa("❌ Registrazione fallita.");
                }
            } catch (IOException e) {
                stampa("Errore registrazione: " + e.getMessage());
            }
        });
    }

    private void logout() {
        try {
            if (client.logout()) {
                stampa("🚪 Logout riuscito.");
            } else {
                stampa("❌ Logout fallito.");
            }
        } catch (IOException e) {
            stampa("Errore logout: " + e.getMessage());
        }
    }

    private void visualizzaProfilo() {
        try {
            Utente utente = client.visualizzaProfilo();
            if (utente != null) {
                stampa("👤 Profilo utente:\n"
                        + "UserID: " + utente.userID() + "\n"
                        + "Nome: " + utente.nomeCompleto() + "\n"
                        + "Email: " + utente.email() + "\n"
                        + "Username: " + utente.username() + "\n"
                        + "Codice Fiscale: " + utente.codiceFiscale() + "\n"
                        + "Registrato il: " + utente.dataRegistrazione());
            } else {
                stampa("⚠️ Impossibile recuperare il profilo.");
            }
        } catch (IOException e) {
            stampa("Errore recupero profilo: " + e.getMessage());
        }
    }

    private void cercaLibri() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setHeaderText("🔍 Inserisci termine di ricerca:");
        dialog.setTitle("Cerca libri");
        Optional<String> result = dialog.showAndWait();

        result.ifPresent(termine -> {
            // Creiamo il task per eseguire la ricerca in background
            Task<List<Libro>> ricercaTask = new Task<>() {
                @Override
                protected List<Libro> call() throws Exception {
                    // Limitiamo i risultati a 100 libri
                    return client.cercaLibri(termine).stream()
                            .limit(100)  // Limita i risultati a 100
                            .toList();
                }

                @Override
                protected void succeeded() {
                    // Quando la ricerca è completata, aggiorna la UI con i risultati
                    List<Libro> libri = getValue();
                    if (libri.isEmpty()) {
                        stampa("Nessun libro trovato.");
                    } else {
                        stampa("📚 Risultati trovati:");
                        libri.forEach(libro -> stampa(
                                "\nTitolo: " + libro.titolo() +
                                        "\nAutori: " + libro.autori() +
                                        "\nCategoria: " + libro.categoria() +
                                        "\nPrezzo: €" + libro.prezzo() + "\n"));
                    }
                }

                @Override
                protected void failed() {
                    // In caso di errore, stampa il messaggio di errore
                    stampa("Errore durante la ricerca: " + getException().getMessage());
                }
            };

            // Esegui il task in un thread separato per evitare il blocco dell'interfaccia
            new Thread(ricercaTask).start();
        });
    }


    private void consigliaLibri() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setHeaderText("📘 Inserisci categoria:");
        dialog.setTitle("Consiglia libri");
        Optional<String> result = dialog.showAndWait();

        result.ifPresent(cat -> {
            try {
                List<Libro> libri = client.consigliaLibri(cat);
                if (libri.isEmpty()) {
                    stampa("Nessun consiglio disponibile.");
                } else {
                    stampa("📚 Consigliati:");
                    libri.forEach(libro -> stampa(
                            "\nTitolo: " + libro.titolo() +
                                    "\nAutori: " + libro.autori() +
                                    "\nCategoria: " + libro.categoria() +
                                    "\nPrezzo: €" + libro.prezzo() + "\n"));
                }
            } catch (IOException e) {
                stampa("Errore consigli: " + e.getMessage());
            }
        });
    }

    private void stampa(String msg) {
        output.appendText(msg + "\n");
    }

    private Dialog<Pair<String, String>> creaDialogoLogin() {
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Login");
        dialog.setHeaderText("Inserisci username e password");

        ButtonType loginButtonType = new ButtonType("Login", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20, 150, 10, 10));

        TextField username = new TextField();
        PasswordField password = new PasswordField();
        grid.add(new Label("Username:"), 0, 0);
        grid.add(username, 1, 0);
        grid.add(new Label("Password:"), 0, 1);
        grid.add(password, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                return new Pair<>(username.getText(), password.getText());
            }
            return null;
        });
        return dialog;
    }

    private Dialog<List<String>> creaDialogoRegistrazione() {
        Dialog<List<String>> dialog = new Dialog<>();
        dialog.setTitle("Registrazione");
        dialog.setHeaderText("Inserisci i tuoi dati");

        ButtonType registerButton = new ButtonType("Registrati", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(registerButton, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nome = new TextField();
        TextField email = new TextField();
        TextField username = new TextField();
        PasswordField password = new PasswordField();
        TextField cf = new TextField();

        grid.add(new Label("Nome Completo:"), 0, 0); grid.add(nome, 1, 0);
        grid.add(new Label("Email:"), 0, 1); grid.add(email, 1, 1);
        grid.add(new Label("Username:"), 0, 2); grid.add(username, 1, 2);
        grid.add(new Label("Password:"), 0, 3); grid.add(password, 1, 3);
        grid.add(new Label("Codice Fiscale:"), 0, 4); grid.add(cf, 1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == registerButton) {
                return List.of(nome.getText(), email.getText(), username.getText(), password.getText(), cf.getText());
            }
            return null;
        });
        return dialog;
    }
}
