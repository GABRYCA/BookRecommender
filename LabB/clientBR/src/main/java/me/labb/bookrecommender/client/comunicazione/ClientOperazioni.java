package me.labb.bookrecommender.client.comunicazione;

import me.labb.bookrecommender.client.oggetti.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Classe che fornisce le operazioni di alto livello per il client.
 * Utilizza la classe ClientComunicazione per la comunicazione con il server.
 * Implementa le operazioni principali supportate dal server.
 * <p>
 * Questa classe è stata estesa per supportare tutte le operazioni offerte dal server,
 * incluse la gestione delle librerie, valutazioni e consigli.
 */
public class ClientOperazioni {

    private final ClientComunicazione client;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Utente utenteAutenticato = null;

    /**
     * Costruttore con indirizzo e porta del server.
     *
     * @param serverAddress Indirizzo del server
     * @param serverPort    Porta del server
     */
    public ClientOperazioni(String serverAddress, int serverPort) {
        this.client = new ClientComunicazione(serverAddress, serverPort);
    }

    /**
     * Costruttore con client di comunicazione già configurato.
     *
     * @param client Client di comunicazione
     */
    public ClientOperazioni(ClientComunicazione client) {
        this.client = client;
    }

    /**
     * Stabilisce una connessione con il server.
     *
     * @return true se la connessione è stata stabilita con successo, false altrimenti
     * @throws IOException se si verifica un errore durante la connessione
     */
    public boolean connetti() throws IOException {
        return client.connetti();
    }

    /**
     * Chiude la connessione con il server.
     */
    public void chiudi() {
        client.chiudi();
    }

    /**
     * Verifica se il client è connesso al server.
     *
     * @return true se il client è connesso, false altrimenti
     */
    public boolean isConnesso() {
        return client.isConnesso();
    }

    /**
     * Verifica se l'utente è autenticato.
     *
     * @return true se l'utente è autenticato, false altrimenti
     */
    public boolean isAutenticato() {
        return utenteAutenticato != null;
    }

    /**
     * Ottiene l'utente attualmente autenticato.
     *
     * @return L'utente autenticato, o null se non c'è un utente autenticato
     */
    public Utente getUtenteAutenticato() {
        return utenteAutenticato;
    }

    /**
     * Effettua il login di un utente.
     *
     * @param username Username o email dell'utente
     * @param password Password dell'utente
     * @return true se il login è avvenuto con successo, false altrimenti
     * @throws IOException se si verifica un errore durante la comunicazione
     */
    public boolean login(String username, String password) throws IOException {
        Map<String, Object> parametri = new HashMap<>();
        parametri.put("username", username);
        parametri.put("password", password);

        String risposta = client.inviaComando("LOGIN", parametri);

        if (client.isSuccesso(risposta)) {
            Map<String, Object> dati = client.estraiDati(risposta);
            if (dati != null && dati.containsKey("utente")) {
                Map<String, Object> utenteMap = (Map<String, Object>) dati.get("utente");

                // Crea l'oggetto Utente
                utenteAutenticato = new Utente(
                        ((Number) utenteMap.get("userID")).intValue(),
                        (String) utenteMap.get("nomeCompleto"),
                        null,
                        (String) utenteMap.get("email"),
                        (String) utenteMap.get("username"),
                        null,
                        ZonedDateTime.now()
                );

                return true;
            }
        }

        return false;
    }

    /**
     * Effettua il logout dell'utente corrente.
     *
     * @return true se il logout è avvenuto con successo, false altrimenti
     * @throws IOException           se si verifica un errore durante la comunicazione
     * @throws IllegalStateException se l'utente non è autenticato
     */
    public boolean logout() throws IOException {
        if (!isAutenticato()) {
            throw new IllegalStateException("Nessun utente autenticato");
        }

        String risposta = client.inviaComando("LOGOUT", "");

        if (client.isSuccesso(risposta)) {
            utenteAutenticato = null;
            return true;
        }

        return false;
    }

