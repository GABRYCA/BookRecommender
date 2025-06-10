package me.labb.bookrecommender.client;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.Pair;
import me.labb.bookrecommender.client.comunicazione.ClientOperazioni;
import me.labb.bookrecommender.client.oggetti.*;

import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller principale per l'interfaccia grafica del client BookRecommender.
 *
 * @author Caretti Gabriele 756564 VA
 * @author Como Riccardo 758697 VA
 * @author Manicone Giorgia 758716 VA
 */
public class ClientController implements Initializable {
    // Cache locale dei libri per mostrare titolo/autore nelle valutazioni
    private final Map<Integer, Libro> libriCache = new HashMap<>();

    @FXML
    private Label statusLabel;
    @FXML
    private Button connettiBtn;
    @FXML
    private Button disconnettiBtn;
    @FXML
    private Button loginBtn;
    @FXML
    private Button registratiBtn;
    @FXML
    private Button logoutBtn;
    @FXML
    private Button cercaBtn;
    @FXML
    private Button cercaCategorieBtn;
    @FXML
    private ComboBox<String> categoryComboBox;
    @FXML
    private Button profiloBtn;
    @FXML
    private TextArea output;
    @FXML
    private TextField searchField;
    @FXML
    private VBox resultContainer;
    @FXML
    private Label resultLabel;
    @FXML
    private TabPane mainTabPane;    // Componenti per la gestione delle librerie
    @FXML
    private Button creaLibreriaBtn;
    @FXML
    private Button rinominaLibreriaBtn;
    @FXML
    private Button EliminaLibreriaBtn;
    @FXML
    private Button aggiornaLibrerieBtn;
    @FXML
    private ListView<Libreria> librerieListView;
    @FXML
    private Label libreriaSelezionataLabel;
    @FXML
    private Button aggiungiLibroBtn;
    @FXML
    private Button rimuoviLibroBtn;
    @FXML
    private VBox libreriaContentContainer;

    // Componenti per la gestione delle valutazioni
    @FXML
    private Button valutaLibroBtn;
    @FXML
    private Button mieValutazioniBtn;
    @FXML
    private TextField libroIDValutazioniField;
    @FXML
    private Button cercaValutazioniBtn;
    @FXML
    private HBox valutazioniContainer;


    // Componenti per la gestione dei consigli
    @FXML
    private TextField libroIDConsigliField;
    @FXML
    private Button generaConsigliBtn;
    @FXML
    private Button mieiConsigliBtn;
    @FXML
    private Button salvaConsiglioBtn;
    @FXML
    private VBox consigliContainer;

    private ClientOperazioni client;
    private boolean isConnected = false;
    private boolean isLoggedIn = false;
    private Utente utente;

    /**
     * Wrapper per Libreria per migliore visualizzazione nei dialoghi.
     */
    private static class LibreriaDisplay {
        private final Libreria libreria;

        public LibreriaDisplay(Libreria libreria) {
            this.libreria = libreria;
        }

        public Libreria getLibreria() {
            return libreria;
        }

        @Override
        public String toString() {
            return libreria.nomeLibreria();
        }
    }

    /**
     * Classe wrapper per Libro per fornire una migliore visualizzazione toString() nei dialoghi di scelta.
     */
    private static class LibroDisplay {
        private final Libro libro;

        public LibroDisplay(Libro libro) {
            this.libro = libro;
        }

        public Libro getLibro() {
            return libro;
        }

        @Override
        public String toString() {
            String autori = String.join(", ", libro.autori());
            return libro.titolo() + " - " + autori;
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        client = new ClientOperazioni("localhost", 8080);


        HBox.setMargin(creaLibreriaBtn, new Insets(20,0,20,0));
        HBox.setMargin(rinominaLibreriaBtn, new Insets(20,0,20,0));
        HBox.setMargin(EliminaLibreriaBtn, new Insets(20,0,20,0));
        HBox.setMargin(aggiornaLibrerieBtn, new Insets(20,0,20,0));
        HBox.setMargin(valutaLibroBtn, new Insets(20,0,20,0));
        HBox.setMargin(mieValutazioniBtn, new Insets(20,0,20,0));
        HBox.setMargin(libroIDValutazioniField, new Insets(20,0,20,0));
        HBox.setMargin(cercaValutazioniBtn, new Insets(20,0,20,0));
        HBox.setMargin(generaConsigliBtn, new Insets(20,0,20,0));
        HBox.setMargin(mieiConsigliBtn, new Insets(20,0,20,0));
        HBox.setMargin(salvaConsiglioBtn, new Insets(20,0,20,0));


        output.getStyleClass().add("terminal-textarea");
        output.setEditable(false);


        // Inizializza lo stato dei pulsanti
        updateUIState();


        // L'output iniziale con animazione
        stampaConAnimazione("Benvenuto nel Book Recommender. Connettiti al server per iniziare.");

        // Aggiungi animazione all'hover dei pulsanti
        configuraAnimazioniPulsanti();        // Configura la ListView delle librerie
        setupLibrerieListView();

        // Configura il listener per il caricamento automatico delle librerie
        setupTabListener();
    }


    /**
     * Carica tutte le categorie nella ComboBox.
     */
    private ObservableList<String> tutteLeCategorie;

    private void caricaCategorieComboBox() {
        try {
            List<String> categorie = client.getCategorie();
            System.out.println(categorie.toString());

            tutteLeCategorie = FXCollections.observableArrayList(categorie);
            categoryComboBox.setItems(tutteLeCategorie);

            installAutoCompletion(categoryComboBox);

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Errore nel caricamento categorie: " + e.getMessage());
        }
    }

    private void installAutoCompletion(ComboBox<String> comboBox) {
        comboBox.setEditable(true);

        comboBox.getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
            // Evita di processare se la ComboBox non è in focus
            if (!comboBox.isFocused()) return;

            // Evita di processare se il nuovo valore è lo stesso del valore selezionato
            if (newValue != null && newValue.equals(comboBox.getValue())) return;

            Platform.runLater(() -> {
                if (newValue == null || newValue.isEmpty()) {
                    comboBox.setItems(tutteLeCategorie);
                } else {
                    List<String> filtered = tutteLeCategorie.stream()
                            .filter(item -> item.toLowerCase().contains(newValue.toLowerCase()))
                            .collect(Collectors.toList());

                    comboBox.setItems(FXCollections.observableArrayList(filtered));
                }

                if (!comboBox.getItems().isEmpty()) {
                    comboBox.show();
                }
            });
        });

        // Ripristina la lista completa quando si nasconde il dropdown
        comboBox.setOnHidden(e -> {
            if (comboBox.getValue() == null || comboBox.getValue().isEmpty()) {
                comboBox.setItems(tutteLeCategorie);
            }
        });
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

                // Aggiorna la cache locale dei libri
                for (Libro libro : libri) {
                    libriCache.put(libro.libroId(), libro);
                }

                if (libri.isEmpty()) {
                    Label emptyLabel = new Label("Questa libreria è vuota. Aggiungi dei libri!");
                    emptyLabel.getStyleClass().add("empty-label");
                    libreriaContentContainer.getChildren().add(emptyLabel);
                } else {
                    for (Libro libro : libri) {
                        libreriaContentContainer.getChildren().add(creaCardLibro(libro, libreriaID));
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
        return creaCardLibro(libro, -1);
    }

    /**
     * Crea una card per visualizzare un libro con possibilità di rimozione.
     *
     * @param libro      Il libro da visualizzare
     * @param libreriaID ID della libreria (se -1, non mostra il pulsante rimuovi)
     * @return Un nodo VBox che rappresenta la card del libro
     */
    private VBox creaCardLibro(Libro libro, int libreriaID) {
        VBox bookCard = new VBox(5);
        bookCard.getStyleClass().add("book-card");
        bookCard.setPadding(new Insets(10));

        // Titolo del libro
        Label titolo = new Label(libro.titolo());
        titolo.getStyleClass().add("book-title");
        titolo.setWrapText(true);

        // ID del libro
        Label IDLibro = new Label("ID del libro: " + libro.libroId());
        IDLibro.getStyleClass().add("book-category");
        IDLibro.setWrapText(true);

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
        HBox pulsantiContainer = new HBox(10);
        pulsantiContainer.setAlignment(Pos.CENTER);

        // Pulsante Valuta/Aggiorna valutazione
        Button valutaBtn = new Button("Valuta");
        valutaBtn.getStyleClass().addAll("primary-button", "small-button");

        // Logica per abilitazione/disabilitazione e testo pulsante
        boolean libroInLibreria = false;
        boolean valutazionePresente = false;
        Valutazione valutazioneUtente = null;
        try {
            if (client != null && client.isAutenticato()) {
                // Se siamo in una libreria, il libro è sicuramente presente
                if (libreriaID != -1) {
                    libroInLibreria = true;
                } else {
                    // Controlla se il libro è in almeno una libreria dell'utente
                    List<Libreria> mieLibrerie = client.elencaLibrerie();
                    for (Libreria lib : mieLibrerie) {
                        List<Libro> libri = client.visualizzaLibreria(lib.libreriaID());
                        for (Libro l : libri) {
                            if (l.libroId() == libro.libroId()) {
                                libroInLibreria = true;
                                break;
                            }
                        }
                        if (libroInLibreria) break;
                    }
                }
                // Controlla se esiste già una valutazione dell'utente per questo libro
                List<Valutazione> mieValutazioni = client.visualizzaMieValutazioni();
                for (Valutazione v : mieValutazioni) {
                    if (v.libroID() == libro.libroId()) {
                        valutazionePresente = true;
                        valutazioneUtente = v;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            // In caso di errore, lascia il pulsante disabilitato
            libroInLibreria = false;
        }

        valutaBtn.setDisable(!libroInLibreria);
        if (valutazionePresente) {
            valutaBtn.setText("Aggiorna valutazione");
        } else {
            valutaBtn.setText("Valuta");
        }

        // Azione pulsante: mostra il form di valutazione, precompilato se già presente
        Valutazione valutazioneDaPassare = valutazioneUtente;
        valutaBtn.setOnAction(_ -> mostraFormValutazioneLibro(libro, (Stage) bookCard.getScene().getWindow(), valutazioneDaPassare));

        // Pulsante rimuovi e sposta (solo se siamo in una libreria)
        if (libreriaID != -1) {
            Button rimuoviBtn = new Button("Rimuovi");
            rimuoviBtn.getStyleClass().addAll("danger-button", "small-button");
            rimuoviBtn.setOnAction(_ -> rimuoviLibroDaLibreriaConConferma(libro, libreriaID));

            Button spostaBtn = new Button("Sposta");
            spostaBtn.getStyleClass().addAll("primary-button", "small-button");
            spostaBtn.setOnAction(_ -> spostaLibroConDialogo(libro, libreriaID));

            pulsantiContainer.getChildren().addAll(spostaBtn, rimuoviBtn, valutaBtn);
        } else {
            pulsantiContainer.getChildren().add(valutaBtn);
        }

        // Aggiunge tutti gli elementi alla card
        bookCard.getChildren().addAll(titolo, IDLibro, autori, categoria, prezzo, pulsantiContainer);
        // Aggiungi effetto hover
        bookCard.setOnMouseEntered(_ -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(100), bookCard);
            st.setToX(1.03);
            st.setToY(1.03);
            st.play();
        });

        bookCard.setOnMouseExited(_ -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(100), bookCard);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });

        return bookCard;
    }

