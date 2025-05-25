package me.labb.bookrecommender.client;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import javafx.util.Pair;
import me.labb.bookrecommender.client.comunicazione.ClientOperazioni;
import me.labb.bookrecommender.client.oggetti.*;

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

    // Componenti per la gestione delle librerie
    @FXML private Button creaLibreriaBtn;
    @FXML private Button aggiornaLibrerieBtn;
    @FXML private ListView<Libreria> librerieListView;
    @FXML private Label libreriaSelezionataLabel;
    @FXML private Button aggiungiLibroBtn;
    @FXML private Button rimuoviLibroBtn;
    @FXML private VBox libreriaContentContainer;

    // Componenti per la gestione delle valutazioni
    @FXML private Button valutaLibroBtn;
    @FXML private Button mieValutazioniBtn;
    @FXML private TextField libroIDValutazioniField;
    @FXML private Button cercaValutazioniBtn;
    @FXML private VBox valutazioniContainer;

    // Componenti per la gestione dei consigli
    @FXML private TextField libroIDConsigliField;
    @FXML private Button generaConsigliBtn;
    @FXML private Button mieiConsigliBtn;
    @FXML private Button salvaConsiglioBtn;
    @FXML private VBox consigliContainer;

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

        // Configura la ListView delle librerie
        setupLibrerieListView();
    }

    /**
     * Configura la ListView delle librerie con un cell factory personalizzato.
     */
    private void setupLibrerieListView() {
        // Configura il cell factory per mostrare il nome della libreria
        librerieListView.setCellFactory(param -> new ListCell<Libreria>() {
            @Override
            protected void updateItem(Libreria libreria, boolean empty) {
                super.updateItem(libreria, empty);

                if (empty || libreria == null) {
                    setText(null);
                } else {
                    setText(libreria.nomeLibreria());
                }
            }
        });

        // Aggiungi un listener per la selezione di una libreria
        librerieListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                libreriaSelezionataLabel.setText("Libreria: " + newValue.nomeLibreria());
                caricaLibriInLibreria(newValue.libreriaID());
            }
        });
    }

    /**
     * Carica i libri presenti in una libreria e li mostra nell'interfaccia.
     * 
     * @param libreriaID ID della libreria da visualizzare
     */
    private void caricaLibriInLibreria(int libreriaID) {
        libreriaContentContainer.getChildren().clear();

        // Mostra un indicatore di caricamento
        Label loadingLabel = new Label("Caricamento libri in corso...");
        loadingLabel.getStyleClass().add("loading-label");
        libreriaContentContainer.getChildren().add(loadingLabel);

        // Esegui il caricamento in un thread separato
        Task<List<Libro>> task = new Task<>() {
            @Override
            protected List<Libro> call() throws Exception {
                return client.visualizzaLibreria(libreriaID);
            }

            @Override
            protected void succeeded() {
                libreriaContentContainer.getChildren().clear();
                List<Libro> libri = getValue();

                if (libri.isEmpty()) {
                    Label emptyLabel = new Label("Questa libreria è vuota. Aggiungi dei libri!");
                    emptyLabel.getStyleClass().add("empty-label");
                    libreriaContentContainer.getChildren().add(emptyLabel);
                } else {
                    for (Libro libro : libri) {
                        libreriaContentContainer.getChildren().add(creaCardLibro(libro));
                    }
                }
            }

            @Override
            protected void failed() {
                libreriaContentContainer.getChildren().clear();
                Label errorLabel = new Label("Errore nel caricamento dei libri: " + getException().getMessage());
                errorLabel.getStyleClass().add("error-label");
                libreriaContentContainer.getChildren().add(errorLabel);
            }
        };

        new Thread(task).start();
    }

    /**
     * Crea una card per visualizzare un libro.
     * 
     * @param libro Il libro da visualizzare
     * @return Un nodo VBox che rappresenta la card del libro
     */
    private VBox creaCardLibro(Libro libro) {
        VBox bookCard = new VBox(5);
        bookCard.getStyleClass().add("book-card");
        bookCard.setPadding(new Insets(10));

        // Titolo del libro
        Label titolo = new Label(libro.titolo());
        titolo.getStyleClass().add("book-title");
        titolo.setWrapText(true);

        // Autori
        Label autori = new Label("Autori: " + libro.autori());
        autori.getStyleClass().add("book-author");
        autori.setWrapText(true);

        // Categoria
        Label categoria = new Label("Categoria: " + libro.categoria());
        categoria.getStyleClass().add("book-category");

        // Prezzo
        Label prezzo = new Label("Prezzo: €" + libro.prezzo());
        prezzo.getStyleClass().add("book-price");

        // Aggiunge tutti gli elementi alla card
        bookCard.getChildren().addAll(titolo, autori, categoria, prezzo);

        // Aggiungi effetto hover
        bookCard.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(100), bookCard);
            st.setToX(1.03);
            st.setToY(1.03);
            st.play();
        });

        bookCard.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(100), bookCard);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });

        return bookCard;
    }

    /**
     * Gestisce la creazione di una nuova libreria.
     */
    @FXML
    private void creaLibreria() {
        if (!client.isAutenticato()) {
            stampaConAnimazione("Devi effettuare il login per creare una libreria.");
            return;
        }

        // Crea un dialogo per inserire il nome della libreria
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Crea Libreria");
        dialog.setHeaderText("Crea una nuova libreria personale");
        dialog.setContentText("Nome della libreria:");

        // Aggiungi stile al dialogo
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        // Mostra il dialogo e attendi il risultato
        Optional<String> result = dialog.showAndWait();

        result.ifPresent(nomeLibreria -> {
            if (nomeLibreria.trim().isEmpty()) {
                stampaConAnimazione("Il nome della libreria non può essere vuoto.");
                return;
            }

            try {
                int libreriaID = client.creaLibreria(nomeLibreria);
                if (libreriaID > 0) {
                    stampaConAnimazione("Libreria '" + nomeLibreria + "' creata con successo (ID: " + libreriaID + ").");
                    aggiornaLibrerie();
                } else {
                    stampaConAnimazione("Errore nella creazione della libreria.");
                }
            } catch (IOException e) {
                stampaConAnimazione("Errore: " + e.getMessage());
            }
        });
    }

    /**
     * Aggiorna la lista delle librerie dell'utente.
     */
    @FXML
    private void aggiornaLibrerie() {
        if (!client.isAutenticato()) {
            stampaConAnimazione("Devi effettuare il login per visualizzare le tue librerie.");
            return;
        }

        // Disabilita il pulsante durante l'aggiornamento
        aggiornaLibrerieBtn.setDisable(true);

        // Esegui l'aggiornamento in un thread separato
        Task<List<Libreria>> task = new Task<>() {
            @Override
            protected List<Libreria> call() throws Exception {
                return client.elencaLibrerie();
            }

            @Override
            protected void succeeded() {
                List<Libreria> librerie = getValue();
                librerieListView.getItems().clear();

                if (librerie.isEmpty()) {
                    stampaConAnimazione("Non hai ancora creato librerie.");
                } else {
                    librerieListView.getItems().addAll(librerie);
                    stampaConAnimazione("Trovate " + librerie.size() + " librerie.");
                }

                aggiornaLibrerieBtn.setDisable(false);
            }

            @Override
            protected void failed() {
                stampaConAnimazione("Errore nell'aggiornamento delle librerie: " + getException().getMessage());
                aggiornaLibrerieBtn.setDisable(false);
            }
        };

        new Thread(task).start();
    }

    /**
     * Gestisce l'aggiunta di un libro a una libreria.
     */
    @FXML
    private void aggiungiLibroALibreria() {
        if (!client.isAutenticato()) {
            stampaConAnimazione("Devi effettuare il login per aggiungere libri alle librerie.");
            return;
        }

        Libreria libreriaSelezionata = librerieListView.getSelectionModel().getSelectedItem();
        if (libreriaSelezionata == null) {
            stampaConAnimazione("Seleziona prima una libreria.");
            return;
        }

        // Crea un dialogo per inserire l'ID del libro
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Aggiungi Libro");
        dialog.setHeaderText("Aggiungi un libro alla libreria '" + libreriaSelezionata.nomeLibreria() + "'");
        dialog.setContentText("ID del libro:");

        // Aggiungi stile al dialogo
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        // Mostra il dialogo e attendi il risultato
        Optional<String> result = dialog.showAndWait();

        result.ifPresent(libroIDStr -> {
            try {
                int libroID = Integer.parseInt(libroIDStr.trim());

                boolean success = client.aggiungiLibroALibreria(libreriaSelezionata.libreriaID(), libroID);
                if (success) {
                    stampaConAnimazione("Libro aggiunto alla libreria con successo.");
                    // Aggiorna la visualizzazione della libreria
                    caricaLibriInLibreria(libreriaSelezionata.libreriaID());
                } else {
                    stampaConAnimazione("Errore nell'aggiunta del libro alla libreria.");
                }
            } catch (NumberFormatException e) {
                stampaConAnimazione("ID libro non valido. Inserisci un numero intero.");
            } catch (IOException e) {
                stampaConAnimazione("Errore: " + e.getMessage());
            }
        });
    }

    /**
     * Gestisce la rimozione di un libro da una libreria.
     */
    @FXML
    private void rimuoviLibroDaLibreria() {
        if (!client.isAutenticato()) {
            stampaConAnimazione("Devi effettuare il login per rimuovere libri dalle librerie.");
            return;
        }

        Libreria libreriaSelezionata = librerieListView.getSelectionModel().getSelectedItem();
        if (libreriaSelezionata == null) {
            stampaConAnimazione("Seleziona prima una libreria.");
            return;
        }

        // Crea un dialogo per inserire l'ID del libro
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Rimuovi Libro");
        dialog.setHeaderText("Rimuovi un libro dalla libreria '" + libreriaSelezionata.nomeLibreria() + "'");
        dialog.setContentText("ID del libro:");

        // Aggiungi stile al dialogo
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        // Mostra il dialogo e attendi il risultato
        Optional<String> result = dialog.showAndWait();

        result.ifPresent(libroIDStr -> {
            try {
                int libroID = Integer.parseInt(libroIDStr.trim());

                boolean success = client.rimuoviLibroDaLibreria(libreriaSelezionata.libreriaID(), libroID);
                if (success) {
                    stampaConAnimazione("Libro rimosso dalla libreria con successo.");
                    // Aggiorna la visualizzazione della libreria
                    caricaLibriInLibreria(libreriaSelezionata.libreriaID());
                } else {
                    stampaConAnimazione("Errore nella rimozione del libro dalla libreria.");
                }
            } catch (NumberFormatException e) {
                stampaConAnimazione("ID libro non valido. Inserisci un numero intero.");
            } catch (IOException e) {
                stampaConAnimazione("Errore: " + e.getMessage());
            }
        });
    }

    /**
     * Mostra un form per valutare un libro.
     */
    @FXML
    private void mostraFormValutazione() {
        if (!client.isAutenticato()) {
            stampaConAnimazione("Devi effettuare il login per valutare un libro.");
            return;
        }

        // Crea un dialogo per inserire l'ID del libro e i punteggi
        Dialog<Valutazione> dialog = new Dialog<>();
        dialog.setTitle("Valuta Libro");
        dialog.setHeaderText("Valuta un libro");

        // Aggiungi stile al dialogo
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        // Configura i pulsanti
        ButtonType valutaButtonType = new ButtonType("Valuta", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(valutaButtonType, ButtonType.CANCEL);

        // Crea la griglia per i campi
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // Campo per l'ID del libro
        TextField libroIDField = new TextField();
        libroIDField.setPromptText("ID del libro");

        // Campi per i punteggi e le note
        Spinner<Integer> scoreStileSpinner = new Spinner<>(1, 5, 3);
        scoreStileSpinner.setEditable(true);
        TextField noteStileField = new TextField();
        noteStileField.setPromptText("Note sullo stile");

        Spinner<Integer> scoreContenutoSpinner = new Spinner<>(1, 5, 3);
        scoreContenutoSpinner.setEditable(true);
        TextField noteContenutoField = new TextField();
        noteContenutoField.setPromptText("Note sul contenuto");

        Spinner<Integer> scoreGradevolezzaSpinner = new Spinner<>(1, 5, 3);
        scoreGradevolezzaSpinner.setEditable(true);
        TextField noteGradevolezzaField = new TextField();
        noteGradevolezzaField.setPromptText("Note sulla gradevolezza");

        Spinner<Integer> scoreOriginalitaSpinner = new Spinner<>(1, 5, 3);
        scoreOriginalitaSpinner.setEditable(true);
        TextField noteOriginalitaField = new TextField();
        noteOriginalitaField.setPromptText("Note sull'originalità");

        Spinner<Integer> scoreEdizioneSpinner = new Spinner<>(1, 5, 3);
        scoreEdizioneSpinner.setEditable(true);
        TextField noteEdizioneField = new TextField();
        noteEdizioneField.setPromptText("Note sull'edizione");

        // Aggiungi i campi alla griglia
        grid.add(new Label("ID Libro:"), 0, 0);
        grid.add(libroIDField, 1, 0);

        grid.add(new Label("Stile (1-5):"), 0, 1);
        grid.add(scoreStileSpinner, 1, 1);
        grid.add(new Label("Note:"), 2, 1);
        grid.add(noteStileField, 3, 1);

        grid.add(new Label("Contenuto (1-5):"), 0, 2);
        grid.add(scoreContenutoSpinner, 1, 2);
        grid.add(new Label("Note:"), 2, 2);
        grid.add(noteContenutoField, 3, 2);

        grid.add(new Label("Gradevolezza (1-5):"), 0, 3);
        grid.add(scoreGradevolezzaSpinner, 1, 3);
        grid.add(new Label("Note:"), 2, 3);
        grid.add(noteGradevolezzaField, 3, 3);

        grid.add(new Label("Originalità (1-5):"), 0, 4);
        grid.add(scoreOriginalitaSpinner, 1, 4);
        grid.add(new Label("Note:"), 2, 4);
        grid.add(noteOriginalitaField, 3, 4);

        grid.add(new Label("Edizione (1-5):"), 0, 5);
        grid.add(scoreEdizioneSpinner, 1, 5);
        grid.add(new Label("Note:"), 2, 5);
        grid.add(noteEdizioneField, 3, 5);

        dialog.getDialogPane().setContent(grid);

        // Richiedi il focus sul campo ID libro
        Platform.runLater(libroIDField::requestFocus);

        // Converti il risultato
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == valutaButtonType) {
                try {
                    int libroID = Integer.parseInt(libroIDField.getText().trim());
                    short scoreStile = scoreStileSpinner.getValue().shortValue();
                    String noteStile = noteStileField.getText();
                    short scoreContenuto = scoreContenutoSpinner.getValue().shortValue();
                    String noteContenuto = noteContenutoField.getText();
                    short scoreGradevolezza = scoreGradevolezzaSpinner.getValue().shortValue();
                    String noteGradevolezza = noteGradevolezzaField.getText();
                    short scoreOriginalita = scoreOriginalitaSpinner.getValue().shortValue();
                    String noteOriginalita = noteOriginalitaField.getText();
                    short scoreEdizione = scoreEdizioneSpinner.getValue().shortValue();
                    String noteEdizione = noteEdizioneField.getText();

                    // Crea un oggetto Valutazione temporaneo (senza ID e data)
                    return new Valutazione(
                        0,
                        client.getUtenteAutenticato().userID(),
                        libroID,
                        scoreStile,
                        noteStile,
                        scoreContenuto,
                        noteContenuto,
                        scoreGradevolezza,
                        noteGradevolezza,
                        scoreOriginalita,
                        noteOriginalita,
                        scoreEdizione,
                        noteEdizione,
                        null
                    );
                } catch (NumberFormatException e) {
                    stampaConAnimazione("ID libro non valido. Inserisci un numero intero.");
                    return null;
                }
            }
            return null;
        });

        // Mostra il dialogo e processa il risultato
        Optional<Valutazione> result = dialog.showAndWait();

        result.ifPresent(valutazione -> {
            try {
                int valutazioneID = client.valutaLibro(
                    valutazione.libroID(),
                    valutazione.scoreStile(),
                    valutazione.noteStile(),
                    valutazione.scoreContenuto(),
                    valutazione.noteContenuto(),
                    valutazione.scoreGradevolezza(),
                    valutazione.noteGradevolezza(),
                    valutazione.scoreOriginalita(),
                    valutazione.noteOriginalita(),
                    valutazione.scoreEdizione(),
                    valutazione.noteEdizione()
                );

                if (valutazioneID > 0) {
                    stampaConAnimazione("Valutazione salvata con successo (ID: " + valutazioneID + ").");
                } else {
                    stampaConAnimazione("Errore nel salvataggio della valutazione.");
                }
            } catch (IOException e) {
                stampaConAnimazione("Errore: " + e.getMessage());
            } catch (IllegalArgumentException e) {
                stampaConAnimazione("Errore: " + e.getMessage());
            }
        });
    }

    /**
     * Visualizza le valutazioni dell'utente autenticato.
     */
    @FXML
    private void visualizzaMieValutazioni() {
        if (!client.isAutenticato()) {
            stampaConAnimazione("Devi effettuare il login per visualizzare le tue valutazioni.");
            return;
        }

        valutazioniContainer.getChildren().clear();

        // Mostra un indicatore di caricamento
        Label loadingLabel = new Label("Caricamento valutazioni in corso...");
        loadingLabel.getStyleClass().add("loading-label");
        valutazioniContainer.getChildren().add(loadingLabel);

        // Esegui il caricamento in un thread separato
        Task<List<Valutazione>> task = new Task<>() {
            @Override
            protected List<Valutazione> call() throws Exception {
                return client.visualizzaMieValutazioni();
            }

            @Override
            protected void succeeded() {
                valutazioniContainer.getChildren().clear();
                List<Valutazione> valutazioni = getValue();

                if (valutazioni.isEmpty()) {
                    Label emptyLabel = new Label("Non hai ancora valutato nessun libro.");
                    emptyLabel.getStyleClass().add("empty-label");
                    valutazioniContainer.getChildren().add(emptyLabel);
                } else {
                    for (Valutazione valutazione : valutazioni) {
                        valutazioniContainer.getChildren().add(creaCardValutazione(valutazione));
                    }
                }
            }

            @Override
            protected void failed() {
                valutazioniContainer.getChildren().clear();
                Label errorLabel = new Label("Errore nel caricamento delle valutazioni: " + getException().getMessage());
                errorLabel.getStyleClass().add("error-label");
                valutazioniContainer.getChildren().add(errorLabel);
            }
        };

        new Thread(task).start();
    }

    /**
     * Cerca le valutazioni di un libro specifico.
     */
    @FXML
    private void cercaValutazioniLibro() {
        if (!client.isAutenticato()) {
            stampaConAnimazione("Devi effettuare il login per visualizzare le valutazioni.");
            return;
        }

        String libroIDStr = libroIDValutazioniField.getText().trim();
        if (libroIDStr.isEmpty()) {
            stampaConAnimazione("Inserisci l'ID del libro di cui visualizzare le valutazioni.");
            return;
        }

        try {
            int libroID = Integer.parseInt(libroIDStr);

            valutazioniContainer.getChildren().clear();

            // Mostra un indicatore di caricamento
            Label loadingLabel = new Label("Caricamento valutazioni in corso...");
            loadingLabel.getStyleClass().add("loading-label");
            valutazioniContainer.getChildren().add(loadingLabel);

            // Esegui il caricamento in un thread separato
            Task<List<Valutazione>> task = new Task<>() {
                @Override
                protected List<Valutazione> call() throws Exception {
                    return client.visualizzaValutazioniLibro(libroID);
                }

                @Override
                protected void succeeded() {
                    valutazioniContainer.getChildren().clear();
                    List<Valutazione> valutazioni = getValue();

                    if (valutazioni.isEmpty()) {
                        Label emptyLabel = new Label("Nessuna valutazione trovata per questo libro.");
                        emptyLabel.getStyleClass().add("empty-label");
                        valutazioniContainer.getChildren().add(emptyLabel);
                    } else {
                        for (Valutazione valutazione : valutazioni) {
                            valutazioniContainer.getChildren().add(creaCardValutazione(valutazione));
                        }
                    }
                }

                @Override
                protected void failed() {
                    valutazioniContainer.getChildren().clear();
                    Label errorLabel = new Label("Errore nel caricamento delle valutazioni: " + getException().getMessage());
                    errorLabel.getStyleClass().add("error-label");
                    valutazioniContainer.getChildren().add(errorLabel);
                }
            };

            new Thread(task).start();
        } catch (NumberFormatException e) {
            stampaConAnimazione("ID libro non valido. Inserisci un numero intero.");
        }
    }

    /**
     * Crea una card per visualizzare una valutazione.
     * 
     * @param valutazione La valutazione da visualizzare
     * @return Un nodo VBox che rappresenta la card della valutazione
     */
    private VBox creaCardValutazione(Valutazione valutazione) {
        VBox card = new VBox(5);
        card.getStyleClass().add("valutazione-card");
        card.setPadding(new Insets(10));

        // Intestazione
        Label header = new Label("Valutazione Libro ID: " + valutazione.libroID());
        header.getStyleClass().add("valutazione-header");

        // Punteggi
        HBox punteggiBox = new HBox(15);
        punteggiBox.getChildren().addAll(
            creaPunteggioBox("Stile", valutazione.scoreStile()),
            creaPunteggioBox("Contenuto", valutazione.scoreContenuto()),
            creaPunteggioBox("Gradevolezza", valutazione.scoreGradevolezza()),
            creaPunteggioBox("Originalità", valutazione.scoreOriginalita()),
            creaPunteggioBox("Edizione", valutazione.scoreEdizione())
        );

        // Note
        VBox noteBox = new VBox(5);
        if (!valutazione.noteStile().isEmpty()) {
            noteBox.getChildren().add(new Label("Note Stile: " + valutazione.noteStile()));
        }
        if (!valutazione.noteContenuto().isEmpty()) {
            noteBox.getChildren().add(new Label("Note Contenuto: " + valutazione.noteContenuto()));
        }
        if (!valutazione.noteGradevolezza().isEmpty()) {
            noteBox.getChildren().add(new Label("Note Gradevolezza: " + valutazione.noteGradevolezza()));
        }
        if (!valutazione.noteOriginalita().isEmpty()) {
            noteBox.getChildren().add(new Label("Note Originalità: " + valutazione.noteOriginalita()));
        }
        if (!valutazione.noteEdizione().isEmpty()) {
            noteBox.getChildren().add(new Label("Note Edizione: " + valutazione.noteEdizione()));
        }

        // Data
        Label dataLabel = new Label("Data: " + (valutazione.dataValutazione() != null ? valutazione.dataValutazione().toString() : "N/A"));
        dataLabel.getStyleClass().add("valutazione-data");

        // Aggiungi tutti gli elementi alla card
        card.getChildren().addAll(header, punteggiBox, noteBox, dataLabel);

        // Aggiungi effetto hover
        card.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(100), card);
            st.setToX(1.02);
            st.setToY(1.02);
            st.play();
        });

        card.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(100), card);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });

        return card;
    }

    /**
     * Crea un box per visualizzare un punteggio.
     * 
     * @param categoria La categoria del punteggio
     * @param punteggio Il punteggio (1-5)
     * @return Un nodo VBox che rappresenta il box del punteggio
     */
    private VBox creaPunteggioBox(String categoria, short punteggio) {
        VBox box = new VBox(2);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("punteggio-box");

        Label categoriaLabel = new Label(categoria);
        categoriaLabel.getStyleClass().add("punteggio-categoria");

        Label punteggioLabel = new Label(String.valueOf(punteggio));
        punteggioLabel.getStyleClass().add("punteggio-valore");

        // Stelle
        HBox stelleBox = new HBox(2);
        stelleBox.setAlignment(Pos.CENTER);
        for (int i = 0; i < 5; i++) {
            Label stella = new Label(i < punteggio ? "★" : "☆");
            stella.getStyleClass().add(i < punteggio ? "stella-piena" : "stella-vuota");
            stelleBox.getChildren().add(stella);
        }

        box.getChildren().addAll(categoriaLabel, stelleBox);
        return box;
    }

    /**
     * Genera consigli personalizzati per un libro specifico.
     */
    @FXML
    private void generaConsigli() {
        if (!client.isAutenticato()) {
            stampaConAnimazione("Devi effettuare il login per generare consigli.");
            return;
        }

        String libroIDStr = libroIDConsigliField.getText().trim();
        if (libroIDStr.isEmpty()) {
            stampaConAnimazione("Inserisci l'ID del libro per cui generare consigli.");
            return;
        }

        try {
            int libroID = Integer.parseInt(libroIDStr);

            consigliContainer.getChildren().clear();

            // Mostra un indicatore di caricamento
            Label loadingLabel = new Label("Generazione consigli in corso...");
            loadingLabel.getStyleClass().add("loading-label");
            consigliContainer.getChildren().add(loadingLabel);

            // Esegui la generazione in un thread separato
            Task<List<Libro>> task = new Task<>() {
                @Override
                protected List<Libro> call() throws Exception {
                    return client.generaConsigli(libroID);
                }

                @Override
                protected void succeeded() {
                    consigliContainer.getChildren().clear();
                    List<Libro> libri = getValue();

                    if (libri.isEmpty()) {
                        Label emptyLabel = new Label("Nessun consiglio disponibile per questo libro.");
                        emptyLabel.getStyleClass().add("empty-label");
                        consigliContainer.getChildren().add(emptyLabel);
                    } else {
                        // Aggiungi un'etichetta con il libro di riferimento
                        Label refLabel = new Label("Consigli per il libro ID: " + libroID);
                        refLabel.getStyleClass().add("section-title");
                        consigliContainer.getChildren().add(refLabel);

                        // Aggiungi le card dei libri consigliati
                        for (Libro libro : libri) {
                            VBox bookCard = creaCardLibroConsigliato(libro, libroID);
                            consigliContainer.getChildren().add(bookCard);
                        }
                    }
                }

                @Override
                protected void failed() {
                    consigliContainer.getChildren().clear();
                    Label errorLabel = new Label("Errore nella generazione dei consigli: " + getException().getMessage());
                    errorLabel.getStyleClass().add("error-label");
                    consigliContainer.getChildren().add(errorLabel);
                }
            };

            new Thread(task).start();
        } catch (NumberFormatException e) {
            stampaConAnimazione("ID libro non valido. Inserisci un numero intero.");
        }
    }

    /**
     * Visualizza i consigli salvati dall'utente autenticato.
     */
    @FXML
    private void visualizzaMieiConsigli() {
        if (!client.isAutenticato()) {
            stampaConAnimazione("Devi effettuare il login per visualizzare i tuoi consigli.");
            return;
        }

        consigliContainer.getChildren().clear();

        // Mostra un indicatore di caricamento
        Label loadingLabel = new Label("Caricamento consigli in corso...");
        loadingLabel.getStyleClass().add("loading-label");
        consigliContainer.getChildren().add(loadingLabel);

        // Esegui il caricamento in un thread separato
        Task<List<Consiglio>> task = new Task<>() {
            @Override
            protected List<Consiglio> call() throws Exception {
                return client.visualizzaMieiConsigli();
            }

            @Override
            protected void succeeded() {
                consigliContainer.getChildren().clear();
                List<Consiglio> consigli = getValue();

                if (consigli.isEmpty()) {
                    Label emptyLabel = new Label("Non hai ancora salvato nessun consiglio.");
                    emptyLabel.getStyleClass().add("empty-label");
                    consigliContainer.getChildren().add(emptyLabel);
                } else {
                    // Aggiungi un'etichetta con il numero di consigli
                    Label titleLabel = new Label("I tuoi consigli salvati (" + consigli.size() + ")");
                    titleLabel.getStyleClass().add("section-title");
                    consigliContainer.getChildren().add(titleLabel);

                    // Aggiungi le card dei consigli
                    for (Consiglio consiglio : consigli) {
                        VBox consiglioCard = creaCardConsiglio(consiglio);
                        consigliContainer.getChildren().add(consiglioCard);
                    }
                }
            }

            @Override
            protected void failed() {
                consigliContainer.getChildren().clear();
                Label errorLabel = new Label("Errore nel caricamento dei consigli: " + getException().getMessage());
                errorLabel.getStyleClass().add("error-label");
                consigliContainer.getChildren().add(errorLabel);
            }
        };

        new Thread(task).start();
    }

    /**
     * Salva un consiglio di libro.
     */
    @FXML
    private void salvaConsiglio() {
        if (!client.isAutenticato()) {
            stampaConAnimazione("Devi effettuare il login per salvare un consiglio.");
            return;
        }

        // Crea un dialogo per inserire gli ID dei libri
        Dialog<Pair<Integer, Integer>> dialog = new Dialog<>();
        dialog.setTitle("Salva Consiglio");
        dialog.setHeaderText("Salva un consiglio di libro");

        // Aggiungi stile al dialogo
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        // Configura i pulsanti
        ButtonType salvaButtonType = new ButtonType("Salva", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(salvaButtonType, ButtonType.CANCEL);

        // Crea la griglia per i campi
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // Campi per gli ID dei libri
        TextField libroRiferimentoIDField = new TextField();
        libroRiferimentoIDField.setPromptText("ID del libro di riferimento");

        TextField libroSuggeritoIDField = new TextField();
        libroSuggeritoIDField.setPromptText("ID del libro suggerito");

        // Aggiungi i campi alla griglia
        grid.add(new Label("ID Libro Riferimento:"), 0, 0);
        grid.add(libroRiferimentoIDField, 1, 0);
        grid.add(new Label("ID Libro Suggerito:"), 0, 1);
        grid.add(libroSuggeritoIDField, 1, 1);

        dialog.getDialogPane().setContent(grid);

        // Richiedi il focus sul primo campo
        Platform.runLater(libroRiferimentoIDField::requestFocus);

        // Converti il risultato
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == salvaButtonType) {
                try {
                    int libroRiferimentoID = Integer.parseInt(libroRiferimentoIDField.getText().trim());
                    int libroSuggeritoID = Integer.parseInt(libroSuggeritoIDField.getText().trim());

                    return new Pair<>(libroRiferimentoID, libroSuggeritoID);
                } catch (NumberFormatException e) {
                    stampaConAnimazione("ID libro non valido. Inserisci numeri interi.");
                    return null;
                }
            }
            return null;
        });

        // Mostra il dialogo e processa il risultato
        Optional<Pair<Integer, Integer>> result = dialog.showAndWait();

        result.ifPresent(pair -> {
            try {
                int consiglioID = client.salvaConsiglio(pair.getKey(), pair.getValue());

                if (consiglioID > 0) {
                    stampaConAnimazione("Consiglio salvato con successo (ID: " + consiglioID + ").");
                } else {
                    stampaConAnimazione("Errore nel salvataggio del consiglio.");
                }
            } catch (IOException e) {
                stampaConAnimazione("Errore: " + e.getMessage());
            }
        });
    }

    /**
     * Crea una card per visualizzare un libro consigliato con un pulsante per salvare il consiglio.
     * 
     * @param libro Il libro consigliato
     * @param libroRiferimentoID L'ID del libro di riferimento
     * @return Un nodo VBox che rappresenta la card del libro consigliato
     */
    private VBox creaCardLibroConsigliato(Libro libro, int libroRiferimentoID) {
        VBox bookCard = creaCardLibro(libro);

        // Aggiungi un pulsante per salvare il consiglio
        Button salvaConsiglioBtn = new Button("Salva Consiglio");
        salvaConsiglioBtn.getStyleClass().add("action-button");

        salvaConsiglioBtn.setOnAction(e -> {
            try {
                int consiglioID = client.salvaConsiglio(libroRiferimentoID, 0); // Assumiamo che l'ID del libro sia 0 per ora

                if (consiglioID > 0) {
                    stampaConAnimazione("Consiglio salvato con successo (ID: " + consiglioID + ").");
                } else {
                    stampaConAnimazione("Errore nel salvataggio del consiglio.");
                }
            } catch (IOException ex) {
                stampaConAnimazione("Errore: " + ex.getMessage());
            }
        });

        bookCard.getChildren().add(salvaConsiglioBtn);

        return bookCard;
    }

    /**
     * Crea una card per visualizzare un consiglio.
     * 
     * @param consiglio Il consiglio da visualizzare
     * @return Un nodo VBox che rappresenta la card del consiglio
     */
    private VBox creaCardConsiglio(Consiglio consiglio) {
        VBox card = new VBox(5);
        card.getStyleClass().add("consiglio-card");
        card.setPadding(new Insets(10));

        // Intestazione
        Label header = new Label("Consiglio ID: " + consiglio.consiglioID());
        header.getStyleClass().add("consiglio-header");

        // Dettagli
        Label libroRiferimentoLabel = new Label("Libro Riferimento ID: " + consiglio.libroRiferimentoID());
        Label libroSuggeritoLabel = new Label("Libro Suggerito ID: " + consiglio.libroSuggeritoID());

        // Data
        Label dataLabel = new Label("Data: " + consiglio.dataSuggerimento().toString());
        dataLabel.getStyleClass().add("consiglio-data");

        // Aggiungi tutti gli elementi alla card
        card.getChildren().addAll(header, libroRiferimentoLabel, libroSuggeritoLabel, dataLabel);

        // Aggiungi effetto hover
        card.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(100), card);
            st.setToX(1.02);
            st.setToY(1.02);
            st.play();
        });

        card.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(100), card);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });

        return card;
    }

    /**
     * Configura il ComboBox per la selezione delle categorie di libri.
     */
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

    /**
     * Configura le animazioni per i pulsanti dell'interfaccia.
     */
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

    /**
     * Connette il client al server e aggiorna lo stato dell'interfaccia.
     */
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

    /**
     * Disconnette il client dal server e aggiorna lo stato dell'interfaccia.
     */
    @FXML
    private void disconnetti() {
        client.chiudi();
        isConnected = false;
        isLoggedIn = false;
        animaCambioStato(statusLabel, "Non connesso", "status-connected", "status-disconnected");
        stampaConAnimazione("🔌 Disconnesso dal server.");
        updateUIState();
    }

    /**
     * Effettua il login dell'utente autenticato.
     */
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

    /**
     * Registra un nuovo utente.
     */
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
                    alert.getDialogPane().getStyleClass().add("registration-alert");

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

    /**
     * Effettua il logout dell'utente autenticato.
     */
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

    /**
     * Visualizza il profilo dell'utente autenticato.
     */
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

    /**
     * Cerca libri in base al termine inserito nel campo di ricerca.
     */
    @FXML
    private void cercaLibri() {
        String termine = searchField.getText().trim();

        if (termine.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Campo vuoto");
            alert.setHeaderText("Nessun termine di ricerca");
            alert.setContentText("Inserisci un termine di ricerca.");
            // Aggiungi la classe CSS all'Alert
            alert.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
            alert.getDialogPane().getStyleClass().add("registration-alert");
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

    /**
     * Consiglia libri in base alla categoria selezionata.
     */
    @FXML
    private void consigliaLibri() {
        String categoria = categoriaComboBox.getValue();

        if (categoria == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Nessuna categoria");
            alert.setHeaderText("Categoria non selezionata");
            alert.setContentText("Seleziona una categoria per ricevere consigli.");
            // Aggiungi la classe CSS all'Alert
            alert.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
            alert.getDialogPane().getStyleClass().add("registration-alert");
            animaDialogo(alert);
            alert.showAndWait();
            return;
        }

        resultLabel.setText("Ricerca consigli in corso...");
        resultContainer.getChildren().clear();

        consigliaBtn.setDisable(true);

        Task<List<Libro>> task = new Task<>() {
            @Override
            protected List<Libro> call() throws Exception {
                return client.consigliaLibri(categoria);
            }

            @Override
            protected void succeeded() {
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
                consigliaBtn.setDisable(false);
                stampaConAnimazione("Errore durante la ricerca consigli: " + getException().getMessage());
                resultLabel.setText("Errore nella ricerca");
            }
        };

        new Thread(task).start();
    }

    /**
     * Mostra i risultati della ricerca o dei consigli in modo elegante con animazioni.
     *
     * @param libri La lista di libri da visualizzare
     */
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
            bookCard.setOpacity(0);

            // Titolo del libro
            Label titolo = new Label(libro.titolo());
            titolo.getStyleClass().add("book-title");
            titolo.setWrapText(true);

            // Autori
            Label autori = new Label("Autori: " + libro.autori());
            autori.getStyleClass().add("book-author");
            autori.setWrapText(true);

            // Categoria come etichetta normale
            String testoCategoria = libro.categoria();
            if (testoCategoria == null || testoCategoria.isEmpty()) testoCategoria = "Nessuna categoria";
            Label categoria = new Label("Categoria: " + testoCategoria);
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

    /**
     * Mostra il profilo dell'utente in un dialogo elegante con animazioni.
     *
     * @param utente L'utente di cui mostrare il profilo
     */
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

    /**
     * Aggiunge un campo al profilo con animazione di apparizione.
     *
     * @param grid Il GridPane in cui aggiungere il campo
     * @param labelText Il testo dell'etichetta del campo
     * @param value Il valore del campo da visualizzare
     * @param row La riga in cui posizionare il campo nel GridPane
     */
    private void addAnimatedProfileField(GridPane grid, String labelText, String value, int row) {
        Label label = new Label(labelText);
        label.getStyleClass().add("profile-label");

        Label valueLabel = new Label(value);
        valueLabel.setOpacity(0);
        valueLabel.getStyleClass().add("profile-value");

        grid.add(label, 0, row);
        grid.add(valueLabel, 1, row);

        // Animazione di apparizione ritardata
        PauseTransition delay = new PauseTransition(Duration.millis(100 * row));
        delay.setOnFinished(e -> {
            FadeTransition ft = new FadeTransition(Duration.millis(500), valueLabel);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.setCycleCount(1);
            ft.setInterpolator(Interpolator.LINEAR);
            ft.play();
        });
        delay.play();
    }

    /**
     * Stampa un messaggio con un'animazione di evidenziazione.
     *
     * @param msg Il messaggio da stampare
     */
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

    /**
     * Crea un dialogo per il login dell'utente.
     *
     * @return Un dialogo configurato per il login
     */
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

    /**
     * Crea un dialogo per la registrazione di un nuovo utente.
     *
     * @return Un dialogo configurato per la registrazione
     */
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

    /**
     * Esegue un'animazione di entrata per un dialogo.
     *
     * @param dialog Il dialogo su cui eseguire l'animazione
     */
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

    /**
     * Esegue un'animazione di cambio stato su un'etichetta.
     *
     * @param label L'etichetta su cui eseguire l'animazione
     * @param nuovoTesto Il nuovo testo da impostare sull'etichetta
     * @param classeRimuovere La classe da rimuovere dallo stile dell'etichetta
     * @param classeAggiungere La classe da aggiungere allo stile dell'etichetta
     */
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

    /**
     * Esegue un'animazione di shake su un nodo specificato.
     *
     * @param node Il nodo su cui eseguire l'animazione di shake.
     */
    private void animaShake(Node node) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(50), node);
        tt.setFromX(0);
        tt.setByX(5);
        tt.setCycleCount(6);
        tt.setAutoReverse(true);
        tt.play();
    }

    /**
     * Aggiorna lo stato dell'interfaccia utente in base alla connessione e al login.
     */
    private void updateUIState() {
        // Crea una lista di controlli che cambieranno stato
        List<Node> controlsToUpdate = List.of(
                connettiBtn, disconnettiBtn, loginBtn, registratiBtn,
                logoutBtn, cercaBtn, consigliaBtn, profiloBtn,
                searchField, categoriaComboBox,
                // Nuovi controlli per le librerie
                creaLibreriaBtn, aggiornaLibrerieBtn, aggiungiLibroBtn, rimuoviLibroBtn,
                // Nuovi controlli per le valutazioni
                valutaLibroBtn, mieValutazioniBtn, cercaValutazioniBtn, libroIDValutazioniField,
                // Nuovi controlli per i consigli
                generaConsigliBtn, mieiConsigliBtn, salvaConsiglioBtn, libroIDConsigliField
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
            // Controlli per le librerie
            else if (control == creaLibreriaBtn || control == aggiornaLibrerieBtn || 
                     control == aggiungiLibroBtn || control == rimuoviLibroBtn) {
                newDisabled = !isConnected || !isLoggedIn;
            }
            // Controlli per le valutazioni
            else if (control == valutaLibroBtn || control == mieValutazioniBtn || 
                     control == cercaValutazioniBtn || control == libroIDValutazioniField) {
                newDisabled = !isConnected || !isLoggedIn;
            }
            // Controlli per i consigli
            else if (control == generaConsigliBtn || control == mieiConsigliBtn || 
                     control == salvaConsiglioBtn || control == libroIDConsigliField) {
                newDisabled = !isConnected || !isLoggedIn;
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