    /**
     * Registra un nuovo utente.
     *
     * @param nomeCompleto  Nome completo dell'utente
     * @param email         Email dell'utente
     * @param username      Username dell'utente
     * @param password      Password dell'utente
     * @param codiceFiscale Codice fiscale dell'utente (opzionale)
     * @return L'ID dell'utente registrato, o -1 se la registrazione fallisce
     * @throws IOException se si verifica un errore durante la comunicazione
     */
    public int registra(String nomeCompleto, String email, String username, String password, String codiceFiscale) throws IOException {
        Map<String, Object> parametri = new HashMap<>();
        parametri.put("nomeCompleto", nomeCompleto);
        parametri.put("email", email);
        parametri.put("username", username);
        parametri.put("password", password);
        if (codiceFiscale != null && !codiceFiscale.isEmpty()) {
            parametri.put("codiceFiscale", codiceFiscale);
        }

        String risposta = client.inviaComando("REGISTRA", parametri);

        if (client.isSuccesso(risposta)) {
            Map<String, Object> dati = client.estraiDati(risposta);
            if (dati != null && dati.containsKey("userID")) {
                return ((Number) dati.get("userID")).intValue();
            }
        }

        return -1;
    }

    /**
     * Registra un nuovo utente senza codice fiscale.
     *
     * @param nomeCompleto Nome completo dell'utente
     * @param email        Email dell'utente
     * @param username     Username dell'utente
     * @param password     Password dell'utente
     * @return L'ID dell'utente registrato, o -1 se la registrazione fallisce
     * @throws IOException se si verifica un errore durante la comunicazione
     */
    public int registra(String nomeCompleto, String email, String username, String password) throws IOException {
        return registra(nomeCompleto, email, username, password, null);
    }

    /**
     * Ottiene le informazioni del profilo dell'utente autenticato.
     *
     * @return L'utente con le informazioni complete del profilo, o null se l'operazione fallisce
     * @throws IOException           se si verifica un errore durante la comunicazione
     * @throws IllegalStateException se l'utente non è autenticato
     */
    public Utente visualizzaProfilo() throws IOException {
        if (!isAutenticato()) {
            throw new IllegalStateException("Nessun utente autenticato");
        }

        String risposta = client.inviaComando("PROFILO", "");

        if (client.isSuccesso(risposta)) {
            Map<String, Object> dati = client.estraiDati(risposta);
            if (dati != null && dati.containsKey("profilo")) {
                Map<String, Object> profiloMap = (Map<String, Object>) dati.get("profilo");

                // Crea l'oggetto Utente con le informazioni complete
                return new Utente(
                        ((Number) profiloMap.get("userID")).intValue(),
                        (String) profiloMap.get("nome"),
                        (String) profiloMap.getOrDefault("codiceFiscale", ""),
                        (String) profiloMap.get("email"),
                        (String) profiloMap.get("username"),
                        null,
                        ZonedDateTime.parse((String) profiloMap.get("dataRegistrazione"))
                );
            }
        }

        return null;
    }

    /**
     * Cerca libri nel database in base a un termine di ricerca.
     *
     * @param termine Termine di ricerca
     * @return Lista di libri trovati, o una lista vuota se nessun libro è stato trovato
     * @throws IOException se si verifica un errore durante la comunicazione
     */
    public List<Libro> cercaLibri(String termine) throws IOException {
        String risposta = client.inviaComando("CERCA", termine);

        List<Libro> libri = new ArrayList<>();

        if (client.isSuccesso(risposta)) {
            Map<String, Object> dati = client.estraiDati(risposta);
            if (dati != null && dati.containsKey("libri")) {
                List<Map<String, Object>> libriList = (List<Map<String, Object>>) dati.get("libri");

                for (Map<String, Object> libroMap : libriList) {
                    // Crea l'oggetto Libro
                    Libro libro = new Libro(
                            (Integer) libroMap.get("id"),
                            (String) libroMap.get("titolo"),
                            (String) libroMap.get("autori"),
                            "",
                            (String) libroMap.get("categoria"),
                            "",
                            ((Number) libroMap.get("prezzo")).floatValue(),
                            "",
                            0
                    );

                    libri.add(libro);
                }
            }
        }

        return libri;
    }