    /**
     * Configura il listener per il caricamento automatico delle librerie
     * quando viene selezionata la tab "Le Mie Librerie".
     */
    private void setupTabListener() {
        // Aggiungi un listener al SelectionModel del TabPane
        mainTabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldTab, newTab) -> {
            if (newTab != null && "Le Mie Librerie".equals(newTab.getText())) {
                // La tab "Le Mie Librerie" è stata selezionata
                // Carica automaticamente le librerie se l'utente è autenticato
                if (client.isAutenticato() && librerieListView.getItems().isEmpty()) {
                    aggiornaLibrerie();
                }
            }
        });
    }

    /**
     * Gestisce la creazione di una nuova libreria.
     */
    @FXML
    private void creaLibreria() {
        if (!client.isAutenticato()) {
            Alert alertLogin = new Alert(Alert.AlertType.WARNING);
            alertLogin.setTitle("Login Richiesto");
            alertLogin.setHeaderText("Accesso Negato");
            alertLogin.setContentText("Devi effettuare il login per creare una libreria.");
            alertLogin.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
            alertLogin.getDialogPane().getStyleClass().add("warning-dialog");
            alertLogin.showAndWait();
            stampaConAnimazione("Devi effettuare il login per creare una libreria.");
            return;
        }

        // Dialog per inserire il nome della libreria
        TextInputDialog dialog = new TextInputDialog();
        dialog.getDialogPane().getStyleClass().add("profile-dialog");
        dialog.setTitle("Crea Libreria");
        dialog.setHeaderText("Crea una nuova libreria personale");
        dialog.setContentText("Nome della libreria:");
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        // Personalizza i pulsanti
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setText("Crea");

        Optional<String> result = dialog.showAndWait();

        result.ifPresent(nomeLibreria -> {
            if (nomeLibreria.trim().isEmpty()) {
                Alert alertNomeVuoto = new Alert(Alert.AlertType.WARNING);
                alertNomeVuoto.setTitle("Nome non valido");
                alertNomeVuoto.setHeaderText("Nome libreria mancante");
                alertNomeVuoto.setContentText("Il nome della libreria non può essere vuoto.\nInserisci un nome valido.");
                alertNomeVuoto.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
                alertNomeVuoto.getDialogPane().getStyleClass().add("warning-dialog");
                alertNomeVuoto.showAndWait();
                stampaConAnimazione("Il nome della libreria non può essere vuoto.");
                return;
            }

            try {
                int libreriaID = client.creaLibreria(nomeLibreria);
                if (libreriaID > 0) {
                    // Alert di successo
                    Alert alertSuccess = new Alert(Alert.AlertType.INFORMATION);
                    alertSuccess.setTitle("Operazione Completata");
                    alertSuccess.setHeaderText("Libreria Creata!");
                    alertSuccess.setContentText("La libreria '" + nomeLibreria + "' è stata creata con successo.\n" +
                            "ID Libreria: " + libreriaID);
                    alertSuccess.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
                    alertSuccess.getDialogPane().getStyleClass().add("success-dialog");
                    alertSuccess.showAndWait();

                    stampaConAnimazione("Libreria '" + nomeLibreria + "' creata con successo (ID: " + libreriaID + ").");
                    aggiornaLibrerie();
                } else {
                    // Alert di errore
                    Alert alertError = new Alert(Alert.AlertType.ERROR);
                    alertError.setTitle("Errore");
                    alertError.setHeaderText("Creazione Fallita");
                    alertError.setContentText("Non è stato possibile creare la libreria '" + nomeLibreria + "'.\n" +
                            "Verifica che non esista già una libreria con lo stesso nome.");
                    alertError.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
                    alertError.getDialogPane().getStyleClass().add("error-dialog");
                    alertError.showAndWait();

                    stampaConAnimazione("Errore nella creazione della libreria.");
                }
            } catch (IOException e) {
                // Alert per errore di comunicazione
                Alert alertIOError = new Alert(Alert.AlertType.ERROR);
                alertIOError.setTitle("Errore di Comunicazione");
                alertIOError.setHeaderText("Errore di Connessione");
                alertIOError.setContentText("Si è verificato un errore durante la comunicazione con il server:\n" +
                        e.getMessage());
                alertIOError.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
                alertIOError.getDialogPane().getStyleClass().add("error-dialog");
                alertIOError.showAndWait();

                stampaConAnimazione("Errore: " + e.getMessage());
            }
        });
    }


    /**
     * Gestisce l'eliminazione della libreria.
     */
    @FXML
    private void EliminaLibreria() {
        if (!client.isAutenticato()) {
            Alert alertLogin = new Alert(Alert.AlertType.WARNING);
            alertLogin.setTitle("Login Richiesto");
            alertLogin.setHeaderText("Accesso Negato");
            alertLogin.setContentText("Devi effettuare il login per eliminare una libreria.");
            alertLogin.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
            alertLogin.getDialogPane().getStyleClass().add("warning-dialog");
            alertLogin.showAndWait();
            stampaConAnimazione("Devi effettuare il login per eliminare una libreria.");
            return;
        }

        Libreria libreriaSelezionata = librerieListView.getSelectionModel().getSelectedItem();
        if (libreriaSelezionata == null) {
            Alert alertSelezione = new Alert(Alert.AlertType.WARNING);
            alertSelezione.setTitle("Selezione Richiesta");
            alertSelezione.setHeaderText("Nessuna Libreria Selezionata");
            alertSelezione.setContentText("Per favore, seleziona prima una libreria da eliminare.");
            alertSelezione.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
            alertSelezione.getDialogPane().getStyleClass().add("warning-dialog");
            alertSelezione.showAndWait();
            stampaConAnimazione("Seleziona prima una libreria da eliminare.");
            return;
        }

        // Dialog di conferma eliminazione
        Alert alertConferma = new Alert(Alert.AlertType.CONFIRMATION);
        alertConferma.setTitle("Conferma Eliminazione");
        alertConferma.setHeaderText("Elimina Libreria");
        alertConferma.setContentText("Sei sicuro di voler eliminare la libreria '" +
                libreriaSelezionata.nomeLibreria() + "'?\n" +
                "Questa operazione non può essere annullata.");
        alertConferma.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        alertConferma.getDialogPane().getStyleClass().add("confirmation-dialog");

        // Personalizza i pulsanti
        ButtonType buttonTypeYes = new ButtonType("Sì, elimina");
        ButtonType buttonTypeNo = new ButtonType("Annulla", ButtonBar.ButtonData.CANCEL_CLOSE);
        alertConferma.getButtonTypes().setAll(buttonTypeYes, buttonTypeNo);

        Optional<ButtonType> result = alertConferma.showAndWait();
        if (result.isPresent() && result.get() == buttonTypeYes) {
            try {
                boolean success = client.eliminaLibreria(libreriaSelezionata.libreriaID());
                if (success) {
                    // Alert di successo
                    Alert alertSuccess = new Alert(Alert.AlertType.INFORMATION);
                    alertSuccess.setTitle("Operazione Completata");
                    alertSuccess.setHeaderText("Libreria Eliminata!");
                    alertSuccess.setContentText("La libreria '" + libreriaSelezionata.nomeLibreria() +
                            "' è stata eliminata con successo.");
                    alertSuccess.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
                    alertSuccess.getDialogPane().getStyleClass().add("success-dialog");
                    alertSuccess.showAndWait();

                    stampaConAnimazione("Libreria '" + libreriaSelezionata.nomeLibreria() + "' eliminata con successo.");
                    aggiornaLibrerie();
                } else {
                    // Alert di errore
                    Alert alertError = new Alert(Alert.AlertType.ERROR);
                    alertError.setTitle("Errore");
                    alertError.setHeaderText("Eliminazione Fallita");
                    alertError.setContentText("Non è stato possibile eliminare la libreria '" +
                            libreriaSelezionata.nomeLibreria() + "'. " +
                            "Verifica che la libreria esista ancora e riprova.");
                    alertError.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
                    alertError.getDialogPane().getStyleClass().add("error-dialog");
                    alertError.showAndWait();

                    stampaConAnimazione("Errore nell'eliminazione della libreria.");
                }
            } catch (IOException e) {
                // Alert per errore di comunicazione
                Alert alertIOError = new Alert(Alert.AlertType.ERROR);
                alertIOError.setTitle("Errore di Comunicazione");
                alertIOError.setHeaderText("Errore di Connessione");
                alertIOError.setContentText("Si è verificato un errore durante la comunicazione con il server: " +
                        e.getMessage());
                alertIOError.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
                alertIOError.getDialogPane().getStyleClass().add("error-dialog");
                alertIOError.showAndWait();

                stampaConAnimazione("Errore: " + e.getMessage());
            }
        }
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
            Alert alertLogin = new Alert(Alert.AlertType.WARNING);
            alertLogin.setTitle("Login Richiesto");
            alertLogin.setHeaderText("Accesso Negato");
            alertLogin.setContentText("Devi effettuare il login per aggiungere libri alle librerie.");
            alertLogin.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
            alertLogin.showAndWait();
            stampaConAnimazione("Devi effettuare il login per aggiungere libri alle librerie.");
            return;
        }

        Libreria libreriaSelezionata = librerieListView.getSelectionModel().getSelectedItem();
        if (libreriaSelezionata == null) {
            Alert alertSelezione = new Alert(Alert.AlertType.WARNING);
            alertSelezione.setTitle("Selezione Richiesta");
            alertSelezione.setHeaderText("Nessuna Libreria Selezionata");
            alertSelezione.setContentText("Per favore, seleziona prima una libreria.");
            alertSelezione.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
            alertSelezione.showAndWait();
            stampaConAnimazione("Seleziona prima una libreria.");
            return;
        }

        // Crea un dialogo per inserire l'ID del libro
        TextInputDialog dialog = new TextInputDialog();
        dialog.getDialogPane().getStyleClass().add("profile-dialog");
        dialog.setTitle("Aggiungi Libro");
        dialog.setHeaderText("Aggiungi un libro alla libreria '" + libreriaSelezionata.nomeLibreria() + "'");
        dialog.setContentText("ID del libro:");
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        Optional<String> result = dialog.showAndWait();

        result.ifPresent(libroIDStr -> {
            try {
                int libroID = Integer.parseInt(libroIDStr.trim());

                boolean success = client.aggiungiLibroALibreria(libreriaSelezionata.libreriaID(), libroID);
                if (success) {
                    // Alert di successo
                    Alert alertSuccess = new Alert(Alert.AlertType.INFORMATION);
                    alertSuccess.setTitle("Operazione Completata");
                    alertSuccess.setHeaderText("Libro Aggiunto!");
                    alertSuccess.setContentText("Il libro è stato aggiunto con successo alla libreria '" +
                            libreriaSelezionata.nomeLibreria() + "'.");
                    alertSuccess.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
                    alertSuccess.getDialogPane().getStyleClass().add("success-dialog");
                    alertSuccess.showAndWait();

                    stampaConAnimazione("Libro aggiunto alla libreria con successo.");
                    caricaLibriInLibreria(libreriaSelezionata.libreriaID());
                } else {
                    // Alert di errore generico
                    Alert alertError = new Alert(Alert.AlertType.ERROR);
                    alertError.setTitle("Errore");
                    alertError.setHeaderText("Operazione Fallita");
                    alertError.setContentText("Non è stato possibile aggiungere il libro alla libreria. " +
                            "Verifica che l'ID del libro sia corretto e che il libro non sia già presente.");
                    alertError.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
                    alertError.getDialogPane().getStyleClass().add("error-dialog");
                    alertError.showAndWait();

                    stampaConAnimazione("Errore nell'aggiunta del libro alla libreria.");
                }
            } catch (NumberFormatException e) {
                // Alert per errore di formato
                Alert alertFormatError = new Alert(Alert.AlertType.ERROR);
                alertFormatError.setTitle("Errore di Formato");
                alertFormatError.setHeaderText("ID Non Valido");
                alertFormatError.setContentText("L'ID inserito non è un numero valido. " +
                        "Per favore, inserisci un numero intero positivo.");
                alertFormatError.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
                alertFormatError.getDialogPane().getStyleClass().add("error-dialog");
                alertFormatError.showAndWait();

                stampaConAnimazione("ID libro non valido. Inserisci un numero intero.");
            } catch (IOException e) {
                // Alert per errore di comunicazione
                Alert alertIOError = new Alert(Alert.AlertType.ERROR);
                alertIOError.setTitle("Errore di Comunicazione");
                alertIOError.setHeaderText("Errore di Connessione");
                alertIOError.setContentText("Si è verificato un errore durante la comunicazione con il server: " +
                        e.getMessage());
                alertIOError.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
                alertIOError.getDialogPane().getStyleClass().add("error-dialog");
                alertIOError.showAndWait();

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
        dialog.getDialogPane().getStyleClass().add("profile-dialog");
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
                    // Alert di successo
                    Alert alertSuccess = new Alert(Alert.AlertType.INFORMATION);
                    alertSuccess.setTitle("Operazione Completata");
                    alertSuccess.setHeaderText("Libro Rimosso!");
                    alertSuccess.setContentText("Il libro con ID " + libroID +
                            " è stato rimosso con successo dalla libreria '" +
                            libreriaSelezionata.nomeLibreria() + "'.");
                    alertSuccess.getDialogPane().getStylesheets()
                            .add(getClass().getResource("/styles.css").toExternalForm());
                    alertSuccess.getDialogPane().getStyleClass().add("success-dialog");
                    alertSuccess.showAndWait();

                    stampaConAnimazione("Libro rimosso dalla libreria con successo.");
                    // Aggiorna la visualizzazione della libreria
                    caricaLibriInLibreria(libreriaSelezionata.libreriaID());
                } else {
                    // Alert di errore generico
                    Alert alertError = new Alert(Alert.AlertType.ERROR);
                    alertError.setTitle("Errore");
                    alertError.setHeaderText("Operazione Fallita");
                    alertError.setContentText("Non è stato possibile rimuovere il libro dalla libreria. " +
                            "Verifica che l'ID del libro sia corretto e che il libro sia presente.");
                    alertError.getDialogPane().getStylesheets()
                            .add(getClass().getResource("/styles.css").toExternalForm());
                    alertError.getDialogPane().getStyleClass().add("error-dialog");
                    alertError.showAndWait();

                    stampaConAnimazione("Errore nella rimozione del libro dalla libreria.");
                }
            } catch (NumberFormatException e) {
                // Alert per errore di formato
                Alert alertFormatError = new Alert(Alert.AlertType.ERROR);
                alertFormatError.setTitle("Errore di Formato");
                alertFormatError.setHeaderText("ID Non Valido");
                alertFormatError.setContentText("L'ID inserito non è un numero valido. " +
                        "Per favore, inserisci un numero intero positivo.");
                alertFormatError.getDialogPane().getStylesheets()
                        .add(getClass().getResource("/styles.css").toExternalForm());
                alertFormatError.getDialogPane().getStyleClass().add("error-dialog");
                alertFormatError.showAndWait();

                stampaConAnimazione("ID libro non valido. Inserisci un numero intero.");
            } catch (IOException e) {
                // Alert per errore di comunicazione
                Alert alertIOError = new Alert(Alert.AlertType.ERROR);
                alertIOError.setTitle("Errore di Comunicazione");
                alertIOError.setHeaderText("Errore di Connessione");
                alertIOError.setContentText("Si è verificato un errore durante la comunicazione con il server: " +
                        e.getMessage());
                alertIOError.getDialogPane().getStylesheets()
                        .add(getClass().getResource("/styles.css").toExternalForm());
                alertIOError.getDialogPane().getStyleClass().add("error-dialog");
                alertIOError.showAndWait();

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
                    // Alert di successo inline
                    Alert alertOk = new Alert(Alert.AlertType.INFORMATION);
                    alertOk.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
                    alertOk.setTitle("Valutazione salvata");
                    alertOk.setHeaderText(null);
                    alertOk.setContentText("Valutazione salvata con successo! (ID: " + valutazioneID + ")");
                    alertOk.getDialogPane().getStyleClass().add("success-dialog");
                    alertOk.showAndWait();

                    stampaConAnimazione("Valutazione salvata con successo (ID: " + valutazioneID + ").");
                } else {
                    // Alert per errore di formato
                    Alert alertFormatError = new Alert(Alert.AlertType.ERROR);
                    alertFormatError.setTitle("Errore");
                    alertFormatError.setHeaderText("La valutazione non è stata salvata");
                    alertFormatError.setContentText("La valutazione con ID: " + valutazioneID + " non è stata salvata");
                    alertFormatError.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
                    alertFormatError.getDialogPane().getStyleClass().add("error-dialog");
                    alertFormatError.showAndWait();

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
        VBox card = new VBox();
        card.setPrefWidth(390);    // Imposta la larghezza preferita
        card.setMinWidth(390);     // Larghezza minima
        card.setMaxWidth(390);     // Larghezza massima
        card.getStyleClass().addAll("valutazione-card");

        // ===== HEADER SECTION =====
        VBox headerSection = new VBox(8);
        headerSection.getStyleClass().add("header-section");

        // Se il libro non è in cache, caricalo
        if (!libriCache.containsKey(valutazione.libroID())) {
            try {
                Task<Libro> task = new Task<>() {
                    @Override
                    protected Libro call() throws Exception {
                        return client.ottieniDettagliLibro(valutazione.libroID());
                    }
                };

                task.setOnSucceeded(e -> {
                    Libro libro = task.getValue();
                    if (libro != null) {
                        libriCache.put(libro.libroId(), libro);
                        // Aggiorna l'UI con il titolo del libro
                        Platform.runLater(() -> {
                            Label titoloLabel = new Label(libro.titolo());
                            titoloLabel.getStyleClass().add("libro-titolo");

                            Label autoreLabel = new Label("di " + libro.autori());
                            autoreLabel.getStyleClass().add("libro-autore");

                            headerSection.getChildren().clear();
                            headerSection.getChildren().addAll(titoloLabel, autoreLabel);
                        });
                    }
                });

                // Mostra temporaneamente l'ID mentre carica
                Label loadingLabel = new Label("Caricamento dettagli libro...");
                loadingLabel.getStyleClass().add("libro-titolo-fallback");
                headerSection.getChildren().add(loadingLabel);

                Thread thread = new Thread(task);
                thread.setDaemon(true);
                thread.start();
            } catch (Exception e) {
                Label errorLabel = new Label("Libro ID: " + valutazione.libroID());
                errorLabel.getStyleClass().add("libro-titolo-fallback");
                headerSection.getChildren().add(errorLabel);
            }
        } else {
            // Usa il libro dalla cache
            Libro libro = libriCache.get(valutazione.libroID());
            Label titoloLabel = new Label(libro.titolo());
            titoloLabel.getStyleClass().add("libro-titolo");

            Label autoreLabel = new Label("di " + libro.autori());
            autoreLabel.getStyleClass().add("libro-autore");

            headerSection.getChildren().addAll(titoloLabel, autoreLabel);
        }


        // ===== RATING OVERVIEW =====
        VBox ratingOverview = new VBox(8);
        ratingOverview.getStyleClass().add("rating-overview");

        // Voto finale prominente
        double media = (valutazione.scoreStile() + valutazione.scoreContenuto() +
                valutazione.scoreGradevolezza() + valutazione.scoreOriginalita() +
                valutazione.scoreEdizione()) / 5.0;

        HBox mediaContainer = new HBox(10);
        mediaContainer.setAlignment(Pos.CENTER_LEFT);

        Label mediaValue = new Label(String.format("%.1f", media));
        mediaValue.getStyleClass().add("rating-value");

        // Crea stelle per il voto medio
        HBox starsBox = new HBox(2);
        starsBox.setAlignment(Pos.CENTER_LEFT);

        int stellePiene = (int) media;
        boolean mezzaStella = (media - stellePiene) >= 0.5;
        starsBox.getStyleClass().add("stars-container");
        for (int i = 0; i < 5; i++) {
            Label stella = new Label();
            if (i < stellePiene) {
                stella.setText("★");
                stella.getStyleClass().add("stella-piena");
            } else if (i == stellePiene && mezzaStella) {
                stella.setText("☆");
                stella.getStyleClass().add("stella-mezza");
            } else {
                stella.setText("☆");
                stella.getStyleClass().add("stella-vuota");
            }
            starsBox.getChildren().add(stella);
        }

        Label mediaText = new Label("/ 5.0");
        mediaText.getStyleClass().add("rating-max");

        mediaContainer.getChildren().addAll(mediaValue, starsBox, mediaText);
        ratingOverview.getChildren().add(mediaContainer);

        // ===== DETAILED SCORES =====
        VBox scoresSection = new VBox(10);
        scoresSection.getStyleClass().add("scores-section");


        // Usa FlowPane per layout più flessibile dei punteggi
        FlowPane punteggiContainer = new FlowPane();
        punteggiContainer.setHgap(15);
        punteggiContainer.setVgap(10);
        punteggiContainer.setAlignment(Pos.CENTER);

        // Utilizza il metodo esistente creaPunteggioBox
        punteggiContainer.getChildren().addAll(
                creaPunteggioBox("Stile", (short) valutazione.scoreStile()),
                creaPunteggioBox("Contenuto", (short) valutazione.scoreContenuto()),
                creaPunteggioBox("Gradevolezza", (short) valutazione.scoreGradevolezza()),
                creaPunteggioBox("Originalità", (short) valutazione.scoreOriginalita()),
                creaPunteggioBox("Edizione", (short) valutazione.scoreEdizione())
        );

        scoresSection.getChildren().add(punteggiContainer);

        // ===== NOTES SECTION =====
        VBox noteSection = new VBox(8);
        noteSection.getStyleClass().add("notes-section");

        Label noteTitle = new Label("Note");
        VBox noteContainer = new VBox(6);
        noteContainer.getStyleClass().add("note-box");
        noteTitle.getStyleClass().add("note-title");
        noteSection.getChildren().add(noteTitle);


            // Aggiungi note per ogni categoria se non nulle, non vuote e non composte solo da spazi
            if (valutazione.noteStile() != null && !valutazione.noteStile().trim().isEmpty()) {
                VBox notaBox = new VBox(4);
                notaBox.setStyle("-fx-padding: 0 0 8 0;");

                Label categoriaLabel = new Label("Stile:");
                categoriaLabel.getStyleClass().add("note-category");

                Label noteText = new Label(valutazione.noteStile());
                noteText.getStyleClass().add("note-text");
                noteText.setWrapText(true);

                notaBox.getChildren().addAll(categoriaLabel, noteText);
                noteContainer.getChildren().add(notaBox);
            }

            if (valutazione.noteContenuto() != null && !valutazione.noteContenuto().trim().isEmpty()) {
                VBox notaBox = new VBox(4);
                notaBox.setStyle("-fx-padding: 0 0 8 0;");

                Label categoriaLabel = new Label("Contenuto:");
                categoriaLabel.getStyleClass().add("note-category");

                Label noteText = new Label(valutazione.noteContenuto());
                noteText.getStyleClass().add("note-text");
                noteText.setWrapText(true);

                notaBox.getChildren().addAll(categoriaLabel, noteText);
                noteContainer.getChildren().add(notaBox);
            }

            if (valutazione.noteGradevolezza() != null && !valutazione.noteGradevolezza().trim().isEmpty()) {
                VBox notaBox = new VBox(4);
                notaBox.setStyle("-fx-padding: 0 0 8 0;");

                Label categoriaLabel = new Label("Gradevolezza:");
                categoriaLabel.getStyleClass().add("note-category");

                Label noteText = new Label(valutazione.noteGradevolezza());
                noteText.getStyleClass().add("note-text");
                noteText.setWrapText(true);

                notaBox.getChildren().addAll(categoriaLabel, noteText);
                noteContainer.getChildren().add(notaBox);
            }

            if (valutazione.noteOriginalita() != null && !valutazione.noteOriginalita().trim().isEmpty()) {
                VBox notaBox = new VBox(4);
                notaBox.setStyle("-fx-padding: 0 0 8 0;");

                Label categoriaLabel = new Label("Originalità:");
                categoriaLabel.getStyleClass().add("note-category");

                Label noteText = new Label(valutazione.noteOriginalita());
                noteText.getStyleClass().add("note-text");
                noteText.setWrapText(true);

                notaBox.getChildren().addAll(categoriaLabel, noteText);
                noteContainer.getChildren().add(notaBox);
            }

            if (valutazione.noteEdizione() != null && !valutazione.noteEdizione().trim().isEmpty()) {
                VBox notaBox = new VBox(4);
                notaBox.setStyle("-fx-padding: 0 0 8 0;");

                Label categoriaLabel = new Label("Edizione:");
                categoriaLabel.getStyleClass().add("note-category");

                Label noteText = new Label(valutazione.noteEdizione());
                noteText.getStyleClass().add("note-text");
                noteText.setWrapText(true);

                notaBox.getChildren().addAll(categoriaLabel, noteText);
                noteContainer.getChildren().add(notaBox);
            }
        noteSection.getChildren().add(noteContainer);

        // ===== FOOTER =====
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.getStyleClass().add("card-footer");

        // ===== ASSEMBLY =====
        card.getChildren().addAll(headerSection, ratingOverview, scoresSection, noteSection, footer);

        // ===== ANIMATIONS =====
        // Hover effect con scala e ombra
        card.setOnMouseEntered(e -> {
            ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(150), card);
            scaleTransition.setToX(1.03);
            scaleTransition.setToY(1.03);
            scaleTransition.setInterpolator(Interpolator.EASE_OUT);

            scaleTransition.play();
        });

        card.setOnMouseExited(e -> {
            ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(150), card);
            scaleTransition.setToX(1.0);
            scaleTransition.setToY(1.0);
            scaleTransition.setInterpolator(Interpolator.EASE_OUT);

            scaleTransition.play();
        });

        // Animazione di entrata
        card.setOpacity(0);
        card.setTranslateY(20);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), card);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        TranslateTransition slideIn = new TranslateTransition(Duration.millis(300), card);
        slideIn.setFromY(20);
        slideIn.setToY(0);

        ParallelTransition entrance = new ParallelTransition(fadeIn, slideIn);
        entrance.setDelay(Duration.millis(50)); // Leggero delay per effetto staggered
        entrance.play();

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

        // Stelle
        HBox stelleBox = new HBox(2);
        stelleBox.setAlignment(Pos.CENTER);
        stelleBox.getStyleClass().add("stars-container");
        // Calcola stelle piene e mezze stelle
        int stellePiene = (int) Math.floor(punteggio);
        boolean mezzaStella = (punteggio - stellePiene) >= 0.5;

        for (int i = 0; i < 5; i++) {
            Label stella = new Label();
            if (i < stellePiene) {
                // Stelle piene
                stella.setText("★");
                stella.getStyleClass().add("stella-piena");
            } else if (i == stellePiene && mezzaStella) {
                // Mezza stella
                stella.setText("☆");
                stella.getStyleClass().add("stella-mezza");
            } else {
                // Stelle vuote
                stella.setText("☆");
                stella.getStyleClass().add("stella-vuota");
            }

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
        if (libroIDStr.isEmpty() || !libroIDStr.matches("\\d+") || Integer.parseInt(libroIDStr) < 0) {

            // Alert per errore di formato
            Alert alertFormatError = new Alert(Alert.AlertType.ERROR);
            alertFormatError.setTitle("Errore");
            alertFormatError.setHeaderText("Inseriesci un ID del libro valido");
            alertFormatError.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
            alertFormatError.getDialogPane().getStyleClass().add("error-dialog");
            alertFormatError.showAndWait();

            stampaConAnimazione("Errore nel salvataggio del consiglio.");
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
            // Alert per errore di formato
            Alert alertFormatError = new Alert(Alert.AlertType.ERROR);
            alertFormatError.setTitle("Errore");
            alertFormatError.setHeaderText("Inseriesci un ID valido");
            alertFormatError.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
            alertFormatError.getDialogPane().getStyleClass().add("error-dialog");
            alertFormatError.showAndWait();

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
                    consigliContainer.getChildren().add(titleLabel);                    // Aggiungi le card dei consigli con la nuova implementazione migliorata
                    for (Consiglio consiglio : consigli) {
                        VBox consiglioCard = creaCardConsiglioMigliorata(consiglio);
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
                    // Alert di successo inline
                    Alert alertOk = new Alert(Alert.AlertType.INFORMATION);
                    alertOk.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
                    alertOk.setTitle("Consiglio salvato");
                    alertOk.setHeaderText(null);
                    alertOk.setContentText("Consiglio salvato con successo! (ID: " + consiglioID + ")");
                    alertOk.getDialogPane().getStyleClass().add("success-dialog");
                    alertOk.showAndWait();

                    stampaConAnimazione("Consiglio salvato con successo (ID: " + consiglioID + ").");
                } else {
                    // Alert per errore di formato
                    Alert alertFormatError = new Alert(Alert.AlertType.ERROR);
                    alertFormatError.setTitle("Errore");
                    alertFormatError.setHeaderText("Il consiglio non è stata salvata");
                    alertFormatError.setContentText("Il consiglio con ID: " + consiglioID + " non è stato salvato");
                    alertFormatError.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
                    alertFormatError.getDialogPane().getStyleClass().add("error-dialog");
                    alertFormatError.showAndWait();
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
     * @param libro              Il libro consigliato
     * @param libroRiferimentoID L'ID del libro di riferimento
     * @return Un nodo VBox che rappresenta la card del libro consigliato
     */
    private VBox creaCardLibroConsigliato(Libro libro, int libroRiferimentoID) {
        VBox bookCard = creaCardLibro(libro);

        // Aggiungi un pulsante per salvare il consiglio
        /* Button salvaConsiglioBtn = new Button("Salva Consiglio");
        salvaConsiglioBtn.getStyleClass().add("action-button");

        salvaConsiglioBtn.setOnAction(e -> {
            try {
                int consiglioID = client.salvaConsiglio(libroRiferimentoID, 0);

                if (consiglioID > 0) {
                    stampaConAnimazione("Consiglio salvato con successo (ID: " + consiglioID + ").");
                } else {
                    stampaConAnimazione("Errore nel salvataggio del consiglio.");
                }
            } catch (IOException ex) {
                stampaConAnimazione("Errore: " + ex.getMessage());
            }
        });

        bookCard.getChildren().add(salvaConsiglioBtn);*/

        return bookCard;
    }

    /**
     * Crea una card migliorata per visualizzare un consiglio con informazioni dettagliate sui libri.
     *
     * @param consiglio Il consiglio da visualizzare
     * @return Un nodo VBox che rappresenta la card del consiglio
     */
    private VBox creaCardConsiglioMigliorata(Consiglio consiglio) {
        VBox card = new VBox(10);
        card.getStyleClass().add("consiglio-card");
        card.setPadding(new Insets(15));

        // Intestazione della card
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label headerIcon = new Label("💡");
        headerIcon.getStyleClass().add("consiglio-icon");

        Label header = new Label("Consiglio #" + consiglio.consiglioID());
        header.getStyleClass().add("consiglio-header");

        headerBox.getChildren().addAll(headerIcon, header);

        // Sezione "Se ti è piaciuto"
        VBox libroRiferimentoSection = new VBox(5);
        Label labelRiferimento = new Label("Se ti è piaciuto:");
        labelRiferimento.getStyleClass().add("consiglio-section-label");

        HBox libroRiferimentoBox = new HBox(10);
        libroRiferimentoBox.setAlignment(Pos.CENTER_LEFT);

        Label iconRiferimento = new Label("📖");

        VBox infoRiferimento = new VBox(2);
        String titoloRiferimento = consiglio.titoloLibroRiferimento() != null ?
                consiglio.titoloLibroRiferimento() :
                "Libro ID: " + consiglio.libroRiferimentoID();
        Label titoloRifLabel = new Label(titoloRiferimento);
        titoloRifLabel.getStyleClass().add("libro-titolo");
        titoloRifLabel.setWrapText(true);

        Label idRifLabel = new Label("ID: " + consiglio.libroRiferimentoID());
        idRifLabel.getStyleClass().add("libro-id");

        infoRiferimento.getChildren().addAll(titoloRifLabel, idRifLabel);
        libroRiferimentoBox.getChildren().addAll(iconRiferimento, infoRiferimento);
        libroRiferimentoSection.getChildren().addAll(labelRiferimento, libroRiferimentoBox);

        // Freccia indicativa
        Label freccia = new Label("⬇");
        freccia.getStyleClass().add("consiglio-freccia");
        freccia.setAlignment(Pos.CENTER);

        // Sezione "Ti potrebbe piacere"
        VBox libroSuggeritoSection = new VBox(5);
        Label labelSuggerito = new Label("Ti potrebbe piacere:");
        labelSuggerito.getStyleClass().add("consiglio-section-label");

        HBox libroSuggeritoBox = new HBox(10);
        libroSuggeritoBox.setAlignment(Pos.CENTER_LEFT);

        Label iconSuggerito = new Label("⭐");

        VBox infoSuggerito = new VBox(2);
        String titoloSuggerito = consiglio.titoloLibroSuggerito() != null ?
                consiglio.titoloLibroSuggerito() :
                "Libro ID: " + consiglio.libroSuggeritoID();
        Label titoloSugLabel = new Label(titoloSuggerito);
        titoloSugLabel.getStyleClass().add("libro-titolo");
        titoloSugLabel.setWrapText(true);

        Label idSugLabel = new Label("ID: " + consiglio.libroSuggeritoID());
        idSugLabel.getStyleClass().add("libro-id");

        infoSuggerito.getChildren().addAll(titoloSugLabel, idSugLabel);
        libroSuggeritoBox.getChildren().addAll(iconSuggerito, infoSuggerito);
        libroSuggeritoSection.getChildren().addAll(labelSuggerito, libroSuggeritoBox);

        // Separatore
        Separator separator = new Separator();
        separator.getStyleClass().add("consiglio-separator");

        // Footer con data
        HBox footerBox = new HBox(10);
        footerBox.setAlignment(Pos.CENTER_LEFT);

        Label calendarIcon = new Label("📅");
        Label dataLabel = new Label("Salvato il: " + consiglio.dataSuggerimento().toLocalDate().toString());
        dataLabel.getStyleClass().add("consiglio-data");

        footerBox.getChildren().addAll(calendarIcon, dataLabel);        // Pulsanti di azione con miglior layout
        HBox pulsantiBox = new HBox(15);
        pulsantiBox.setAlignment(Pos.CENTER);
        pulsantiBox.setPadding(new Insets(10, 0, 0, 0));
        Button dettagliRifBtn = new Button("Dettagli libro di riferimento");
        dettagliRifBtn.getStyleClass().addAll("secondary-button", "small-button");
        dettagliRifBtn.setOnAction(e -> mostraDettagliLibro(consiglio.libroRiferimentoID()));

        // Tooltip informativo per il pulsante di riferimento
        Tooltip tooltipRif = new Tooltip("Visualizza i dettagli del libro che ti è piaciuto");
        tooltipRif.setShowDelay(Duration.millis(500));
        dettagliRifBtn.setTooltip(tooltipRif);

        // Animazioni per il pulsante secondario
        dettagliRifBtn.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), dettagliRifBtn);
            st.setToX(1.05);
            st.setToY(1.05);
            st.play();
        });

        dettagliRifBtn.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), dettagliRifBtn);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });

        Button dettagliSugBtn = new Button("Dettagli libro suggerito");
        dettagliSugBtn.getStyleClass().addAll("primary-button", "small-button");
        dettagliSugBtn.setOnAction(e -> mostraDettagliLibro(consiglio.libroSuggeritoID()));

        // Tooltip informativo per il pulsante suggerito
        Tooltip tooltipSug = new Tooltip("Visualizza i dettagli del libro che ti viene consigliato");
        tooltipSug.setShowDelay(Duration.millis(500));
        dettagliSugBtn.setTooltip(tooltipSug);

        // Animazioni per il pulsante primario
        dettagliSugBtn.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), dettagliSugBtn);
            st.setToX(1.05);
            st.setToY(1.05);
            st.play();

            // Effetto pulsazione per il pulsante principale
            FadeTransition ft = new FadeTransition(Duration.millis(300), dettagliSugBtn);
            ft.setFromValue(1.0);
            ft.setToValue(0.8);
            ft.setCycleCount(2);
            ft.setAutoReverse(true);
            ft.play();
        });

        dettagliSugBtn.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), dettagliSugBtn);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });

        pulsantiBox.getChildren().addAll(dettagliRifBtn, dettagliSugBtn);

        // Aggiungi tutti gli elementi alla card
        card.getChildren().addAll(
                headerBox,
                libroRiferimentoSection,
                freccia,
                libroSuggeritoSection,
                separator,
                footerBox,
                pulsantiBox
        );

        // Aggiungi effetti di animazione
        card.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), card);
            st.setToX(1.02);
            st.setToY(1.02);
            st.play();
        });

        card.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), card);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });

        return card;
    }


    /**
     * Configura le animazioni per i pulsanti dell'interfaccia.
     */
    private void configuraAnimazioniPulsanti() {
        // Aggiungi animazioni a tutti i pulsanti
        List<Button> buttons = List.of(
                connettiBtn, disconnettiBtn, loginBtn, registratiBtn,
                logoutBtn, cercaBtn,cercaCategorieBtn, profiloBtn,aggiungiLibroBtn,rimuoviLibroBtn
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
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                try {

                    return client.connetti();
                } catch (IOException e) {
                    Platform.runLater(() -> {


                        // Errore di connessione con Alert inline
                        stampa("Errore connessione: " + e.getMessage());
                        Alert alertErr = new Alert(Alert.AlertType.ERROR);
                        alertErr.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
                        alertErr.setTitle("Errore di connessione");
                        alertErr.setHeaderText(null);
                        alertErr.setContentText("Impossibile connettersi al server.\nDettagli: " + e.getMessage());
                        alertErr.getDialogPane().getStyleClass().add("error-dialog");
                        alertErr.showAndWait();
                    });
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


                    // Alert di successo inline
                    Alert alertOk = new Alert(Alert.AlertType.INFORMATION);
                    alertOk.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
                    alertOk.setTitle("Connessione riuscita");
                    alertOk.setHeaderText(null);
                    alertOk.setContentText("Sei connesso al server.");
                    alertOk.getDialogPane().getStyleClass().add("success-dialog");
                    alertOk.showAndWait();
                }

                updateUIState();
                caricaCategorieComboBox();
            }
        };

        // Animazione bottone durante la connessione
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
            connettiBtn.setDisable(false);
        });

        task.setOnFailed(event -> {
            rt.stop();
            connettiBtn.setText("Connetti");
            connettiBtn.setDisable(false);

            // Alert errore generico inline
            Platform.runLater(() -> {
                Alert alertErr = new Alert(Alert.AlertType.ERROR);
                alertErr.setTitle("Errore");
                alertErr.setHeaderText(null);
                alertErr.setContentText("Si è verificato un errore durante la connessione.");
                alertErr.getDialogPane().getStyleClass().add("error-dialog");
                alertErr.showAndWait();
            });
        });
    }

    @FXML
    private void disconnetti() {
        client.chiudi();
        isConnected = false;
        isLoggedIn = false;
        animaCambioStato(statusLabel, "Non connesso", "status-connected", "status-disconnected");
        stampaConAnimazione("🔌 Disconnesso dal server.");

        Alert alertOk = new Alert(Alert.AlertType.INFORMATION);
        alertOk.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        alertOk.setTitle("Disconnessione riuscita");
        alertOk.setHeaderText(null);
        alertOk.setContentText("Sei stato disconnesso dal server.");
        alertOk.getDialogPane().getStyleClass().add("success-dialog");
        alertOk.showAndWait();

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

                    // Mostra alert di successo
                    Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                    successAlert.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
                    successAlert.setTitle("Login Riuscito");
                    successAlert.setHeaderText("Accesso effettuato con successo");
                    successAlert.setContentText("Benvenuto, " + creds.getKey() + "!");
                    // Applica la classe CSS personalizzata
                    successAlert.getDialogPane().getStyleClass().add("success-dialog");
                    successAlert.showAndWait();

                    // Aggiorna librerie se necessario
                    Tab selectedTab = mainTabPane.getSelectionModel().getSelectedItem();
                    if (selectedTab != null && "Le Mie Librerie".equals(selectedTab.getText())) {
                        aggiornaLibrerie();
                    }
                } else {
                    stampaConAnimazione("❌ Login fallito. Controlla username e password.");
                    animaShake(loginBtn);

                    // Mostra alert di errore
                    Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                    errorAlert.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
                    errorAlert.setTitle("Login Fallito");
                    errorAlert.setHeaderText("Credenziali errate");
                    errorAlert.setContentText("Username o password non corretti. Riprova.");
                    errorAlert.getDialogPane().getStyleClass().add("error-dialog");
                    errorAlert.showAndWait();
                }
            } catch (IOException e) {
                stampa("Errore login: " + e.getMessage());

                // Mostra alert di errore per eccezione
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
                errorAlert.setTitle("Errore Login");
                errorAlert.setHeaderText("Errore durante il login");
                errorAlert.setContentText("Si è verificato un errore: " + e.getMessage());
                errorAlert.getDialogPane().getStyleClass().add("error-dialog");
                errorAlert.showAndWait();
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

                    Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                    successAlert.setTitle("Registrazione Completata");
                    successAlert.setHeaderText("Registrazione effettuata con successo");
                    successAlert.setContentText("UserID: " + userID + "\nPuoi ora accedere con le tue credenziali.");
                    successAlert.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
                    successAlert.getDialogPane().getStyleClass().add("success-dialog");
                    animaDialogo(successAlert);
                    successAlert.showAndWait();

                } else {
                    stampaConAnimazione("❌ Registrazione fallita.");
                    animaShake(registratiBtn);

                    Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                    errorAlert.setTitle("Registrazione Fallita");
                    errorAlert.setHeaderText("Errore durante la registrazione");
                    errorAlert.setContentText("Non è stato possibile completare la registrazione. Riprova.");
                    errorAlert.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
                    errorAlert.getDialogPane().getStyleClass().add("error-dialog");
                    animaDialogo(errorAlert);
                    errorAlert.showAndWait();
                }
            } catch (IOException e) {
                stampa("Errore registrazione: " + e.getMessage());

                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Errore Registrazione");
                errorAlert.setHeaderText("Errore imprevisto");
                errorAlert.setContentText("Si è verificato un errore: " + e.getMessage());
                errorAlert.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
                errorAlert.getDialogPane().getStyleClass().add("error-dialog");
                animaDialogo(errorAlert);
                errorAlert.showAndWait();
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
                clearUI();

                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
                successAlert.setTitle("Logout Riuscito");
                successAlert.setHeaderText("Logout completato");
                successAlert.setContentText("Sei stato disconnesso con successo.");
                successAlert.getDialogPane().getStyleClass().add("success-dialog");
                successAlert.showAndWait();

            } else {
                stampaConAnimazione("❌ Logout fallito.");
                animaShake(logoutBtn);

                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
                errorAlert.setTitle("Logout Fallito");
                errorAlert.setHeaderText("Impossibile effettuare il logout");
                errorAlert.setContentText("Si è verificato un problema durante il logout. Riprova.");
                errorAlert.getDialogPane().getStyleClass().add("error-dialog");
                errorAlert.showAndWait();
            }
        } catch (IOException e) {
            stampa("Errore logout: " + e.getMessage());

            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
            errorAlert.setTitle("Errore Logout");
            errorAlert.setHeaderText("Errore durante il logout");
            errorAlert.setContentText("Si è verificato un errore: " + e.getMessage());
            errorAlert.getDialogPane().getStyleClass().add("error-dialog");
            errorAlert.showAndWait();
        }
    }

    /**
     * Ripulisce la GUI allo stato iniziale (prima che un qualsiasi utente si loggasse).
     */
    private void clearUI() {
        resultContainer.getChildren().clear();
        consigliContainer.getChildren().clear();
        searchField.clear();
        resultLabel.setText("Inserisci un termine di ricerca");
        libreriaContentContainer.getChildren().clear();
        librerieListView.getItems().clear();
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
     * Cerca libri in base alla categoria selezionata.
     */
    @FXML
    private void cercaLibriPerCategoria() {
        String categoriaSelezionata = categoryComboBox.getValue();
        String termine = searchField.getText().trim();
        if (categoriaSelezionata == null || categoriaSelezionata.trim().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Categoria non selezionata");
            alert.setHeaderText("Nessuna categoria selezionata");
            alert.setContentText("Seleziona una categoria dalla lista.");
            // Aggiungi la classe CSS all'Alert
            alert.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
            alert.getDialogPane().getStyleClass().add("registration-alert");
            animaDialogo(alert);
            alert.showAndWait();
            return;
        }
        categoriaSelezionata = categoriaSelezionata + ">" + termine;

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
        String finalCategoriaSelezionata = categoriaSelezionata;
        Task<List<Libro>> task = new Task<>() {
            @Override
            protected List<Libro> call() throws Exception {
                return client.cercaLibriPerCategoria(finalCategoriaSelezionata);
            }

            @Override
            protected void succeeded() {
                fade.stop();
                resultLabel.setOpacity(1.0);

                List<Libro> libri = getValue();
                if (libri.isEmpty()) {
                    stampaConAnimazione("Nessun libro trovato per la categoria: \"" + finalCategoriaSelezionata + "\"");
                    resultLabel.setText("Nessun risultato trovato");
                } else {
                    stampaConAnimazione("📚 Trovati " + libri.size() + " libri nella categoria \"" + finalCategoriaSelezionata + "\"");
                    resultLabel.setText("Libri trovati (" + libri.size() + ")");
                    mostraRisultati(libri);
                }
            }

            @Override
            protected void failed() {
                fade.stop();
                resultLabel.setOpacity(1.0);
                stampaConAnimazione("Errore durante la ricerca per categoria: " + getException().getMessage());
                resultLabel.setText("Errore nella ricerca");

                // Mostra alert di errore
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Errore nella ricerca");
                alert.setHeaderText("Impossibile cercare libri per categoria");
                alert.setContentText("Si è verificato un errore: " + getException().getMessage());
                alert.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
                alert.getDialogPane().getStyleClass().add("registration-alert");
                animaDialogo(alert);
                alert.showAndWait();
            }
        };

        new Thread(task).start();
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

            // ID del libro
            Label IDLibro = new Label("ID del libro: " + libro.libroId());
            IDLibro.getStyleClass().add("book-category");
            IDLibro.setWrapText(true);

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
            bookCard.getChildren().addAll(titolo, IDLibro, autori, categoria, prezzo);
            resultContainer.getChildren().add(bookCard);            // Aggiungi evento click sulla card
            bookCard.setOnMouseClicked(event -> {
                // Crea effetto pulsazione quando cliccato
                ScaleTransition st = new ScaleTransition(Duration.millis(100), bookCard);
                st.setToX(1.03);
                st.setToY(1.03);
                st.setCycleCount(2);
                st.setAutoReverse(true);
                st.play();

                // Apri il dialogo di dettagli del libro
                st.setOnFinished(e -> mostraDettagliLibro(libro));
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
     * Mostra il profilo dell'utente in un dialogo elegante con animazioni e layout ottimizzato.
     *
     * @param utente L'utente di cui mostrare il profilo
     */
    private void mostraprofilo(Utente utente) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Profilo Utente");

        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        dialog.getDialogPane().getStyleClass().add("profile-dialog");

        ButtonType closeButton = new ButtonType("Chiudi", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().add(closeButton);

        BorderPane rootPane = new BorderPane();
        rootPane.setPadding(new Insets(30));
        rootPane.setPrefWidth(500);

        VBox contentBox = new VBox(25);
        contentBox.setAlignment(Pos.CENTER);
        contentBox.setStyle("-fx-background-color: transparent;");

        // Titolo centrato
        Label titolo = new Label("👤 Profilo Utente");
        titolo.getStyleClass().add("section-title");

        // Griglia centrata
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(18);
        grid.setAlignment(Pos.CENTER);
        grid.setStyle("-fx-background-color: transparent;");
        grid.getStyleClass().add("profile-grid");

        // Colonne centrali (nessun bordo smussato)
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setHalignment(HPos.RIGHT);
        col1.setPercentWidth(40);

        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHalignment(HPos.LEFT);
        col2.setPercentWidth(60);

        grid.getColumnConstraints().addAll(col1, col2);

        // Campi profilo
        int row = 0;
        row = addAnimatedProfileField(grid, "User ID:", String.valueOf(utente.userID()), row);
        row = addAnimatedProfileField(grid, "Nome Completo:", utente.nomeCompleto(), row);
        row = addAnimatedProfileField(grid, "Email:", utente.email(), row);
        row = addAnimatedProfileField(grid, "Username:", utente.username(), row);
        row = addAnimatedProfileField(grid, "Codice Fiscale:", utente.codiceFiscale() != null ? utente.codiceFiscale() : "Non specificato", row);
        row = addAnimatedProfileField(grid, "Iscrizione:", utente.dataRegistrazione().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), row);

        contentBox.getChildren().addAll(titolo, grid);
        rootPane.setCenter(contentBox);

        dialog.getDialogPane().setContent(rootPane);
        animaDialogo(dialog);
        dialog.showAndWait();
    }

    /**
     * Aggiunge un campo al profilo con animazione di apparizione e ritorna la riga successiva.
     *
     * @param grid      Il GridPane in cui aggiungere il campo
     * @param labelText Il testo dell'etichetta del campo
     * @param value     Il valore del campo da visualizzare
     * @param row       La riga corrente nel GridPane
     * @return La riga successiva disponibile
     */
    private int addAnimatedProfileField(GridPane grid, String labelText, String value, int row) {
        Label label = new Label(labelText);
        label.getStyleClass().add("profile-label");
        label.setAlignment(Pos.CENTER);
        label.setMaxWidth(Double.MAX_VALUE);

        Label value1 = new Label(value);
        value1.getStyleClass().add("profile-value");
        value1.setAlignment(Pos.CENTER);
        value1.setMaxWidth(Double.MAX_VALUE);

        GridPane.setHalignment(label, HPos.CENTER);
        GridPane.setHalignment(value1, HPos.CENTER);

        grid.add(label, 0, row);
        grid.add(value1, 1, row);

        // Delay e fade
        PauseTransition delay = new PauseTransition(Duration.millis(80 * row));
        delay.setOnFinished(e -> {
            FadeTransition ft = new FadeTransition(Duration.millis(400), value1);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.setInterpolator(Interpolator.EASE_OUT);
            ft.play();
        });
        delay.play();
        return row + 1;
    }


    /**
     * Stampa un messaggio con un'animazione di evidenziazione.
     *
     * @param msg Il messaggio da stampare
     */
    private void stampaConAnimazione(String msg) {
        output.appendText(msg + "\n");
        output.positionCaret(output.getText().length());

        int lastLineIndex = output.getText().lastIndexOf("\n", output.getText().length() - 2);
        if (lastLineIndex == -1) lastLineIndex = 0;

        int startIndex = lastLineIndex + 1;
        int endIndex = output.getText().length();

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, e -> {
                    output.setStyle("-fx-highlight-fill: #214283; -fx-highlight-text-fill: white;");
                    output.selectRange(startIndex, endIndex);
                }),
                new KeyFrame(Duration.millis(1000), e -> {
                    output.deselect();
                    output.setStyle(""); // resetta lo stile (ma conserva la classe CSS)
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
        dialog.getDialogPane().getStyleClass().add("profile-dialog");

        ButtonType loginButtonType = new ButtonType("Login", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(20, 25, 10, 25));
        grid.setAlignment(Pos.CENTER_LEFT);

        Label labelUsername = new Label("Username:");
        labelUsername.getStyleClass().add("custom-label-box");

        Label labelPassword = new Label("Password:");
        labelPassword.getStyleClass().add("custom-label-box");

        TextField username = new TextField();
        username.setPromptText("username");
        username.getStyleClass().add("custom-field");

        PasswordField password = new PasswordField();
        password.setPromptText("password");
        password.getStyleClass().add("custom-field");

        grid.add(labelUsername, 0, 0);
        grid.add(username, 1, 0);
        grid.add(labelPassword, 0, 1);
        grid.add(password, 1, 1);

        VBox container = new VBox();
        container.setPadding(new Insets(30, 0, 0, 0));  // margine alto 30 px
        container.getChildren().add(grid);

        dialog.getDialogPane().setContent(container);

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
        dialog.getDialogPane().getStyleClass().add("profile-dialog");  // usa lo stile del profilo

        ButtonType registerButton = new ButtonType("Registrati", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(registerButton, ButtonType.CANCEL);

        BorderPane rootPane = new BorderPane();
        rootPane.setPadding(new Insets(30));
        rootPane.setPrefWidth(500);

        VBox contentBox = new VBox(25);
        contentBox.setAlignment(Pos.CENTER);
        contentBox.setStyle("-fx-background-color: transparent;");

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(18);
        grid.setAlignment(Pos.CENTER);
        grid.setStyle("-fx-background-color: transparent;");
        grid.getStyleClass().add("profile-grid");

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setHalignment(HPos.CENTER);   // centra la label orizzontalmente
        col1.setPercentWidth(40);

        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHalignment(HPos.LEFT);
        col2.setPercentWidth(60);

        grid.getColumnConstraints().addAll(col1, col2);

        // Crea label con classe .custom-label-box (centrata in box)
        Label nomeLabel = new Label("Nome Completo:");
        nomeLabel.getStyleClass().add("custom-label-box");
        TextField nome = new TextField();
        nome.setPromptText("Nome completo");
        nome.getStyleClass().add("custom-field");

        Label emailLabel = new Label("Email:");
        emailLabel.getStyleClass().add("custom-label-box");
        TextField email = new TextField();
        email.setPromptText("Email");
        email.getStyleClass().add("custom-field");

        Label usernameLabel = new Label("Username:");
        usernameLabel.getStyleClass().add("custom-label-box");
        TextField username = new TextField();
        username.setPromptText("Username");
        username.getStyleClass().add("custom-field");

        Label passwordLabel = new Label("Password:");
        passwordLabel.getStyleClass().add("custom-label-box");
        PasswordField password = new PasswordField();
        password.setPromptText("Password sicura");
        password.getStyleClass().add("custom-field");

        Label cfLabel = new Label("Codice Fiscale:");
        cfLabel.getStyleClass().add("custom-label-box");
        TextField cf = new TextField();
        cf.setPromptText("Codice fiscale (opzionale)");
        cf.getStyleClass().add("custom-field");

        grid.add(nomeLabel, 0, 0);
        grid.add(nome, 1, 0);
        grid.add(emailLabel, 0, 1);
        grid.add(email, 1, 1);
        grid.add(usernameLabel, 0, 2);
        grid.add(username, 1, 2);
        grid.add(passwordLabel, 0, 3);
        grid.add(password, 1, 3);
        grid.add(cfLabel, 0, 4);
        grid.add(cf, 1, 4);

        contentBox.getChildren().addAll(grid);
        rootPane.setCenter(contentBox);

        dialog.getDialogPane().setContent(rootPane);

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
     * @param label            L'etichetta su cui eseguire l'animazione
     * @param nuovoTesto       Il nuovo testo da impostare sull'etichetta
     * @param classeRimuovere  La classe da rimuovere dallo stile dell'etichetta
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
                logoutBtn, cercaBtn, cercaCategorieBtn,categoryComboBox,
                searchField,profiloBtn,
                creaLibreriaBtn, rinominaLibreriaBtn, aggiornaLibrerieBtn, aggiungiLibroBtn, rimuoviLibroBtn, EliminaLibreriaBtn,
                valutaLibroBtn, mieValutazioniBtn, cercaValutazioniBtn, libroIDValutazioniField,
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
            } else if (control == logoutBtn || control == profiloBtn ) {
                newDisabled = !isConnected || !isLoggedIn;
            } else if (control == cercaBtn || control == searchField || control == cercaCategorieBtn || control== categoryComboBox) {
                newDisabled = !isConnected;
            } else if (control == creaLibreriaBtn || control == rinominaLibreriaBtn || control == aggiornaLibrerieBtn ||
                    control == aggiungiLibroBtn || control == rimuoviLibroBtn || control == EliminaLibreriaBtn) {
                newDisabled = !isConnected || !isLoggedIn;
            } else if (control == valutaLibroBtn || control == mieValutazioniBtn ||
                    control == cercaValutazioniBtn || control == libroIDValutazioniField) {
                newDisabled = !isConnected || !isLoggedIn;
            } else if (control == generaConsigliBtn || control == mieiConsigliBtn ||
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

    /**
     * Rimuove un libro da una libreria con richiesta di conferma e messaggio di successo.
     *
     * @param libro      Il libro da rimuovere
     * @param libreriaID ID della libreria da cui rimuovere il libro
     */
    private void rimuoviLibroDaLibreriaConConferma(Libro libro, int libreriaID) {
        // Chiedi conferma prima di rimuovere
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Conferma rimozione");
        alert.setHeaderText("Rimuovi libro dalla libreria");
        alert.setContentText("Sei sicuro di voler rimuovere il libro '" + libro.titolo() + "' dalla libreria?");

        // Aggiungi stile all'alert
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                boolean success = client.rimuoviLibroDaLibreria(libreriaID, libro.libroId());
                if (success) {
                    // Messaggio di successo
                    mostraMessaggioSuccesso("Libro '" + libro.titolo() + "' rimosso con successo dalla libreria!");

                    // Aggiorna la visualizzazione della libreria
                    caricaLibriInLibreria(libreriaID);
                } else {
                    stampaConAnimazione("Errore nella rimozione del libro dalla libreria.");
                }
            } catch (IOException e) {
                stampaConAnimazione("Errore di comunicazione: " + e.getMessage());
            }
        }
    }

    /**
     * Sposta un libro da una libreria all'altra con dialogo di selezione.
     */
    private void spostaLibroConDialogo(Libro libro, int libreriaCorrenteID) {
        // Carica le librerie dell'utente
        Task<List<Libreria>> loadLibrariesTask = new Task<>() {
            @Override
            protected List<Libreria> call() throws Exception {
                return client.elencaLibrerie();
            }

            @Override
            protected void succeeded() {
                List<Libreria> librerie = getValue();

                // Filtra escludendo la libreria corrente
                List<Libreria> librerieDestinazione = librerie.stream()
                        .filter(lib -> lib.libreriaID() != libreriaCorrenteID)
                        .collect(java.util.stream.Collectors.toList());

                if (librerieDestinazione.isEmpty()) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Nessuna Libreria");
                    alert.setHeaderText("Non hai altre librerie");
                    alert.setContentText("Crea un'altra libreria per spostare i libri.");
                    alert.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
                    alert.showAndWait();
                    return;
                }

                // Dialogo selezione libreria destinazione
                List<LibreriaDisplay> librerieDisplay = librerieDestinazione.stream()
                        .map(LibreriaDisplay::new)
                        .collect(java.util.stream.Collectors.toList());

                ChoiceDialog<LibreriaDisplay> dialog = new ChoiceDialog<>(librerieDisplay.get(0), librerieDisplay);
                dialog.setTitle("Sposta Libro");
                dialog.setHeaderText("Sposta \"" + libro.titolo() + "\" in un'altra libreria");
                dialog.setContentText("Scegli la libreria di destinazione:");
                dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

                Optional<LibreriaDisplay> result = dialog.showAndWait();
                result.ifPresent(libreriaDisplay -> {
                    Libreria libreriaDestinazione = libreriaDisplay.getLibreria();

                    // Task per spostare il libro
                    Task<Boolean> moveBookTask = new Task<>() {
                        @Override
                        protected Boolean call() throws Exception {
                            return client.spostaLibro(libreriaCorrenteID, libreriaDestinazione.libreriaID(), libro.libroId());
                        }

                        @Override
                        protected void succeeded() {
                            boolean success = getValue();
                            if (success) {
                                mostraMessaggioSuccesso("Libro \"" + libro.titolo() + "\" spostato con successo!");
                                caricaLibriInLibreria(libreriaCorrenteID);
                            } else {
                                mostraMessaggioErrore("Impossibile spostare il libro.");
                            }
                        }

                        @Override
                        protected void failed() {
                            mostraMessaggioErrore("Errore: " + getException().getMessage());
                        }
                    };

                    new Thread(moveBookTask).start();
                });
            }

            @Override
            protected void failed() {
                mostraMessaggioErrore("Errore nel caricamento delle librerie: " + getException().getMessage());
            }
        };

        new Thread(loadLibrariesTask).start();
    }

    /**
     * Mostra un messaggio di successo con animazione.
     *
     * @param messaggio Il messaggio da mostrare
     */
    private void mostraMessaggioSuccesso(String messaggio) {
        // Crea un Alert di tipo INFORMATION per il messaggio di successo
        Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
        successAlert.setTitle("Operazione completata");
        successAlert.setHeaderText("Successo! ✅");
        successAlert.setContentText(messaggio);

        // Aggiungi stile all'alert
        successAlert.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        // Mostra il messaggio senza bloccare e con auto-close dopo 3 secondi
        successAlert.show();
        // Auto-close dopo 3 secondi
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(3), _ -> successAlert.close()));
        timeline.play();

        // Stampa anche nella console per coerenza
        stampaConAnimazione(messaggio);
    }

    /**
     * Mostra un messaggio di errore con un popup Alert che si chiude automaticamente dopo 3 secondi
     *
     * @param messaggio Il messaggio di errore da mostrare
     */
    private void mostraMessaggioErrore(String messaggio) {
        // Crea un Alert di tipo ERROR per il messaggio di errore
        Alert errorAlert = new Alert(Alert.AlertType.ERROR);
        errorAlert.setTitle("Operazione fallita");
        errorAlert.setHeaderText("Errore! ❌");
        errorAlert.setContentText(messaggio);

        // Aggiungi stile all'alert
        errorAlert.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        // Mostra il messaggio senza bloccare e con auto-close dopo 3 secondi
        errorAlert.show();
        // Auto-close dopo 3 secondi
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(3), _ -> errorAlert.close()));
        timeline.play();

        // Stampa anche nella console per coerenza
        stampaConAnimazione("ERRORE: " + messaggio);
    }

    /**
     * Gestisce la rinomina di una libreria.
     */
    @FXML
    private void rinominaLibreria() {
        if (!client.isAutenticato()) {
            stampaConAnimazione("Devi effettuare il login per rinominare una libreria.");
            return;
        }

        Libreria libreriaSelezionata = librerieListView.getSelectionModel().getSelectedItem();
        if (libreriaSelezionata == null) {
            stampaConAnimazione("Seleziona prima una libreria da rinominare.");
            return;
        }

        // Crea un dialogo per inserire il nuovo nome della libreria
        TextInputDialog dialog = new TextInputDialog(libreriaSelezionata.nomeLibreria());
        dialog.getDialogPane().getStyleClass().add("profile-dialog");
        dialog.setTitle("Rinomina Libreria");
        dialog.setHeaderText("Rinomina la libreria '" + libreriaSelezionata.nomeLibreria() + "'");
        dialog.setContentText("Nuovo nome:");

        // Aggiungi stile al dialogo
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        // Mostra il dialogo e attendi il risultato
        Optional<String> result = dialog.showAndWait();

        result.ifPresent(nuovoNome -> {
            if (nuovoNome.trim().isEmpty()) {
                stampaConAnimazione("Il nome della libreria non può essere vuoto.");
                return;
            }

            if (nuovoNome.trim().equals(libreriaSelezionata.nomeLibreria())) {
                stampaConAnimazione("Il nuovo nome è uguale al precedente.");
                return;
            }

            try {
                boolean success = client.rinominaLibreria(libreriaSelezionata.libreriaID(), nuovoNome.trim());
                if (success) {
                    stampaConAnimazione("Libreria rinominata da '" + libreriaSelezionata.nomeLibreria() + "' a '" + nuovoNome.trim() + "' con successo.");

                    // Aggiorna la lista delle librerie per riflettere le modifiche
                    aggiornaLibrerie();

                    // Aggiorna anche l'etichetta della libreria selezionata se necessario
                    if (libreriaSelezionataLabel.getText().contains(libreriaSelezionata.nomeLibreria())) {
                        libreriaSelezionataLabel.setText("Libreria: " + nuovoNome.trim());
                    }
                } else {
                    stampaConAnimazione("Errore durante la rinomina della libreria.");
                }
            } catch (IOException e) {
                stampaConAnimazione("Errore: " + e.getMessage());
            }
        });
    }

    /**
     * Mostra un dialogo dettagliato con tutte le informazioni del libro.
     *
     * @param libro Il libro di cui mostrare i dettagli
     */
    private void mostraDettagliLibro(Libro libro) {
        // Crea il dialogo principale
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("Dettagli Libro - " + libro.titolo());
        dialogStage.setResizable(false);

        // Container principale con scroll
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("scroll-pane");

        VBox mainContainer = new VBox(15);
        mainContainer.setPadding(new Insets(20));
        mainContainer.getStyleClass().add("dialog-container");

        // Applica il tema scuro al dialogo
        scrollPane.getStyleClass().add("root");
        mainContainer.getStyleClass().add("root");

        // === SEZIONE INFORMAZIONI LIBRO ===
        VBox bookInfoSection = creaSezioneInformazioniLibro(libro);
        mainContainer.getChildren().add(bookInfoSection);

        // === SEZIONE VALUTAZIONI AGGREGATE ===
        VBox ratingsSection = creaSezioneValutazioniAggregate(libro);
        mainContainer.getChildren().add(ratingsSection);

        // === SEZIONE RECENSIONI UTENTI ===
        VBox reviewsSection = creaSezioneRecensioni(libro);
        mainContainer.getChildren().add(reviewsSection);

        // === SEZIONE LIBRI CONSIGLIATI (solo per utenti autenticati) ===
        if (client.isAutenticato()) {
            VBox recommendationsSection = creaSezioneLibriConsigliati(libro);
            mainContainer.getChildren().add(recommendationsSection);
        }

        // === SEZIONE AZIONI UTENTE ===
        if (client.isAutenticato()) {
            VBox actionsSection = creaSezioneAzioniUtente(libro, dialogStage);
            mainContainer.getChildren().add(actionsSection);
        }

        scrollPane.setContent(mainContainer);

        // Configura la scena
        Scene scene = new Scene(scrollPane, 600, 700);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        scene.setFill(javafx.scene.paint.Color.web("#0d1b2a")); // Forza il colore di sfondo scuro
        dialogStage.setScene(scene);

        // Animazione di apertura
        dialogStage.setOpacity(0);
        dialogStage.show();

        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), dialogStage.getScene().getRoot());
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.setOnFinished(e -> dialogStage.setOpacity(1));
        fadeIn.play();
    }

    /**
     * Crea la sezione con le informazioni base del libro.
     */
    private VBox creaSezioneInformazioniLibro(Libro libro) {
        VBox section = new VBox(10);
        section.getStyleClass().add("book-card");

        // Titolo della sezione
        Label sectionTitle = new Label("📚 INFORMAZIONI LIBRO");
        sectionTitle.getStyleClass().addAll("book-title", "section-header");

        // Titolo del libro
        Label titleLabel = new Label(libro.titolo());
        titleLabel.getStyleClass().add("book-title");
        titleLabel.setWrapText(true);

        // Autori
        Label authorsLabel = new Label("👤 Autori: " + libro.autori());
        authorsLabel.getStyleClass().add("book-author");
        authorsLabel.setWrapText(true);

        // Categoria
        Label categoryLabel = new Label("📖 Categoria: " + libro.categoria());
        categoryLabel.getStyleClass().add("book-category");
        categoryLabel.setWrapText(true);

        // Editore (solo se non è vuoto)
        if (!libro.editore().isEmpty()) {
            Label publisherLabel = new Label("🏢 Editore: " + libro.editore());
            publisherLabel.getStyleClass().add("book-info");
            publisherLabel.setWrapText(true);
            section.getChildren().add(publisherLabel);
        }

        // Prezzo
        Label priceLabel = new Label("💰 Prezzo: €" + String.format("%.2f", libro.prezzo()));
        priceLabel.getStyleClass().add("book-price");

        // Data di pubblicazione (solo se l'anno è valido)
        if (libro.annoPubblicazione() > 0) {
            String dataString = (libro.mesePubblicazione() == null || libro.mesePubblicazione().isEmpty()) ?
                    String.valueOf(libro.annoPubblicazione()) :
                    libro.mesePubblicazione() + " " + libro.annoPubblicazione();
            Label dateLabel = new Label("📅 Pubblicazione: " + dataString);
            dateLabel.getStyleClass().add("book-info");
            section.getChildren().add(dateLabel);
        }

        // Descrizione (se disponibile)
        if (!libro.descrizione().isEmpty()) {
            Label descLabel = new Label("📝 Descrizione:");
            descLabel.getStyleClass().add("book-info");

            Label descText = new Label(libro.descrizione());
            descText.getStyleClass().add("book-author");
            descText.setWrapText(true);

            section.getChildren().addAll(sectionTitle, titleLabel, authorsLabel, categoryLabel,
                    priceLabel, descLabel, descText);
        } else {
            section.getChildren().addAll(sectionTitle, titleLabel, authorsLabel, categoryLabel, priceLabel);
        }

        return section;
    }

    /**
     * Crea la sezione con le valutazioni aggregate del libro.
     */
    private VBox creaSezioneValutazioniAggregate(Libro libro) {
        VBox section = new VBox(10);
        section.getStyleClass().add("book-card");

        Label sectionTitle = new Label("⭐ VALUTAZIONI AGGREGATE");
        sectionTitle.getStyleClass().addAll("book-title", "section-header");
        section.getChildren().add(sectionTitle);

        // Carica le valutazioni in background
        Task<List<Valutazione>> loadRatingsTask = new Task<>() {
            @Override
            protected List<Valutazione> call() throws Exception {
                return client.visualizzaValutazioniLibro(libro.libroId());
            }

            @Override
            protected void succeeded() {
                List<Valutazione> valutazioni = getValue();
                if (valutazioni.isEmpty()) {
                    Label noRatingsLabel = new Label("Nessuna valutazione disponibile per questo libro.");
                    noRatingsLabel.getStyleClass().add("book-author");
                    section.getChildren().add(noRatingsLabel);
                } else {
                    // Calcola medie per ogni criterio
                    double mediaStile = valutazioni.stream().mapToDouble(v -> v.scoreStile()).average().orElse(0);
                    double mediaContenuto = valutazioni.stream().mapToDouble(v -> v.scoreContenuto()).average().orElse(0);
                    double mediaGradevolezza = valutazioni.stream().mapToDouble(v -> v.scoreGradevolezza()).average().orElse(0);
                    double mediaOriginalita = valutazioni.stream().mapToDouble(v -> v.scoreOriginalita()).average().orElse(0);
                    double mediaEdizione = valutazioni.stream().mapToDouble(v -> v.scoreEdizione()).average().orElse(0);
                    double mediaComplessiva = (mediaStile + mediaContenuto + mediaGradevolezza + mediaOriginalita + mediaEdizione) / 5;

                    // Mostra numero totale di valutazioni
                    Label countLabel = new Label("📊 Basato su " + valutazioni.size() + " valutazioni");
                    countLabel.getStyleClass().add("book-author");
                    section.getChildren().add(countLabel);

                    // Media complessiva prominente
                    HBox overallBox = new HBox(10);
                    overallBox.setAlignment(Pos.CENTER_LEFT);
                    Label overallLabel = new Label("Media Complessiva: ");
                    overallLabel.getStyleClass().add("book-info");
                    Label overallStars = new Label(creaStelle(mediaComplessiva) + " " + String.format("%.1f/5", mediaComplessiva));
                    overallStars.getStyleClass().add("book-title");
                    overallBox.getChildren().addAll(overallLabel, overallStars);
                    section.getChildren().add(overallBox);

                    // Valutazioni dettagliate per criterio
                    section.getChildren().addAll(
                            creaRigaValutazione("🎨 Stile", mediaStile),
                            creaRigaValutazione("📚 Contenuto", mediaContenuto),
                            creaRigaValutazione("😊 Gradevolezza", mediaGradevolezza),
                            creaRigaValutazione("💡 Originalità", mediaOriginalita),
                            creaRigaValutazione("📖 Edizione", mediaEdizione)
                    );
                }
            }

            @Override
            protected void failed() {
                Label errorLabel = new Label("Errore nel caricamento delle valutazioni: " + getException().getMessage());
                errorLabel.getStyleClass().add("book-author");
                section.getChildren().add(errorLabel);
            }
        };

        new Thread(loadRatingsTask).start();
        return section;
    }

    /**
     * Crea una riga di valutazione con stelle.
     */
    private HBox creaRigaValutazione(String criterio, double valore) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        Label criterioLabel = new Label(criterio + ":");
        criterioLabel.getStyleClass().add("book-info");
        criterioLabel.setPrefWidth(100);

        Label stelleLabel = new Label(creaStelle(valore) + " " + String.format("%.1f", valore));
        stelleLabel.getStyleClass().add("book-author");

        row.getChildren().addAll(criterioLabel, stelleLabel);
        return row;
    }

    /**
     * Genera la rappresentazione a stelle di un voto.
     */
    private String creaStelle(double voto) {
        StringBuilder stelle = new StringBuilder();
        int stellePiene = (int) voto;
        boolean mezzaStella = (voto - stellePiene) >= 0.5;

        for (int i = 0; i < stellePiene; i++) {
            stelle.append("★");
        }
        if (mezzaStella) {
            stelle.append("✨");
        }
        for (int i = stellePiene + (mezzaStella ? 1 : 0); i < 5; i++) {
            stelle.append("☆");
        }

        return stelle.toString();
    }

    /**
     * Crea la sezione con le recensioni degli utenti.
     */
    private VBox creaSezioneRecensioni(Libro libro) {
        VBox section = new VBox(10);
        section.getStyleClass().add("book-card");

        Label sectionTitle = new Label("💬 RECENSIONI UTENTI");
        sectionTitle.getStyleClass().addAll("book-title", "section-header");
        section.getChildren().add(sectionTitle);

        // Carica le recensioni dettagliate
        Task<List<Valutazione>> loadReviewsTask = new Task<>() {
            @Override
            protected List<Valutazione> call() throws Exception {
                return client.visualizzaValutazioniLibro(libro.libroId());
            }

            @Override
            protected void succeeded() {
                List<Valutazione> valutazioni = getValue();
                if (valutazioni.isEmpty()) {
                    Label noReviewsLabel = new Label("Nessuna recensione disponibile.");
                    noReviewsLabel.getStyleClass().add("book-author");
                    section.getChildren().add(noReviewsLabel);
                } else {
                    // Mostra solo le prime 5 recensioni per non appesantire l'interfaccia
                    int maxReviews = Math.min(5, valutazioni.size());
                    for (int i = 0; i < maxReviews; i++) {
                        Valutazione val = valutazioni.get(i);
                        VBox reviewCard = creaCardRecensione(val);
                        section.getChildren().add(reviewCard);
                    }

                    if (valutazioni.size() > 5) {
                        Label moreLabel = new Label("... e altre " + (valutazioni.size() - 5) + " recensioni");
                        moreLabel.getStyleClass().add("book-author");
                        section.getChildren().add(moreLabel);
                    }
                }
            }

            @Override
            protected void failed() {
                Label errorLabel = new Label("Errore nel caricamento delle recensioni: " + getException().getMessage());
                errorLabel.getStyleClass().add("book-author");
                section.getChildren().add(errorLabel);
            }
        };

        new Thread(loadReviewsTask).start();
        return section;
    }

    /**
     * Crea una card per una singola recensione.
     */
    private VBox creaCardRecensione(Valutazione valutazione) {
        VBox card = new VBox(8);
        card.getStyleClass().add("book-card");
        card.setPadding(new Insets(10));

        // Header con data e voto complessivo
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label dateLabel = new Label("📅 " + valutazione.dataValutazione().toLocalDate().toString());
        dateLabel.getStyleClass().add("book-info");

        double mediaPersonale = (valutazione.scoreStile() + valutazione.scoreContenuto() +
                valutazione.scoreGradevolezza() + valutazione.scoreOriginalita() +
                valutazione.scoreEdizione()) / 5.0;
        Label overallLabel = new Label(creaStelle(mediaPersonale) + " " + String.format("%.1f/5", mediaPersonale));
        overallLabel.getStyleClass().add("book-title");

        header.getChildren().addAll(dateLabel, new Region(), overallLabel);
        HBox.setHgrow(header.getChildren().get(1), Priority.ALWAYS);

        card.getChildren().add(header);

        // Valutazioni dettagliate (solo se hanno note)
        aggiungiNotaSePresente(card, "🎨 Stile", valutazione.scoreStile(), valutazione.noteStile());
        aggiungiNotaSePresente(card, "📚 Contenuto", valutazione.scoreContenuto(), valutazione.noteContenuto());
        aggiungiNotaSePresente(card, "😊 Gradevolezza", valutazione.scoreGradevolezza(), valutazione.noteGradevolezza());
        aggiungiNotaSePresente(card, "💡 Originalità", valutazione.scoreOriginalita(), valutazione.noteOriginalita());
        aggiungiNotaSePresente(card, "📖 Edizione", valutazione.scoreEdizione(), valutazione.noteEdizione());

        return card;
    }

    /**
     * Aggiunge una nota di valutazione se presente.
     */
    private void aggiungiNotaSePresente(VBox container, String criterio, short score, String nota) {
        if (nota != null && !nota.trim().isEmpty()) {
            HBox noteBox = new HBox(5);
            noteBox.setAlignment(Pos.TOP_LEFT);
            Label criterioLabel = new Label(criterio + " " + creaStelle(score) + ":");
            criterioLabel.getStyleClass().add("book-info");
            criterioLabel.setMinWidth(100);

            Label noteLabel = new Label(nota);
            noteLabel.getStyleClass().add("book-author");
            noteLabel.setWrapText(true);

            noteBox.getChildren().addAll(criterioLabel, noteLabel);
            container.getChildren().add(noteBox);
        }
    }

    /**
     * Crea la sezione con i libri consigliati.
     */
    private VBox creaSezioneLibriConsigliati(Libro libro) {
        VBox section = new VBox(10);
        section.getStyleClass().add("book-card");

        Label sectionTitle = new Label("🔗 LIBRI CONSIGLIATI");
        sectionTitle.getStyleClass().addAll("book-title", "section-header");
        section.getChildren().add(sectionTitle);

        // Carica i consigli in background
        Task<List<Libro>> loadRecommendationsTask = new Task<>() {
            @Override
            protected List<Libro> call() throws Exception {
                return client.generaConsigli(libro.libroId());
            }

            @Override
            protected void succeeded() {
                List<Libro> consigli = getValue();
                if (consigli.isEmpty()) {
                    Label noRecommendationsLabel = new Label("Nessun libro consigliato al momento.");
                    noRecommendationsLabel.getStyleClass().add("book-author");
                    section.getChildren().add(noRecommendationsLabel);
                } else {
                    // Mostra fino a 3 libri consigliati
                    int maxRecommendations = Math.min(3, consigli.size());
                    for (int i = 0; i < maxRecommendations; i++) {
                        Libro libroConsigliato = consigli.get(i);
                        HBox recommendationCard = creaCardConsiglio(libroConsigliato);
                        section.getChildren().add(recommendationCard);
                    }

                    if (consigli.size() > 3) {
                        Label moreLabel = new Label("... e altri " + (consigli.size() - 3) + " libri consigliati");
                        moreLabel.getStyleClass().add("book-author");
                        section.getChildren().add(moreLabel);
                    }
                }
            }

            @Override
            protected void failed() {
                Label errorLabel = new Label("Errore nel caricamento dei consigli: " + getException().getMessage());
                errorLabel.getStyleClass().add("book-author");
                section.getChildren().add(errorLabel);
            }
        };

        new Thread(loadRecommendationsTask).start();
        return section;
    }

    /**
     * Crea una card per un libro consigliato.
     */
    private HBox creaCardConsiglio(Libro libro) {
        HBox card = new HBox(10);
        card.getStyleClass().add("book-card");
        card.setPadding(new Insets(10));
        card.setAlignment(Pos.CENTER_LEFT);

        VBox bookInfo = new VBox(3);

        Label titleLabel = new Label(libro.titolo());
        titleLabel.getStyleClass().add("book-title");
        titleLabel.setWrapText(true);

        Label authorsLabel = new Label("di " + libro.autori());
        authorsLabel.getStyleClass().add("book-author");

        Label categoryLabel = new Label(libro.categoria());
        categoryLabel.getStyleClass().add("book-info");

        bookInfo.getChildren().addAll(titleLabel, authorsLabel, categoryLabel);

        Button detailsButton = new Button("Dettagli");
        detailsButton.getStyleClass().add("action-button");
        detailsButton.setOnAction(event -> {
            Stage currentStage = (Stage) card.getScene().getWindow();
            currentStage.close();
            mostraDettagliLibro(libro);
        });

        card.getChildren().addAll(bookInfo, new Region(), detailsButton);
        HBox.setHgrow(bookInfo, Priority.ALWAYS);

        return card;
    }

    /**
     * Crea la sezione con le azioni disponibili per l'utente loggato.
     */
    private VBox creaSezioneAzioniUtente(Libro libro, Stage dialogStage) {
        VBox section = new VBox(10);
        section.getStyleClass().add("book-card");

        Label sectionTitle = new Label("⚡ AZIONI");
        sectionTitle.getStyleClass().addAll("book-title", "section-header");
        section.getChildren().add(sectionTitle);

        HBox actionsBox = new HBox(10);
        actionsBox.setAlignment(Pos.CENTER);

        // Pulsante "Aggiungi alla Libreria"
        Button addToLibraryButton = new Button("📚 Aggiungi alla Libreria");
        addToLibraryButton.getStyleClass().add("action-button");
        addToLibraryButton.setOnAction(event -> mostraDialogoAggiungiLibreria(libro));        // Pulsante "Valuta Libro"
        Button rateBookButton = new Button("⭐ Valuta Libro");
        rateBookButton.getStyleClass().add("action-button");
        rateBookButton.setOnAction(event -> {
            mostraFormValutazioneLibro(libro, dialogStage);
        });

        // Pulsante "Suggerisci Libro Correlato"
        Button suggestButton = new Button("💡 Suggerisci Libro");
        suggestButton.getStyleClass().add("action-button");
        suggestButton.setOnAction(event -> mostraDialogoSuggerisciLibro(libro));

        actionsBox.getChildren().addAll(addToLibraryButton, rateBookButton, suggestButton);
        section.getChildren().add(actionsBox);

        return section;
    }

    /**
     * Mostra un dialogo per aggiungere il libro a una libreria.
     */
    private void mostraDialogoAggiungiLibreria(Libro libro) {
        // Carica prima le librerie dell'utente
        Task<List<Libreria>> loadLibrariesTask = new Task<>() {
            @Override
            protected List<Libreria> call() throws Exception {
                return client.elencaLibrerie();
            }

            @Override
            protected void succeeded() {
                List<Libreria> librerie = getValue();

                if (librerie.isEmpty()) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Nessuna Libreria");
                    alert.setHeaderText("Non hai ancora creato librerie");
                    alert.setContentText("Crea prima una libreria nella sezione Librerie per poter aggiungere libri.");
                    alert.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
                    alert.showAndWait();
                    return;
                }

                // Crea dialogo di selezione libreria con wrapper per display migliorato
                List<LibreriaDisplay> librerieDisplay = librerie.stream()
                        .map(LibreriaDisplay::new)
                        .collect(java.util.stream.Collectors.toList());

                ChoiceDialog<LibreriaDisplay> dialog = new ChoiceDialog<>(librerieDisplay.get(0), librerieDisplay);
                dialog.setTitle("Seleziona Libreria");
                dialog.setHeaderText("Aggiungi \"" + libro.titolo() + "\" alla libreria");
                dialog.setContentText("Scegli la libreria:");
                dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

                Optional<LibreriaDisplay> result = dialog.showAndWait();
                result.ifPresent(libreriaDisplay -> {
                    Libreria libreria = libreriaDisplay.getLibreria();
                    // Aggiungi il libro alla libreria selezionata
                    Task<Boolean> addBookTask = new Task<>() {
                        @Override
                        protected Boolean call() throws Exception {
                            return client.aggiungiLibroALibreria(libreria.libreriaID(), libro.libroId());
                        }

                        @Override
                        protected void succeeded() {
                            boolean success = getValue();
                            if (success) {
                                mostraMessaggioSuccesso("Libro \"" + libro.titolo() + "\" aggiunto alla libreria \"" + libreria.nomeLibreria() + "\" con successo!");
                            } else {
                                mostraMessaggioErrore("Impossibile aggiungere il libro alla libreria. Il libro potrebbe già essere presente.");
                            }
                        }

                        @Override
                        protected void failed() {
                            mostraMessaggioErrore("Errore durante l'aggiunta del libro: " + getException().getMessage());
                        }
                    };

                    new Thread(addBookTask).start();
                });
            }

            @Override
            protected void failed() {
                mostraMessaggioErrore("Errore nel caricamento delle librerie: " + getException().getMessage());
            }
        };

        new Thread(loadLibrariesTask).start();
    }

    /**
     * Mostra il form di valutazione per il libro specifico.
     */
    // Overload: con valutazione preesistente
    private void mostraFormValutazioneLibro(Libro libro, Stage ownerStage, Valutazione valutazionePreesistente) {
        if (!client.isAutenticato()) {
            stampaConAnimazione("Devi effettuare il login per valutare un libro.");
            return;
        }
        // Crea un dialogo per inserire i punteggi (ID libro già impostato)
        Dialog<Valutazione> dialog = new Dialog<>();
        dialog.setTitle("Valuta Libro");
        dialog.setHeaderText("Valuta: " + libro.titolo() + " (ID: " + libro.libroId() + ")");

        // Imposta il dialogo dei dettagli del libro come owner per mantenere la gerarchia corretta
        dialog.initOwner(ownerStage);

        // Aggiungi stile al dialogo
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        // Configura i pulsanti
        ButtonType valutaButtonType = new ButtonType(valutazionePreesistente != null ? "Aggiorna" : "Valuta", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(valutaButtonType, ButtonType.CANCEL);

        // Crea la griglia per i campi
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // Campi per i punteggi e le note

        Spinner<Integer> scoreStileSpinner = new Spinner<>(1, 5, valutazionePreesistente != null ? valutazionePreesistente.scoreStile() : 3);
        scoreStileSpinner.setEditable(true);
        TextField noteStileField = new TextField(valutazionePreesistente != null ? valutazionePreesistente.noteStile() : "");
        noteStileField.setPromptText("Note sullo stile");

        Spinner<Integer> scoreContenutoSpinner = new Spinner<>(1, 5, valutazionePreesistente != null ? valutazionePreesistente.scoreContenuto() : 3);
        scoreContenutoSpinner.setEditable(true);
        TextField noteContenutoField = new TextField(valutazionePreesistente != null ? valutazionePreesistente.noteContenuto() : "");
        noteContenutoField.setPromptText("Note sul contenuto");

        Spinner<Integer> scoreGradevolezzaSpinner = new Spinner<>(1, 5, valutazionePreesistente != null ? valutazionePreesistente.scoreGradevolezza() : 3);
        scoreGradevolezzaSpinner.setEditable(true);
        TextField noteGradevolezzaField = new TextField(valutazionePreesistente != null ? valutazionePreesistente.noteGradevolezza() : "");
        noteGradevolezzaField.setPromptText("Note sulla gradevolezza");

        Spinner<Integer> scoreOriginalitaSpinner = new Spinner<>(1, 5, valutazionePreesistente != null ? valutazionePreesistente.scoreOriginalita() : 3);
        scoreOriginalitaSpinner.setEditable(true);
        TextField noteOriginalitaField = new TextField(valutazionePreesistente != null ? valutazionePreesistente.noteOriginalita() : "");
        noteOriginalitaField.setPromptText("Note sull'originalità");

        Spinner<Integer> scoreEdizioneSpinner = new Spinner<>(1, 5, valutazionePreesistente != null ? valutazionePreesistente.scoreEdizione() : 3);
        scoreEdizioneSpinner.setEditable(true);
        TextField noteEdizioneField = new TextField(valutazionePreesistente != null ? valutazionePreesistente.noteEdizione() : "");
        noteEdizioneField.setPromptText("Note sull'edizione");

        // Aggiungi i campi alla griglia (senza campo ID libro)
        grid.add(new Label("Stile (1-5):"), 0, 0);
        grid.add(scoreStileSpinner, 1, 0);
        grid.add(new Label("Note:"), 2, 0);
        grid.add(noteStileField, 3, 0);

        grid.add(new Label("Contenuto (1-5):"), 0, 1);
        grid.add(scoreContenutoSpinner, 1, 1);
        grid.add(new Label("Note:"), 2, 1);
        grid.add(noteContenutoField, 3, 1);

        grid.add(new Label("Gradevolezza (1-5):"), 0, 2);
        grid.add(scoreGradevolezzaSpinner, 1, 2);
        grid.add(new Label("Note:"), 2, 2);
        grid.add(noteGradevolezzaField, 3, 2);

        grid.add(new Label("Originalità (1-5):"), 0, 3);
        grid.add(scoreOriginalitaSpinner, 1, 3);
        grid.add(new Label("Note:"), 2, 3);
        grid.add(noteOriginalitaField, 3, 3);

        grid.add(new Label("Edizione (1-5):"), 0, 4);
        grid.add(scoreEdizioneSpinner, 1, 4);
        grid.add(new Label("Note:"), 2, 4);
        grid.add(noteEdizioneField, 3, 4);

        dialog.getDialogPane().setContent(grid);

        // Richiedi il focus sul primo spinner
        Platform.runLater(scoreStileSpinner::requestFocus);

        // Converti il risultato
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == valutaButtonType) {

                short scoreStile = scoreStileSpinner.getValue().shortValue();
                String noteStile = noteStileField.getText().isEmpty() ? "-" : noteStileField.getText();

                short scoreContenuto = scoreContenutoSpinner.getValue().shortValue();
                String noteContenuto = noteContenutoField.getText().isEmpty() ? "-" : noteContenutoField.getText();

                short scoreGradevolezza = scoreGradevolezzaSpinner.getValue().shortValue();
                String noteGradevolezza = noteGradevolezzaField.getText().isEmpty() ? "-" : noteGradevolezzaField.getText();

                short scoreOriginalita = scoreOriginalitaSpinner.getValue().shortValue();
                String noteOriginalita = noteOriginalitaField.getText().isEmpty() ? "-" : noteOriginalitaField.getText();

                short scoreEdizione = scoreEdizioneSpinner.getValue().shortValue();
                String noteEdizione = noteEdizioneField.getText().isEmpty() ? "-" : noteEdizioneField.getText();


                // Crea un oggetto Valutazione temporaneo (con ID se aggiornamento)
                return new Valutazione(
                        valutazionePreesistente != null ? valutazionePreesistente.valutazioneID() : 0,
                        client.getUtenteAutenticato().userID(),
                        libro.libroId(),
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
                    stampaConAnimazione(valutazionePreesistente != null ? "Valutazione aggiornata con successo (ID: " + valutazioneID + ")." : "Valutazione salvata con successo (ID: " + valutazioneID + ").");
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

    // Overload per compatibilità con chiamate esistenti
    private void mostraFormValutazioneLibro(Libro libro, Stage ownerStage) {
        mostraFormValutazioneLibro(libro, ownerStage, null);
    }

    /**
     * Mostra un dialogo per suggerire un libro correlato.
     */
    private void mostraDialogoSuggerisciLibro(Libro libroRiferimento) {
        // Prima carica tutte le librerie dell'utente
        Task<List<Libreria>> loadLibrariesTask = new Task<>() {
            @Override
            protected List<Libreria> call() throws Exception {
                return client.elencaLibrerie();
            }

            @Override
            protected void succeeded() {
                List<Libreria> librerie = getValue();

                if (librerie.isEmpty()) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Nessuna Libreria");
                    alert.setHeaderText("Non hai ancora creato librerie");
                    alert.setContentText("Crea prima delle librerie e aggiungi libri per poter suggerire libri correlati.");
                    alert.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
                    alert.showAndWait();
                    return;
                }

                // Carica tutti i libri dalle librerie dell'utente
                caricaLibriDaLibrerie(librerie, libroRiferimento);
            }

            @Override
            protected void failed() {
                stampaConAnimazione("Errore nel caricamento delle librerie: " + getException().getMessage());
            }
        };

        new Thread(loadLibrariesTask).start();
    }

    /**
     * Carica tutti i libri dalle librerie dell'utente per la selezione.
     */
    private void caricaLibriDaLibrerie(List<Libreria> librerie, Libro libroRiferimento) {
        Task<List<Libro>> loadBooksTask = new Task<>() {
            @Override
            protected List<Libro> call() throws Exception {
                List<Libro> tuttiLibri = new java.util.ArrayList<>();
                for (Libreria libreria : librerie) {
                    List<Libro> libriLibreria = client.visualizzaLibreria(libreria.libreriaID());
                    tuttiLibri.addAll(libriLibreria);
                }

                // Rimuovi duplicati basandosi sull'ID del libro
                return tuttiLibri.stream()
                        .collect(java.util.stream.Collectors.toMap(
                                Libro::libroId,
                                libro -> libro,
                                (existing, replacement) -> existing
                        ))
                        .values()
                        .stream()
                        .filter(libro -> libro.libroId() != libroRiferimento.libroId()) // Escludi il libro di riferimento
                        .collect(java.util.stream.Collectors.toList());
            }

            @Override
            protected void succeeded() {
                List<Libro> libriDisponibili = getValue();

                if (libriDisponibili.isEmpty()) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Nessun Libro Disponibile");
                    alert.setHeaderText("Non ci sono libri disponibili per i suggerimenti");
                    alert.setContentText("Aggiungi libri alle tue librerie per poter suggerire libri correlati.");
                    alert.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
                    alert.showAndWait();
                    return;
                }

                // Mostra il dialogo di selezione del libro
                mostraDialogoSelezioneLibro(libriDisponibili, libroRiferimento);
            }

            @Override
            protected void failed() {
                stampaConAnimazione("Errore nel caricamento dei libri: " + getException().getMessage());
            }
        };

        new Thread(loadBooksTask).start();
    }

    /**
     * Mostra il dialogo per selezionare un libro da suggerire.
     */
    private void mostraDialogoSelezioneLibro(List<Libro> libriDisponibili, Libro libroRiferimento) {
        // Crea wrapper per display migliorato
        List<LibroDisplay> libriDisplay = libriDisponibili.stream()
                .map(LibroDisplay::new)
                .collect(java.util.stream.Collectors.toList());

        ChoiceDialog<LibroDisplay> dialog = new ChoiceDialog<>(libriDisplay.get(0), libriDisplay);
        dialog.setTitle("Suggerisci Libro Correlato");
        dialog.setHeaderText("Suggerisci un libro correlato a \"" + libroRiferimento.titolo() + "\"");
        dialog.setContentText("Scegli un libro dalle tue librerie:");
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        Optional<LibroDisplay> result = dialog.showAndWait();
        result.ifPresent(libroDisplay -> {
            Libro libroSelezionato = libroDisplay.getLibro();
            // Salva il suggerimento
            Task<Integer> suggestTask = new Task<>() {
                @Override
                protected Integer call() throws Exception {
                    return client.salvaConsiglio(libroRiferimento.libroId(), libroSelezionato.libroId());
                }

                @Override
                protected void succeeded() {
                    Integer consiglioId = getValue();
                    if (consiglioId > 0) {
                        stampaConAnimazione("Suggerimento \"" + libroSelezionato.titolo() + "\" salvato con successo per \"" + libroRiferimento.titolo() + "\".");
                    } else {
                        stampaConAnimazione("Errore nel salvataggio del suggerimento (potrebbe già esistere).");
                    }
                }

                @Override
                protected void failed() {
                    stampaConAnimazione("Errore: " + getException().getMessage());
                }
            };

            new Thread(suggestTask).start();
        });
    }

    /**
     * Mostra i dettagli di un libro dato il suo ID. Recupera prima il libro dalla cache o dal server.
     *
     * @param libroId L'ID del libro di cui mostrare i dettagli
     */
    private void mostraDettagliLibro(int libroId) {
        // Prima controlla la cache locale
        if (libriCache.containsKey(libroId)) {
            mostraDettagliLibro(libriCache.get(libroId));
            return;
        }

        // Se non è in cache, cerca nei risultati già caricati
        boolean libroTrovato = false;
        for (Node node : resultContainer.getChildren()) {
            if (node instanceof VBox) {
                VBox bookCard = (VBox) node;
                // Cerca nell'userData se è stata impostata con il libro
                if (bookCard.getUserData() instanceof Libro) {
                    Libro libro = (Libro) bookCard.getUserData();
                    if (libro.libroId() == libroId) {
                        mostraDettagliLibro(libro);
                        libroTrovato = true;
                        break;
                    }
                }
            }
        }

        if (!libroTrovato) {
            // Se il libro non è trovato localmente, effettua una chiamata al server
            try {
                Libro libro = client.ottieniDettagliLibro(libroId);
                if (libro != null) {
                    // Aggiungi alla cache per future richieste
                    libriCache.put(libroId, libro);
                    mostraDettagliLibro(libro);
                } else {
                    stampaConAnimazione("Libro con ID " + libroId + " non trovato.");
                }
            } catch (IOException e) {
                System.err.println("Errore durante il recupero dei dettagli del libro: " + e.getMessage());
                stampaConAnimazione("Errore durante il recupero dei dettagli del libro. Riprova più tardi.");
            }
        }
    }
}