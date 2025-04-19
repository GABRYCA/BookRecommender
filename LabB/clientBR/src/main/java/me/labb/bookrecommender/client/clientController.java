package me.labb.bookrecommender.client;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import javafx.util.Pair;
import me.labb.bookrecommender.client.comunicazione.ClientOperazioni;
import me.labb.bookrecommender.client.oggetti.Libro;
import me.labb.bookrecommender.client.oggetti.Utente;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class clientController implements Initializable {

    @FXML private Label statusLabel;
    @FXML private Button connettiBtn;
    @FXML private Button disconnettiBtn;
    @FXML private Button loginBtn;
    @FXML private Button registratiBtn;
    @FXML private Button logoutBtn;
    @FXML private Button cercaBtn;
    @FXML private Button consigliaBtn;
    @FXML private Button profiloBtn;
    @FXML private TextArea output;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoriaComboBox;
    @FXML private VBox resultContainer;
    @FXML private Label resultLabel;

    private ClientOperazioni client;
    private boolean isConnected = false;
    private boolean isLoggedIn = false;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        client = new ClientOperazioni("localhost", 8080);

        // Inizializza lo stato dei pulsanti
        updateUIState();

        // Configura il ComboBox con funzionalità di ricerca e filtro
        setupCategoriaComboBox();

        // Aggiungi un tooltip per guidare l'utente
        Tooltip tooltip = new Tooltip("Seleziona una categoria e clicca 'Consiglia' per trovare libri simili");
        Tooltip.install(categoriaComboBox, tooltip);

        // L'output iniziale con animazione
        stampaConAnimazione("Benvenuto nel Book Recommender. Connettiti al server per iniziare.");

        // Aggiungi animazione all'hover dei pulsanti
        configuraAnimazioniPulsanti();
    }

    private void setupCategoriaComboBox() {
        // Aggiungi categorie predefinite
        List<String> categorie = List.of(
                "Narrativa", "Saggistica", "Poesia", "Fantascienza",
                "Fantasy", "Gialli", "Romanzi storici", "Biografie",
                "Self-help", "Arte", "Scienze", "Tecnologia",
                "Storia", "Filosofia", "Viaggi", "Cucina",
                "Religione", "Economia", "Lingue", "Musica"
        );

        categoriaComboBox.getItems().addAll(categorie);

        // Aggiungi un prompt text al ComboBox
        categoriaComboBox.setPromptText("Seleziona categoria");

        // Abilita l'editing per permettere la ricerca
        categoriaComboBox.setEditable(true);

        // Aggiungi un listener che filtra le opzioni in base al testo digitato
        categoriaComboBox.getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == null || newValue.isEmpty()) {
                categoriaComboBox.getItems().setAll(categorie);
                return;
            }

            String lowerCaseSearch = newValue.toLowerCase();
            categoriaComboBox.getItems().setAll(
                    categorie.stream()
                            .filter(categoria -> categoria.toLowerCase().contains(lowerCaseSearch))
                            .toList()
            );

            // Mostra il dropdown se ci sono risultati di ricerca
            if (!categoriaComboBox.getItems().isEmpty() && !categoriaComboBox.isShowing()) {
                Platform.runLater(categoriaComboBox::show);
            }
        });

        // Aggiungi evento sulla selezione
        categoriaComboBox.setOnAction(event -> {
            if (categoriaComboBox.getValue() != null) {
                // Applica animazione di fade al gruppo di consigli
                FadeTransition fade = new FadeTransition(Duration.millis(200), consigliaBtn);
                fade.setFromValue(0.7);
                fade.setToValue(1.0);
                fade.play();
            }
        });
    }

    private void configuraAnimazioniPulsanti() {
        // Aggiungi animazioni a tutti i pulsanti
        List<Button> buttons = List.of(
                connettiBtn, disconnettiBtn, loginBtn, registratiBtn,
                logoutBtn, cercaBtn, consigliaBtn, profiloBtn
        );

        for (Button button : buttons) {
            button.setOnMouseEntered(e -> {
                ScaleTransition st = new ScaleTransition(Duration.millis(100), button);
                st.setToX(1.05);
                st.setToY(1.05);
                st.play();
            });

            button.setOnMouseExited(e -> {
                ScaleTransition st = new ScaleTransition(Duration.millis(100), button);
                st.setToX(1.0);
                st.setToY(1.0);
                st.play();
            });

            button.setOnMousePressed(e -> {
                ScaleTransition st = new ScaleTransition(Duration.millis(100), button);
                st.setToX(0.95);
                st.setToY(0.95);
                st.play();
            });

            button.setOnMouseReleased(e -> {
                ScaleTransition st = new ScaleTransition(Duration.millis(100), button);
                st.setToX(1.0);
                st.setToY(1.0);
                st.play();
            });
        }
    }

    @FXML
    private void connetti() {
        Task<Boolean> task = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                try {
                    return client.connetti();
                } catch (IOException e) {
                    Platform.runLater(() -> stampa("Errore connessione: " + e.getMessage()));
                    return false;
                }
            }

            @Override
            protected void succeeded() {
                boolean success = getValue();
                isConnected = success;
                if (success) {
                    stampaConAnimazione("✅ Connesso al server con successo.");
                    animaCambioStato(statusLabel, "Connesso", "status-disconnected", "status-connected");
                } else {
                    stampaConAnimazione("❌ Connessione fallita.");
                    statusLabel.setText("Non connesso");
                    statusLabel.getStyleClass().remove("status-connected");
                    statusLabel.getStyleClass().add("status-disconnected");
                }
                updateUIState();
            }
        };

        // Animazione di caricamento durante la connessione
        RotateTransition rt = new RotateTransition(Duration.millis(2000), connettiBtn);
        rt.setByAngle(360);
        rt.setCycleCount(Animation.INDEFINITE);
        rt.play();

        connettiBtn.setDisable(true);
        connettiBtn.setText("Connessione...");

        new Thread(task).start();

        task.setOnSucceeded(event -> {
            rt.stop();
            connettiBtn.setText("Connetti");
            updateUIState();
        });

        task.setOnFailed(event -> {
            rt.stop();
            connettiBtn.setText("Connetti");
            connettiBtn.setDisable(false);
        });
    }

    @FXML
    private void disconnetti() {
        client.chiudi();
        isConnected = false;
        isLoggedIn = false;
        animaCambioStato(statusLabel, "Non connesso", "status-connected", "status-disconnected");
        stampaConAnimazione("🔌 Disconnesso dal server.");
        updateUIState();
    }

    @FXML
    private void login() {
        Dialog<Pair<String, String>> dialog = creaDialogoLogin();
        animaDialogo(dialog);
        Optional<Pair<String, String>> result = dialog.showAndWait();

        result.ifPresent(creds -> {
            try {
                if (client.login(creds.getKey(), creds.getValue())) {
                    isLoggedIn = true;
                    stampaConAnimazione("👤 Login effettuato con successo.");
                    animaCambioStato(statusLabel, "Connesso come " + creds.getKey(), null, null);
                    updateUIState();
                } else {
                    stampaConAnimazione("❌ Login fallito. Controlla username e password.");
                    animaShake(loginBtn);
                }
            } catch (IOException e) {
                stampa("Errore login: " + e.getMessage());
            }
        });
    }

    @FXML
    private void registrati() {
        Dialog<List<String>> dialog = creaDialogoRegistrazione();
        animaDialogo(dialog);
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
                    stampaConAnimazione("✅ Registrazione completata con successo. UserID: " + userID);
                    // Mostra un dialogo di conferma con animazione
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Registrazione Completata");
                    alert.setHeaderText("Registrazione effettuata con successo");
                    alert.setContentText("UserID: " + userID + "\nPuoi ora accedere con le tue credenziali.");

                    // Aggiungi la classe CSS all'Alert
                    alert.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
                    alert.getDialogPane().getStyleClass().add("registration-alert");  // Aggiungi la classe per personalizzare l'Alert

                    animaDialogo(alert);
                    alert.showAndWait();
                } else {
                    stampaConAnimazione("❌ Registrazione fallita.");
                    animaShake(registratiBtn);
                }
            } catch (IOException e) {
                stampa("Errore registrazione: " + e.getMessage());
            }
        });
    }
    @FXML
    private void logout() {
        try {
            if (client.logout()) {
                isLoggedIn = false;
                stampaConAnimazione("🚪 Logout riuscito.");
                animaCambioStato(statusLabel, "Connesso", null, null);
                updateUIState();
            } else {
                stampaConAnimazione("❌ Logout fallito.");
                animaShake(logoutBtn);
            }
        } catch (IOException e) {
            stampa("Errore logout: " + e.getMessage());
        }
    }

    @FXML
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

                // Mostra i dati del profilo con animazione
                mostraprofilo(utente);
            } else {
                stampaConAnimazione("⚠️ Impossibile recuperare il profilo.");
                animaShake(profiloBtn);
            }
        } catch (IOException e) {
            stampa("Errore recupero profilo: " + e.getMessage());
        }
    }

    @FXML
    private void cercaLibri() {
        String termine = searchField.getText().trim();

        if (termine.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Campo vuoto");
            alert.setHeaderText("Nessun termine di ricerca");
            alert.setContentText("Inserisci un termine di ricerca.");
            animaDialogo(alert);
            alert.showAndWait();
            return;
        }

        resultLabel.setText("Ricerca in corso...");
        resultContainer.getChildren().clear();

        // Animazione di ricerca
        FadeTransition fade = new FadeTransition(Duration.millis(300), resultLabel);
        fade.setFromValue(0.5);
        fade.setToValue(1.0);
        fade.setCycleCount(Animation.INDEFINITE);
        fade.setAutoReverse(true);
        fade.play();

        // Esegui la ricerca in un thread separato
        Task<List<Libro>> task = new Task<>() {
            @Override
            protected List<Libro> call() throws Exception {
                return client.cercaLibri(termine).stream().toList();
            }

            @Override
            protected void succeeded() {
                fade.stop();
                resultLabel.setOpacity(1.0);

                List<Libro> libri = getValue();
                if (libri.isEmpty()) {
                    stampaConAnimazione("Nessun libro trovato per: \"" + termine + "\"");
                    resultLabel.setText("Nessun risultato trovato");
                } else {
                    stampaConAnimazione("📚 Trovati " + libri.size() + " libri per \"" + termine + "\"");
                    resultLabel.setText("Libri trovati (" + libri.size() + ")");
                    mostraRisultati(libri);
                }
            }

            @Override
            protected void failed() {
                fade.stop();
                resultLabel.setOpacity(1.0);
                stampaConAnimazione("Errore durante la ricerca: " + getException().getMessage());
                resultLabel.setText("Errore nella ricerca");
            }
        };

        new Thread(task).start();
    }

    @FXML
    private void consigliaLibri() {
        String categoria = categoriaComboBox.getValue();

        if (categoria == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Nessuna categoria");
            alert.setHeaderText("Categoria non selezionata");
            alert.setContentText("Seleziona una categoria per ricevere consigli.");
            animaDialogo(alert);
            alert.showAndWait();
            return;
        }

        resultLabel.setText("Ricerca consigli in corso...");
        resultContainer.getChildren().clear();

        // Animazione del pulsante durante la ricerca
        RotateTransition rt = new RotateTransition(Duration.millis(2000), consigliaBtn);
        rt.setByAngle(360);
        rt.setCycleCount(Animation.INDEFINITE);
        rt.play();

        consigliaBtn.setDisable(true);

        Task<List<Libro>> task = new Task<>() {
            @Override
            protected List<Libro> call() throws Exception {
                return client.consigliaLibri(categoria);
            }

            @Override
            protected void succeeded() {
                rt.stop();
                consigliaBtn.setDisable(false);

                List<Libro> libri = getValue();
                if (libri.isEmpty()) {
                    stampaConAnimazione("Nessun consiglio disponibile per la categoria: " + categoria);
                    resultLabel.setText("Nessun consiglio disponibile");
                } else {
                    stampaConAnimazione("📚 Consigliati " + libri.size() + " libri per la categoria \"" + categoria + "\"");
                    resultLabel.setText("Libri consigliati (" + libri.size() + ")");
                    mostraRisultati(libri);
                }
            }

            @Override
            protected void failed() {
                rt.stop();
                consigliaBtn.setDisable(false);
                stampaConAnimazione("Errore durante la ricerca consigli: " + getException().getMessage());
                resultLabel.setText("Errore nella ricerca");
            }
        };

        new Thread(task).start();
    }

    private void mostraRisultati(List<Libro> libri) {
        resultContainer.getChildren().clear();

        // Crea un effetto di caricamento progressivo
        PauseTransition delay = new PauseTransition(Duration.millis(50));

        for (int i = 0; i < libri.size(); i++) {
            final int index = i;
            Libro libro = libri.get(i);

            // Creare una card per il libro
            VBox bookCard = new VBox(5);
            bookCard.getStyleClass().add("book-card");
            bookCard.setPadding(new Insets(10));
            bookCard.setOpacity(0);  // Inizia invisibile

            // Titolo del libro
            Label titolo = new Label(libro.titolo());
            titolo.getStyleClass().add("book-title");
            titolo.setWrapText(true);

            // Autori
            Label autori = new Label("Autori: " + libro.autori());
            autori.getStyleClass().add("book-author");
            autori.setWrapText(true);

            // Categoria come etichetta normale
            Label categoria = new Label("Categoria: " + libro.categoria());
            categoria.getStyleClass().add("book-category");

            // Prezzo
            Label prezzo = new Label("Prezzo: €" + libro.prezzo());
            prezzo.getStyleClass().add("book-price");

            // Aggiunge tutti gli elementi alla card
            bookCard.getChildren().addAll(titolo, autori, categoria, prezzo);
            resultContainer.getChildren().add(bookCard);

            // Aggiungi evento click sulla card
            bookCard.setOnMouseClicked(event -> {
                // Crea effetto pulsazione quando cliccato
                ScaleTransition st = new ScaleTransition(Duration.millis(100), bookCard);
                st.setToX(1.03);
                st.setToY(1.03);
                st.setCycleCount(2);
                st.setAutoReverse(true);
                st.play();

                // Qui potresti aggiungere azioni per mostrare dettagli o altre interazioni
            });

            // Animazione di apparizione ritardata e sequenziale
            PauseTransition cardDelay = new PauseTransition(Duration.millis(50 * index));
            cardDelay.setOnFinished(e -> {
                FadeTransition ft = new FadeTransition(Duration.millis(300), bookCard);
                ft.setFromValue(0);
                ft.setToValue(1);
                ft.play();

                TranslateTransition tt = new TranslateTransition(Duration.millis(300), bookCard);
                tt.setFromY(20);
                tt.setToY(0);
                tt.play();
            });
            cardDelay.play();
        }
    }

    private void mostraprofilo(Utente utente) {
        // Crea un dialogo per visualizzare il profilo in modo elegante
        Dialog<Void> dialog = new Dialog<>();

        dialog.setTitle("Profilo Utente");
        dialog.setHeaderText("Informazioni Profilo");

        ButtonType closeButton = new ButtonType("Chiudi", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().add(closeButton);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        dialog.getDialogPane().getStyleClass().add("profile-dialog");
        grid.getStyleClass().add("profile-grid");

        // Campo per ogni informazione con animazioni
        addAnimatedProfileField(grid, "User ID:", String.valueOf(utente.userID()), 0);
        addAnimatedProfileField(grid, "Nome Completo:", utente.nomeCompleto(), 1);
        addAnimatedProfileField(grid, "Email:", utente.email(), 2);
        addAnimatedProfileField(grid, "Username:", utente.username(), 3);
        addAnimatedProfileField(grid, "Codice Fiscale:", utente.codiceFiscale() != null ? utente.codiceFiscale() : "Non specificato", 4);
        addAnimatedProfileField(grid, "Data Registrazione:", utente.dataRegistrazione().toString(), 5);

        dialog.getDialogPane().setContent(grid);
        animaDialogo(dialog);
        dialog.showAndWait();
    }

    private void addAnimatedProfileField(GridPane grid, String labelText, String value, int row) {
        Label label = new Label(labelText);
        label.getStyleClass().add("profile-label"); // Aggiungi la classe CSS per uniformare l'aspetto

        Label valueLabel = new Label(value);
        valueLabel.setOpacity(0);  // Inizialmente invisibile
        valueLabel.getStyleClass().add("profile-value");  // Applica la classe CSS di base

        grid.add(label, 0, row);
        grid.add(valueLabel, 1, row);

        // Animazione di apparizione ritardata
        PauseTransition delay = new PauseTransition(Duration.millis(100 * row));
        delay.setOnFinished(e -> {
            FadeTransition ft = new FadeTransition(Duration.millis(500), valueLabel);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.setCycleCount(1); // Animazione una sola volta
            ft.setInterpolator(Interpolator.LINEAR); // Animazione fluida
            ft.play();
        });
        delay.play();
    }

    // Metodo per animare un messaggio nella console
    private void stampaConAnimazione(String msg) {
        // Aggiungi il messaggio come al solito
        output.appendText(msg + "\n");

        // Scorri alla fine del testo
        output.positionCaret(output.getText().length());

        // Evidenzia brevemente il messaggio aggiunto
        int lastLineIndex = output.getText().lastIndexOf("\n", output.getText().length() - 2);
        if (lastLineIndex == -1) lastLineIndex = 0;

        int startIndex = lastLineIndex + 1;
        int endIndex = output.getText().length();

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, e -> {
                    output.setStyle("-fx-highlight-fill: #d3e5ff;");
                    output.selectRange(startIndex, endIndex);
                }),
                new KeyFrame(Duration.millis(1000), e -> {
                    output.deselect();
                    output.setStyle("");
                })
        );
        timeline.play();
    }

    private void stampa(String msg) {
        output.appendText(msg + "\n");
        output.positionCaret(output.getText().length());
    }

    private Dialog<Pair<String, String>> creaDialogoLogin() {
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Login");

        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        dialog.getDialogPane().getStyleClass().add("custom-dialog");

        ButtonType loginButtonType = new ButtonType("Login", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        grid.getStyleClass().add("custom-grid");

        TextField username = new TextField();
        username.setPromptText("username");
        username.getStyleClass().add("custom-field");

        PasswordField password = new PasswordField();
        password.setPromptText("password");
        password.getStyleClass().add("custom-field");

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

        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        dialog.getDialogPane().getStyleClass().add("custom-dialog");

        ButtonType registerButton = new ButtonType("Registrati", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(registerButton, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        grid.getStyleClass().add("custom-grid");

        TextField nome = new TextField();
        nome.setPromptText("Nome completo");
        nome.getStyleClass().add("custom-field");

        TextField email = new TextField();
        email.setPromptText("Email");
        email.getStyleClass().add("custom-field");

        TextField username = new TextField();
        username.setPromptText("Username");
        username.getStyleClass().add("custom-field");

        PasswordField password = new PasswordField();
        password.setPromptText("Password sicura");
        password.getStyleClass().add("custom-field");

        TextField cf = new TextField();
        cf.setPromptText("Codice fiscale (opzionale)");
        cf.getStyleClass().add("custom-field");

        grid.add(new Label("Nome Completo:"), 0, 0);
        grid.add(nome, 1, 0);
        grid.add(new Label("Email:"), 0, 1);
        grid.add(email, 1, 1);
        grid.add(new Label("Username:"), 0, 2);
        grid.add(username, 1, 2);
        grid.add(new Label("Password:"), 0, 3);
        grid.add(password, 1, 3);
        grid.add(new Label("Codice Fiscale:"), 0, 4);
        grid.add(cf, 1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == registerButton) {
                return List.of(
                        nome.getText(),
                        email.getText(),
                        username.getText(),
                        password.getText(),
                        cf.getText()
                );
            }
            return null;
        });
        return dialog;
    }

    private void animaDialogo(Dialog<?> dialog) {
        // Assicuriamoci che il dialogo abbia un effetto di entrata
        dialog.setOnShowing(event -> {
            Node dialogPane = dialog.getDialogPane();
            dialogPane.setOpacity(0);

            FadeTransition ft = new FadeTransition(Duration.millis(300), dialogPane);
            ft.setFromValue(0);
            ft.setToValue(1);

            ScaleTransition st = new ScaleTransition(Duration.millis(300), dialogPane);
            st.setFromX(0.9);
            st.setFromY(0.9);
            st.setToX(1);
            st.setToY(1);

            ParallelTransition pt = new ParallelTransition(ft, st);
            pt.play();
        });
    }

    private void animaCambioStato(Label label, String nuovoTesto, String classeRimuovere, String classeAggiungere) {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), label);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> {
            label.setText(nuovoTesto);
            if (classeRimuovere != null) {
                label.getStyleClass().remove(classeRimuovere);
            }
            if (classeAggiungere != null) {
                label.getStyleClass().add(classeAggiungere);
            }

            FadeTransition fadeIn = new FadeTransition(Duration.millis(200), label);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });
        fadeOut.play();
    }

    private void animaShake(Node node) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(50), node);
        tt.setFromX(0);
        tt.setByX(5);
        tt.setCycleCount(6);
        tt.setAutoReverse(true);
        tt.play();
    }

    private void updateUIState() {
        // Crea una lista di controlli che cambieranno stato
        List<Node> controlsToUpdate = List.of(
                connettiBtn, disconnettiBtn, loginBtn, registratiBtn,
                logoutBtn, cercaBtn, consigliaBtn, profiloBtn,
                searchField, categoriaComboBox
        );

        // Aggiorna lo stato dei pulsanti con animazione
        for (Node control : controlsToUpdate) {
            boolean oldDisabled = control.isDisabled();
            boolean newDisabled = false;

            // Determina il nuovo stato disabilitato
            if (control == connettiBtn) {
                newDisabled = isConnected;
            } else if (control == disconnettiBtn) {
                newDisabled = !isConnected;
            } else if (control == loginBtn || control == registratiBtn) {
                newDisabled = !isConnected || isLoggedIn;
            } else if (control == logoutBtn || control == profiloBtn || control == consigliaBtn || control == categoriaComboBox) {
                newDisabled = !isConnected || !isLoggedIn;
            } else if (control == cercaBtn || control == searchField) {
                newDisabled = !isConnected;
            }

            // Se c'è un cambiamento, anima
            if (oldDisabled != newDisabled) {
                control.setDisable(newDisabled);

                // Aggiungi una leggera animazione di opacità
                FadeTransition fade = new FadeTransition(Duration.millis(300), control);
                fade.setFromValue(newDisabled ? 1.0 : 0.7);
                fade.setToValue(newDisabled ? 0.7 : 1.0);
                fade.play();
            }
        }
    }
}