    /**
     * Ottiene consigli di libri in base a una categoria.
     *
     * @param categoria Categoria per cui ottenere consigli
     * @return Lista di libri consigliati, o una lista vuota se nessun libro è stato trovato
     * @throws IOException se si verifica un errore durante la comunicazione
     */
    public List<Libro> consigliaLibri(String categoria) throws IOException {
        String risposta = client.inviaComando("CONSIGLIA", categoria);

        List<Libro> libri = new ArrayList<>();

        if (client.isSuccesso(risposta)) {
            Map<String, Object> dati = client.estraiDati(risposta);
            if (dati != null && dati.containsKey("libri")) {
                List<Map<String, Object>> libriList = (List<Map<String, Object>>) dati.get("libri");

                for (Map<String, Object> libroMap : libriList) {
                    // Crea l'oggetto Libro
                    Libro libro = new Libro(
                            (Integer) libroMap.get("id"),
                            (String) libroMap.get("titolo"),
                            (String) libroMap.get("autori"),
                            "",
                            (String) libroMap.get("categoria"),
                            "",
                            ((Number) libroMap.get("prezzo")).floatValue(),
                            "",
                            0
                    );

                    libri.add(libro);
                }
            }
        }

        return libri;
    }

    /**
     * Imposta il formato di default per le comunicazioni.
     *
     * @param formato Il formato da utilizzare (FORMAT_TEXT o FORMAT_JSON)
     * @return true se il formato è stato impostato con successo, false altrimenti
     * @throws IOException se si verifica un errore durante la comunicazione
     */
    public boolean impostaFormato(String formato) throws IOException {
        String risposta = client.inviaComando("FORMAT", formato);

        if (client.isSuccesso(risposta)) {
            client.setFormatoDefault(formato);
            return true;
        }

        return false;
    }

    /**
     * Crea una nuova libreria personale.
     *
     * @param nomeLibreria Nome della libreria da creare
     * @return L'ID della libreria creata, o -1 se la creazione fallisce
     * @throws IOException           se si verifica un errore durante la comunicazione
     * @throws IllegalStateException se l'utente non è autenticato
     */
    public int creaLibreria(String nomeLibreria) throws IOException {
        if (!isAutenticato()) {
            throw new IllegalStateException("Nessun utente autenticato");
        }

        String risposta = client.inviaComando("CREA_LIBRERIA", nomeLibreria);

        if (client.isSuccesso(risposta)) {
            Map<String, Object> dati = client.estraiDati(risposta);
            if (dati != null && dati.containsKey("libreriaID")) {
                return ((Number) dati.get("libreriaID")).intValue();
            }
        }

        return -1;
    }

    /**
     * Ottiene la lista delle librerie dell'utente autenticato.
     *
     * @return Lista delle librerie dell'utente, o una lista vuota se nessuna libreria è stata trovata
     * @throws IOException           se si verifica un errore durante la comunicazione
     * @throws IllegalStateException se l'utente non è autenticato
     */
    public List<Libreria> elencaLibrerie() throws IOException {
        if (!isAutenticato()) {
            throw new IllegalStateException("Nessun utente autenticato");
        }

        String risposta = client.inviaComando("LIBRERIE", "");

        List<Libreria> librerie = new ArrayList<>();

        if (client.isSuccesso(risposta)) {
            Map<String, Object> dati = client.estraiDati(risposta);
            if (dati != null && dati.containsKey("librerie")) {
                List<Map<String, Object>> librerieList = (List<Map<String, Object>>) dati.get("librerie");

                for (Map<String, Object> libreriaMap : librerieList) {
                    // Crea l'oggetto Libreria
                    Libreria libreria = new Libreria(
                            ((Number) libreriaMap.get("libreriaID")).intValue(),
                            ((Number) libreriaMap.get("userID")).intValue(),
                            (String) libreriaMap.get("nomeLibreria"),
                            ZonedDateTime.parse((String) libreriaMap.get("dataCreazione"))
                    );

                    librerie.add(libreria);
                }
            }
        }

        return librerie;
    }

    /**
     * Aggiunge un libro a una libreria.
     *
     * @param libreriaID ID della libreria
     * @param libroID    ID del libro da aggiungere
     * @return true se il libro è stato aggiunto con successo, false altrimenti
     * @throws IOException           se si verifica un errore durante la comunicazione
     * @throws IllegalStateException se l'utente non è autenticato
     */
    public boolean aggiungiLibroALibreria(int libreriaID, int libroID) throws IOException {
        if (!isAutenticato()) {
            throw new IllegalStateException("Nessun utente autenticato");
        }

        Map<String, Object> parametri = new HashMap<>();
        parametri.put("libreriaID", libreriaID);
        parametri.put("libroID", libroID);

        String risposta = client.inviaComando("AGGIUNGI_LIBRO", parametri);

        return client.isSuccesso(risposta);
    }

    /**
     * Rimuove un libro da una libreria.
     *
     * @param libreriaID ID della libreria
     * @param libroID    ID del libro da rimuovere
     * @return true se il libro è stato rimosso con successo, false altrimenti
     * @throws IOException           se si verifica un errore durante la comunicazione
     * @throws IllegalStateException se l'utente non è autenticato
     */
    public boolean rimuoviLibroDaLibreria(int libreriaID, int libroID) throws IOException {
        if (!isAutenticato()) {
            throw new IllegalStateException("Nessun utente autenticato");
        }

        Map<String, Object> parametri = new HashMap<>();
        parametri.put("libreriaID", libreriaID);
        parametri.put("libroID", libroID);

        String risposta = client.inviaComando("RIMUOVI_LIBRO", parametri);

        return client.isSuccesso(risposta);
    }

    /**
     * Visualizza i libri in una libreria.
     *
     * @param libreriaID ID della libreria da visualizzare
     * @return Lista dei libri nella libreria, o una lista vuota se nessun libro è stato trovato
     * @throws IOException           se si verifica un errore durante la comunicazione
     * @throws IllegalStateException se l'utente non è autenticato
     */
    public List<Libro> visualizzaLibreria(int libreriaID) throws IOException {
        if (!isAutenticato()) {
            throw new IllegalStateException("Nessun utente autenticato");
        }

        String risposta = client.inviaComando("VISUALIZZA_LIBRERIA", String.valueOf(libreriaID));

        List<Libro> libri = new ArrayList<>();

        if (client.isSuccesso(risposta)) {
            Map<String, Object> dati = client.estraiDati(risposta);
            if (dati != null && dati.containsKey("libri")) {
                List<Map<String, Object>> libriList = (List<Map<String, Object>>) dati.get("libri");

                for (Map<String, Object> libroMap : libriList) {
                    // Crea l'oggetto Libro
                    Libro libro = new Libro(
                            (Integer) libroMap.get("id"),
                            (String) libroMap.get("titolo"),
                            (String) libroMap.get("autori"),
                            (String) libroMap.getOrDefault("descrizione", ""),
                            (String) libroMap.get("categoria"),
                            (String) libroMap.getOrDefault("editore", ""),
                            ((Number) libroMap.get("prezzo")).floatValue(),
                            (String) libroMap.getOrDefault("mesePubblicazione", ""),
                            ((Number) libroMap.getOrDefault("annoPubblicazione", 0)).intValue()
                    );

                    libri.add(libro);
                }
            }
        }

        return libri;
    }

    /**
     * Valuta un libro con punteggi e note per diversi aspetti.
     *
     * @param libroID           ID del libro da valutare
     * @param scoreStile        Punteggio per lo stile (1-5)
     * @param noteStile         Note sullo stile
     * @param scoreContenuto    Punteggio per il contenuto (1-5)
     * @param noteContenuto     Note sul contenuto
     * @param scoreGradevolezza Punteggio per la gradevolezza (1-5)
     * @param noteGradevolezza  Note sulla gradevolezza
     * @param scoreOriginalita  Punteggio per l'originalità (1-5)
     * @param noteOriginalita   Note sull'originalità
     * @param scoreEdizione     Punteggio per l'edizione (1-5)
     * @param noteEdizione      Note sull'edizione
     * @return L'ID della valutazione creata, o -1 se la creazione fallisce
     * @throws IOException              se si verifica un errore durante la comunicazione
     * @throws IllegalStateException    se l'utente non è autenticato
     * @throws IllegalArgumentException se i punteggi non sono compresi tra 1 e 5
     */
    public int valutaLibro(int libroID, short scoreStile, String noteStile, short scoreContenuto, String noteContenuto,
                           short scoreGradevolezza, String noteGradevolezza, short scoreOriginalita, String noteOriginalita,
                           short scoreEdizione, String noteEdizione) throws IOException {
        if (!isAutenticato()) {
            throw new IllegalStateException("Nessun utente autenticato");
        }

        // Verifica che i punteggi siano validi (1-5)
        if (scoreStile < 1 || scoreStile > 5 ||
                scoreContenuto < 1 || scoreContenuto > 5 ||
                scoreGradevolezza < 1 || scoreGradevolezza > 5 ||
                scoreOriginalita < 1 || scoreOriginalita > 5 ||
                scoreEdizione < 1 || scoreEdizione > 5) {
            throw new IllegalArgumentException("I punteggi devono essere compresi tra 1 e 5");
        }

        Map<String, Object> parametri = new HashMap<>();
        parametri.put("libroID", libroID);
        parametri.put("scoreStile", scoreStile);
        parametri.put("noteStile", noteStile);
        parametri.put("scoreContenuto", scoreContenuto);
        parametri.put("noteContenuto", noteContenuto);
        parametri.put("scoreGradevolezza", scoreGradevolezza);
        parametri.put("noteGradevolezza", noteGradevolezza);
        parametri.put("scoreOriginalita", scoreOriginalita);
        parametri.put("noteOriginalita", noteOriginalita);
        parametri.put("scoreEdizione", scoreEdizione);
        parametri.put("noteEdizione", noteEdizione);

        String risposta = client.inviaComando("VALUTA_LIBRO", parametri);

        if (client.isSuccesso(risposta)) {
            Map<String, Object> dati = client.estraiDati(risposta);
            if (dati != null && dati.containsKey("valutazioneID")) {
                return ((Number) dati.get("valutazioneID")).intValue();
            }
        }

        return -1;
    }

    /**
     * Visualizza le valutazioni di un libro.
     *
     * @param libroID ID del libro di cui visualizzare le valutazioni
     * @return Lista delle valutazioni del libro, o una lista vuota se nessuna valutazione è stata trovata
     * @throws IOException           se si verifica un errore durante la comunicazione
     * @throws IllegalStateException se l'utente non è autenticato
     */
    public List<Valutazione> visualizzaValutazioniLibro(int libroID) throws IOException {
        if (!isAutenticato()) {
            throw new IllegalStateException("Nessun utente autenticato");
        }

        String risposta = client.inviaComando("VALUTAZIONI_LIBRO", String.valueOf(libroID));

        List<Valutazione> valutazioni = new ArrayList<>();

        if (client.isSuccesso(risposta)) {
            Map<String, Object> dati = client.estraiDati(risposta);
            if (dati != null && dati.containsKey("valutazioni")) {
                List<Map<String, Object>> valutazioniList = (List<Map<String, Object>>) dati.get("valutazioni");

                for (Map<String, Object> valutazioneMap : valutazioniList) {
                    // Crea l'oggetto Valutazione
                    Valutazione valutazione = new Valutazione(
                            ((Number) valutazioneMap.get("valutazioneID")).intValue(),
                            ((Number) valutazioneMap.get("userID")).intValue(),
                            ((Number) valutazioneMap.get("libroID")).intValue(),
                            ((Number) valutazioneMap.get("scoreStile")).shortValue(),
                            (String) valutazioneMap.get("noteStile"),
                            ((Number) valutazioneMap.get("scoreContenuto")).shortValue(),
                            (String) valutazioneMap.get("noteContenuto"),
                            ((Number) valutazioneMap.get("scoreGradevolezza")).shortValue(),
                            (String) valutazioneMap.get("noteGradevolezza"),
                            ((Number) valutazioneMap.get("scoreOriginalita")).shortValue(),
                            (String) valutazioneMap.get("noteOriginalita"),
                            ((Number) valutazioneMap.get("scoreEdizione")).shortValue(),
                            (String) valutazioneMap.get("noteEdizione"),
                            ZonedDateTime.parse((String) valutazioneMap.get("dataValutazione"))
                    );

                    valutazioni.add(valutazione);
                }
            }
        }

        return valutazioni;
    }

    /**
     * Visualizza le valutazioni dell'utente autenticato.
     *
     * @return Lista delle valutazioni dell'utente, o una lista vuota se nessuna valutazione è stata trovata
     * @throws IOException           se si verifica un errore durante la comunicazione
     * @throws IllegalStateException se l'utente non è autenticato
     */
    public List<Valutazione> visualizzaMieValutazioni() throws IOException {
        if (!isAutenticato()) {
            throw new IllegalStateException("Nessun utente autenticato");
        }

        String risposta = client.inviaComando("MIE_VALUTAZIONI", "");

        List<Valutazione> valutazioni = new ArrayList<>();

        if (client.isSuccesso(risposta)) {
            Map<String, Object> dati = client.estraiDati(risposta);
            if (dati != null && dati.containsKey("valutazioni")) {
                List<Map<String, Object>> valutazioniList = (List<Map<String, Object>>) dati.get("valutazioni");

                for (Map<String, Object> valutazioneMap : valutazioniList) {
                    // Crea l'oggetto Valutazione
                    Valutazione valutazione = new Valutazione(
                            ((Number) valutazioneMap.get("valutazioneID")).intValue(),
                            ((Number) valutazioneMap.get("userID")).intValue(),
                            ((Number) valutazioneMap.get("libroID")).intValue(),
                            ((Number) valutazioneMap.get("scoreStile")).shortValue(),
                            (String) valutazioneMap.get("noteStile"),
                            ((Number) valutazioneMap.get("scoreContenuto")).shortValue(),
                            (String) valutazioneMap.get("noteContenuto"),
                            ((Number) valutazioneMap.get("scoreGradevolezza")).shortValue(),
                            (String) valutazioneMap.get("noteGradevolezza"),
                            ((Number) valutazioneMap.get("scoreOriginalita")).shortValue(),
                            (String) valutazioneMap.get("noteOriginalita"),
                            ((Number) valutazioneMap.get("scoreEdizione")).shortValue(),
                            (String) valutazioneMap.get("noteEdizione"),
                            ZonedDateTime.parse((String) valutazioneMap.get("dataValutazione"))
                    );

                    valutazioni.add(valutazione);
                }
            }
        }

        return valutazioni;
    }

    /**
     * Genera consigli personalizzati per un libro specifico.
     *
     * @param libroID ID del libro per cui generare consigli
     * @return Lista di libri consigliati, o una lista vuota se nessun libro è stato trovato
     * @throws IOException           se si verifica un errore durante la comunicazione
     * @throws IllegalStateException se l'utente non è autenticato
     */
    public List<Libro> generaConsigli(int libroID) throws IOException {
        if (!isAutenticato()) {
            throw new IllegalStateException("Nessun utente autenticato");
        }

        String risposta = client.inviaComando("GENERA_CONSIGLI", String.valueOf(libroID));

        List<Libro> libri = new ArrayList<>();

        if (client.isSuccesso(risposta)) {
            Map<String, Object> dati = client.estraiDati(risposta);
            if (dati != null && dati.containsKey("libri")) {
                List<Map<String, Object>> libriList = (List<Map<String, Object>>) dati.get("libri");

                for (Map<String, Object> libroMap : libriList) {
                    // Crea l'oggetto Libro
                    Libro libro = new Libro(
                            (Integer) libroMap.get("libroId"),
                            (String) libroMap.get("titolo"),
                            (String) libroMap.get("autori"),
                            (String) libroMap.getOrDefault("descrizione", ""),
                            (String) libroMap.get("categoria"),
                            (String) libroMap.getOrDefault("editore", ""),
                            ((Number) libroMap.get("prezzo")).floatValue(),
                            (String) libroMap.getOrDefault("mesePubblicazione", ""),
                            ((Number) libroMap.getOrDefault("annoPubblicazione", 0)).intValue()
                    );

                    libri.add(libro);
                }
            }
        }

        return libri;
    }

    /**
     * Salva un consiglio di libro.
     *
     * @param libroRiferimentoID ID del libro di riferimento
     * @param libroSuggeritoID   ID del libro suggerito
     * @return L'ID del consiglio creato, o -1 se la creazione fallisce
     * @throws IOException           se si verifica un errore durante la comunicazione
     * @throws IllegalStateException se l'utente non è autenticato
     */
    public int salvaConsiglio(int libroRiferimentoID, int libroSuggeritoID) throws IOException {
        if (!isAutenticato()) {
            throw new IllegalStateException("Nessun utente autenticato");
        }

        Map<String, Object> parametri = new HashMap<>();
        parametri.put("libroRiferimentoID", libroRiferimentoID);
        parametri.put("libroSuggeritoID", libroSuggeritoID);

        String risposta = client.inviaComando("SALVA_CONSIGLIO", parametri);

        if (client.isSuccesso(risposta)) {
            Map<String, Object> dati = client.estraiDati(risposta);
            if (dati != null && dati.containsKey("consiglioID")) {
                return ((Number) dati.get("consiglioID")).intValue();
            }
        }

        return -1;
    }

    /**
     * Visualizza i consigli salvati dall'utente autenticato.
     *
     * @return Lista dei consigli dell'utente, o una lista vuota se nessun consiglio è stato trovato
     * @throws IOException           se si verifica un errore durante la comunicazione
     * @throws IllegalStateException se l'utente non è autenticato
     */
    public List<Consiglio> visualizzaMieiConsigli() throws IOException {
        if (!isAutenticato()) {
            throw new IllegalStateException("Nessun utente autenticato");
        }

        String risposta = client.inviaComando("MIEI_CONSIGLI", "");

        List<Consiglio> consigli = new ArrayList<>();

        if (client.isSuccesso(risposta)) {
            Map<String, Object> dati = client.estraiDati(risposta);
            if (dati != null && dati.containsKey("consigli")) {
                List<Map<String, Object>> consigliList = (List<Map<String, Object>>) dati.get("consigli");

                for (Map<String, Object> consiglioMap : consigliList) {
                    // Crea l'oggetto Consiglio
                    Consiglio consiglio = new Consiglio(
                            ((Number) consiglioMap.get("consiglioID")).intValue(),
                            ((Number) consiglioMap.get("userID")).intValue(),
                            ((Number) consiglioMap.get("libroRiferimentoID")).intValue(),
                            ((Number) consiglioMap.get("libroSuggeritoID")).intValue(),
                            ZonedDateTime.parse((String) consiglioMap.get("dataSuggerimento"))
                    );

                    consigli.add(consiglio);
                }
            }
        }

        return consigli;
    }
}
