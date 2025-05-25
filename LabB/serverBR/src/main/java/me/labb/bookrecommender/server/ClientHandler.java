package me.labb.bookrecommender.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.labb.bookrecommender.server.db.*;
import me.labb.bookrecommender.server.oggetti.*;
import me.labb.bookrecommender.server.utils.RequestParser;
import me.labb.bookrecommender.server.utils.ResponseFormatter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Gestisce una connessione a un client.
 * Ogni istanza viene eseguita in un thread separato.
 */
public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private final DatabaseManager dbManager;
    private final LibroDAO libroDAO;
    private final UtenteDAO utenteDAO;
    private final ValutazioneDAO valutazioneDAO;
    private final LibreriaDAO libreriaDAO;
    private final ConsiglioDAO consiglioDAO;

    private Utente utenteAutenticato = null;

    /**
     * Crea un nuovo handler per la connessione client specificato.
     *
     * @param clientSocket Il socket della connessione client
     */
    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
        this.dbManager = DatabaseManager.getInstance();
        this.libroDAO = new LibroDAO();
        this.utenteDAO = new UtenteDAO();
        this.valutazioneDAO = new ValutazioneDAO();
        this.libreriaDAO = new LibreriaDAO();
        this.consiglioDAO = new ConsiglioDAO();
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            // Messaggio di benvenuto iniziale
            out.println("Benvenuto al server di BookRecommender - LabB!");

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                if ("EXIT".equalsIgnoreCase(inputLine.trim())) {
                    out.println("Arrivederci!"); // Messaggio di chiusura (testo semplice)
                    break;
                }

                // Elabora il comando
                String risposta = elaboraComando(inputLine);
                out.println(risposta);
            }

        } catch (IOException e) {
            System.err.println("Errore nella gestione del client: " + e.getMessage());
        } finally {
            chiudiConnessione();
        }
    }

    /**
     * Elabora comandi inviati dal client.
     * Supporta sia il formato testuale che JSON per l'input.
     * Produce risposte JSON.
     *
     * @param input Il comando da elaborare (testo o JSON)
     * @return La risposta JSON da inviare al client
     */
    private String elaboraComando(String input) {
        RequestParser.ParsedRequest parsedRequest = RequestParser.parseRequest(input);
        String azione = parsedRequest.getComando();
        String parametri = parsedRequest.getParametri();
        if (RequestParser.isJsonRequest(input)) {
            System.out.println("Ricevuta richiesta JSON: comando=" + azione + ", parametri=" + parametri);
        }
        System.out.println("Ricevuta richiesta: comando=" + azione + ", parametri=" + parametri);
        switch (azione) {
            case "CERCA":
                return cercaLibri(parametri);
            case "CONSIGLIA":
                return consigliaLibri(parametri);
            case "HELP":
                return getComandi();
            case "LOGIN":
                return login(parametri);
            case "REGISTRA":
                return registra(parametri);
            case "FORMAT":
                return impostaFormato(parametri);
        }

        if (isAutenticato()) {
            switch (azione) {
                case "LOGOUT":
                    return logout();
                case "PROFILO":
                    return visualizzaProfilo();
                case "CREA_LIBRERIA":
                    return creaLibreria(parametri);
                case "LIBRERIE":
                    return elencaLibrerie();
                case "AGGIUNGI_LIBRO":
                    return aggiungiLibro(parametri);
                case "RIMUOVI_LIBRO":
                    return rimuoviLibro(parametri);
                case "VISUALIZZA_LIBRERIA":
                    return visualizzaLibreria(parametri);
                case "ELIMINA_LIBRERIA":
                    return eliminaLibreria(parametri);
                case "VALUTA_LIBRO":
                    return valutaLibro(parametri);
                case "VALUTAZIONI_LIBRO":
                    return visualizzaValutazioniLibro(parametri);
                case "MIE_VALUTAZIONI":
                    return visualizzaMieValutazioni();
                case "GENERA_CONSIGLI":
                    return generaConsigli(parametri);
                case "SALVA_CONSIGLIO":
                    return salvaConsiglio(parametri);
                case "MIEI_CONSIGLI":
                    return visualizzaMieiConsigli();
            }
        } else if (azione.equals("LOGOUT") || azione.equals("PROFILO") ||
                azione.equals("CREA_LIBRERIA") || azione.equals("LIBRERIE") ||
                azione.equals("AGGIUNGI_LIBRO") || azione.equals("RIMUOVI_LIBRO") ||
                azione.equals("VISUALIZZA_LIBRERIA") || azione.equals("ELIMINA_LIBRERIA") ||
                azione.equals("VALUTA_LIBRO") || azione.equals("VALUTAZIONI_LIBRO") ||
                azione.equals("MIE_VALUTAZIONI") || azione.equals("GENERA_CONSIGLI") ||
                azione.equals("SALVA_CONSIGLIO") || azione.equals("MIEI_CONSIGLI")) {
            return ResponseFormatter.erroreJson("Devi effettuare il login per utilizzare questo comando.");
        }

        return ResponseFormatter.erroreJson("Comando non riconosciuto. Digita HELP per la lista dei comandi.");
    }

    /**
     * Logout dell'utente autenticato.
     *
     * @return Messaggio di successo in formato JSON
     * */
    private String logout() {
        String nomeUtente = utenteAutenticato.nomeCompleto();
        utenteAutenticato = null;
        return ResponseFormatter.successoJson("Logout effettuato con successo. Arrivederci, " + nomeUtente + "!");
    }

    /**
     * Visualizza il profilo dell'utente autenticato.
     *
     * @return Informazioni del profilo in formato JSON
     */
    private String visualizzaProfilo() {
        Map<String, Object> profiloData = new HashMap<>();
        profiloData.put("userID", utenteAutenticato.userID());
        profiloData.put("nome", utenteAutenticato.nomeCompleto());
        profiloData.put("username", utenteAutenticato.username());
        profiloData.put("email", utenteAutenticato.email());
        if (utenteAutenticato.codiceFiscale() != null && !utenteAutenticato.codiceFiscale().isEmpty()) {
            profiloData.put("codiceFiscale", utenteAutenticato.codiceFiscale());
        }
        profiloData.put("dataRegistrazione", utenteAutenticato.dataRegistrazione().toString());
        return ResponseFormatter.successoJson("Informazioni profilo", ResponseFormatter.singletonMap("profilo", profiloData));
    }

    /**
     * Cerca libri nel database in base ai parametri forniti.
     *
     * @param parametri I parametri di ricerca (titolo o descrizione)
     * @return Messaggio di successo o errore in formato JSON
     */
    private String cercaLibri(String parametri) {
        if (parametri.isEmpty()) {
            return ResponseFormatter.erroreJson("Specifica un termine di ricerca.");
        }
        try (Connection conn = dbManager.getConnection()) {
            String sql = "SELECT \"LibroID\", \"Titolo\", \"Autori\", \"Categoria\", \"Prezzo\" FROM \"Libri\" WHERE \"Titolo\" ILIKE ? OR \"Descrizione\" ILIKE ? LIMIT 10";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                String termine = "%" + parametri + "%";
                stmt.setString(1, termine);
                stmt.setString(2, termine);
                try (ResultSet rs = stmt.executeQuery()) {
                    List<Map<String, Object>> libri = new ArrayList<>();
                    while (rs.next()) {
                        Map<String, Object> libro = new HashMap<>();
                        libro.put("id", rs.getInt("LibroID"));
                        libro.put("titolo", rs.getString("Titolo"));
                        libro.put("autori", rs.getString("Autori"));
                        libro.put("categoria", rs.getString("Categoria"));
                        libro.put("prezzo", rs.getFloat("Prezzo"));
                        libri.add(libro);
                    }
                    if (libri.isEmpty()) {
                        return ResponseFormatter.erroreJson("Nessun libro trovato per: " + parametri);
                    }
                    return ResponseFormatter.successoJson("Trovati " + libri.size() + " libri per: " + parametri, ResponseFormatter.singletonMap("libri", libri));
                }
            }
        } catch (SQLException e) {
            System.err.println("Errore durante la ricerca dei libri: " + e.getMessage());
            return ResponseFormatter.erroreJson("Errore durante la ricerca. Riprova più tardi.");
        }
    }

    /**
     * Consiglia libri in base alla categoria specificata.
     *
     * @param categoria La categoria per cui consigliare libri
     * @return Messaggio di successo o errore in formato JSON
     */
    private String consigliaLibri(String categoria) {
        if (categoria.isEmpty()) {
            return ResponseFormatter.erroreJson("Specifica una categoria per le raccomandazioni.");
        }
        try (Connection conn = dbManager.getConnection()) {
            String sql = "SELECT \"LibroID\", \"Titolo\", \"Autori\", \"Categoria\", \"Prezzo\" FROM \"Libri\" WHERE \"Categoria\" ILIKE ? ORDER BY RANDOM() LIMIT 5";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, "%" + categoria + "%");
                try (ResultSet rs = stmt.executeQuery()) {
                    List<Map<String, Object>> libri = new ArrayList<>();
                    while (rs.next()) {
                        Map<String, Object> libro = new HashMap<>();
                        libro.put("id", rs.getInt("LibroID"));
                        libro.put("titolo", rs.getString("Titolo"));
                        libro.put("autori", rs.getString("Autori"));
                        libro.put("categoria", rs.getString("Categoria"));
                        libro.put("prezzo", rs.getFloat("Prezzo"));
                        libri.add(libro);
                    }
                    if (libri.isEmpty()) {
                        return ResponseFormatter.erroreJson("Nessun libro trovato nella categoria: " + categoria);
                    }
                    return ResponseFormatter.successoJson("Libri consigliati nella categoria '" + categoria + "'", ResponseFormatter.singletonMap("libri", libri));
                }
            }
        } catch (SQLException e) {
            System.err.println("Errore durante la ricerca di consigli: " + e.getMessage());
            return ResponseFormatter.erroreJson("Errore durante la ricerca di consigli. Riprova più tardi.");
        }
    }

    /**
     * Effettua il login dell'utente con le credenziali fornite.
     *
     * @param parametri I parametri di login (username e password)
     * @return Messaggio di successo o errore in formato JSON
     */
    private String login(String parametri) {
        String[] parti = parametri.split("\\s+", 2);
        if (parti.length < 2) {
            return ResponseFormatter.erroreJson("Formato non valido. Usa: LOGIN username password");
        }
        String usernameOrEmail = parti[0];
        String password = parti[1];
        try {
            Optional<Utente> utenteOpt = utenteDAO.login(usernameOrEmail, password);
            if (utenteOpt.isPresent()) {
                this.utenteAutenticato = utenteOpt.get();
                Map<String, Object> userData = new HashMap<>();
                userData.put("userID", utenteAutenticato.userID());
                userData.put("nomeCompleto", utenteAutenticato.nomeCompleto());
                userData.put("username", utenteAutenticato.username());
                userData.put("email", utenteAutenticato.email());
                return ResponseFormatter.successoJson("Login effettuato con successo. Benvenuto, " + utenteAutenticato.nomeCompleto() + "!", ResponseFormatter.singletonMap("utente", userData));
            } else {
                return ResponseFormatter.erroreJson("Credenziali non valide. Riprova.");
            }
        } catch (SQLException e) {
            System.err.println("Errore durante il login: " + e.getMessage());
            return ResponseFormatter.erroreJson("Errore durante il login. Riprova più tardi.");
        }
    }

    /**
     * Registra un nuovo utente con i parametri forniti.
     *
     * @param parametri I parametri di registrazione (nomeCompleto, email, username, password, codiceFiscale)
     * @return Messaggio di successo o errore in formato JSON
     */
    private String registra(String parametri) {
        String[] parti = parametri.split("\\s+", 5);
        if (parti.length < 4) {
            return ResponseFormatter.erroreJson("Formato non valido. Usa: REGISTRA nomeCompleto email username password [codiceFiscale]");
        }
        String nomeCompleto = parti[0];
        String email = parti[1];
        String username = parti[2];
        String password = parti[3];
        String codiceFiscale = parti.length > 4 ? parti[4] : null;
        try {
            int userID = utenteDAO.registraUtente(nomeCompleto, codiceFiscale, email, username, password);
            return ResponseFormatter.successoJson("Registrazione completata con successo! Ora puoi effettuare il login.", ResponseFormatter.singletonMap("userID", userID));
        } catch (SQLException e) {
            System.err.println("Errore durante la registrazione: " + e.getMessage());
            return ResponseFormatter.erroreJson("Errore durante la registrazione. L'username o l'email potrebbero essere già in uso.");
        }
    }

    /**
     * Controlla se l'utente è autenticato.
     *
     * @return true se l'utente è autenticato, false altrimenti
     */
    private boolean isAutenticato() {
        return utenteAutenticato != null;
    }

    /**
     * Restituisce la lista dei comandi disponibili.
     *
     * @return Messaggio JSON con i comandi
     */
    private String getComandi() {
        List<Map<String, Object>> comandiGenerali = new ArrayList<>();
        List<Map<String, Object>> comandiAutenticazione = new ArrayList<>();
        List<Map<String, Object>> comandiAccount = new ArrayList<>();
        List<Map<String, Object>> comandiLibrerie = new ArrayList<>();
        List<Map<String, Object>> comandiValutazioni = new ArrayList<>();
        List<Map<String, Object>> comandiConsigli = new ArrayList<>();

        comandiGenerali.add(createCommandInfo("CERCA", "Cerca libri con il termine indicato nel titolo o nella descrizione", "<termine>"));
        comandiGenerali.add(createCommandInfo("CONSIGLIA", "Ottieni consigli di libri in una determinata categoria", "<categoria>"));
        comandiGenerali.add(createCommandInfo("FORMAT", "Imposta il formato di risposta (TEXT o JSON)", "<formato>"));
        comandiGenerali.add(createCommandInfo("HELP", "Mostra questa lista di comandi", ""));
        comandiGenerali.add(createCommandInfo("EXIT", "Chiudi la connessione", ""));

        if (!isAutenticato()) {
            comandiAutenticazione.add(createCommandInfo("LOGIN", "Accedi al tuo account", "<username> <password>"));
            comandiAutenticazione.add(createCommandInfo("REGISTRA", "Crea un nuovo account", "<nomeCompleto> <email> <username> <password> [codiceFiscale]"));
        } else {
            comandiAccount.add(createCommandInfo("LOGOUT", "Esci dal tuo account", ""));
            comandiAccount.add(createCommandInfo("PROFILO", "Visualizza i dettagli del tuo profilo", ""));
            comandiLibrerie.add(createCommandInfo("CREA_LIBRERIA", "Crea una nuova libreria personale", "<nomeLibreria>"));
            comandiLibrerie.add(createCommandInfo("LIBRERIE", "Visualizza tutte le tue librerie", ""));
            comandiLibrerie.add(createCommandInfo("AGGIUNGI_LIBRO", "Aggiungi un libro a una libreria", "<libreriaID> <libroID>"));
            comandiLibrerie.add(createCommandInfo("RIMUOVI_LIBRO", "Rimuovi un libro da una libreria", "<libreriaID> <libroID>"));
            comandiLibrerie.add(createCommandInfo("VISUALIZZA_LIBRERIA", "Visualizza i libri in una libreria", "<libreriaID>"));
            comandiLibrerie.add(createCommandInfo("ELIMINA_LIBRERIA", "Elimina una libreria personale", "<libreriaID>"));
            comandiValutazioni.add(createCommandInfo("VALUTA_LIBRO", "Valuta un libro", "<libroID> <scoreStile> <noteStile> <scoreContenuto> <noteContenuto> <scoreGradevolezza> <noteGradevolezza> <scoreOriginalita> <noteOriginalita> <scoreEdizione> <noteEdizione>"));
            comandiValutazioni.add(createCommandInfo("VALUTAZIONI_LIBRO", "Visualizza le valutazioni di un libro", "<libroID>"));
            comandiValutazioni.add(createCommandInfo("MIE_VALUTAZIONI", "Visualizza le tue valutazioni", ""));
            comandiConsigli.add(createCommandInfo("GENERA_CONSIGLI", "Genera consigli personalizzati per un libro", "<libroID>"));
            comandiConsigli.add(createCommandInfo("SALVA_CONSIGLIO", "Salva un consiglio di libro", "<libroRiferimentoID> <libroSuggeritoID>"));
            comandiConsigli.add(createCommandInfo("MIEI_CONSIGLI", "Visualizza i tuoi consigli salvati", ""));
        }

        Map<String, Object> data = new HashMap<>();
        data.put("comandiGenerali", comandiGenerali);
        Map<String, Object> jsonInfo = new HashMap<>();
        jsonInfo.put("descrizione", "Il server supporta richieste in formato JSON con la seguente struttura");
        jsonInfo.put("esempio1", "{\"comando\": \"NOME_COMANDO\", \"parametri\": \"param1 param2\"}");
        jsonInfo.put("esempio2", "{\"comando\": \"LOGIN\", \"parametri\": {\"username\": \"utente\", \"password\": \"password123\"}}");
        jsonInfo.put("esempio3", "{\"comando\": \"VALUTA_LIBRO\", \"parametri\": {\"libroID\": 123, \"scoreStile\": 4, \"noteStile\": \"Ottimo stile\", \"scoreContenuto\": 5, \"noteContenuto\": \"Contenuto interessante\"}}");
        jsonInfo.put("nota", "I parametri possono essere una stringa o un oggetto JSON strutturato. Il server elaborerà correttamente entrambi i formati.");
        data.put("formatoJSON", jsonInfo);

        if (!isAutenticato()) {
            data.put("comandiAutenticazione", comandiAutenticazione);
        } else {
            data.put("comandiAccount", comandiAccount);
            data.put("comandiLibrerie", comandiLibrerie);
            data.put("comandiValutazioni", comandiValutazioni);
            data.put("comandiConsigli", comandiConsigli);
        }
        return ResponseFormatter.successoJson("Comandi disponibili", data);
    }

    /**
     * Crea informazioni per il comando.
     *
     * @param nome Il nome del comando
     * @param descrizione La descrizione del comando
     * @param parametri I parametri richiesti dal comando
     * @return Una mappa contenente le informazioni del comando
     */
    private Map<String, Object> createCommandInfo(String nome, String descrizione, String parametri) {
        Map<String, Object> comando = new HashMap<>();
        comando.put("nome", nome);
        comando.put("descrizione", descrizione);
        comando.put("parametri", parametri);
        return comando;
    }

    /**
     * Imposta il formato di risposta per il client.
     *
     * @param formato Il formato richiesto (TEXT o JSON)
     * @return Messaggio di successo o errore in formato JSON
     */
    private String impostaFormato(String formato) {
        if (formato == null || formato.isEmpty()) {
            return ResponseFormatter.erroreJson("Specifica un formato (TEXT o JSON).");
        }
        formato = formato.toUpperCase();
        if (formato.equals(ResponseFormatter.FORMAT_TEXT) || formato.equals(ResponseFormatter.FORMAT_JSON)) {
            return ResponseFormatter.successoJson("Comando FORMAT ricevuto. Questo client riceverà risposte in formato JSON.");
        } else {
            return ResponseFormatter.erroreJson("Formato non valido. Usa TEXT o JSON.");
        }
    }

    /**
     * Chiude la connessione con il client.
     */
    private void chiudiConnessione() {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
                System.out.println("Connessione client chiusa: " + clientSocket.getInetAddress().getHostAddress());
            }
        } catch (IOException e) {
            System.err.println("Errore durante la chiusura della connessione client: " + e.getMessage());
        }
    }

    /**
     * Crea una nuova libreria per l'utente autenticato.
     *
     * @param nomeLibreria Il nome della libreria da creare
     * @return Messaggio di successo o errore in formato JSON
     * */
    private String creaLibreria(String nomeLibreria) {
        if (nomeLibreria.isEmpty()) {
            return ResponseFormatter.erroreJson("Specifica un nome per la libreria.");
        }
        try {
            int libreriaID = libreriaDAO.creaLibreria(utenteAutenticato.userID(), nomeLibreria);
            Map<String, Object> data = new HashMap<>();
            data.put("libreriaID", libreriaID);
            return ResponseFormatter.successoJson("Libreria '" + nomeLibreria + "' creata con successo.", data);
        } catch (SQLException e) {
            System.err.println("Errore durante la creazione della libreria: " + e.getMessage());
            return ResponseFormatter.erroreJson("Errore durante la creazione della libreria. Riprova più tardi.");
        }
    }

    /**
     * Elenca le librerie dell'utente autenticato.
     *
     * @return Messaggio di successo o errore in formato JSON con l'elenco delle librerie
     */
    private String elencaLibrerie() {

        try {
            List<Libreria> librerieObj = libreriaDAO.getLibrerieUtente(utenteAutenticato.userID());
            List<Map<String, Object>> librerieData = new ArrayList<>();
            if (librerieObj.isEmpty()) {
                return ResponseFormatter.successoJson("Non hai ancora creato librerie.", ResponseFormatter.singletonMap("librerie", librerieData));
            }
            for (Libreria libreria : librerieObj) {
                Map<String, Object> libMap = new HashMap<>();
                libMap.put("userID", libreria.userID());
                libMap.put("libreriaID", libreria.libreriaID());
                libMap.put("nomeLibreria", libreria.nomeLibreria());
                libMap.put("dataCreazione", libreria.dataCreazione().toString());
                librerieData.add(libMap);
            }
            return ResponseFormatter.successoJson("Elenco delle tue librerie.", ResponseFormatter.singletonMap("librerie", librerieData));
        } catch (SQLException e) {
            System.err.println("Errore durante il recupero delle librerie: " + e.getMessage());
            return ResponseFormatter.erroreJson("Errore durante il recupero delle librerie. Riprova più tardi.");
        }
    }

    /**
     * Aggiunge un libro a una libreria specificata dall'utente autenticato.
     *
     * @param parametri I parametri per l'aggiunta del libro (libreriaID e libroID)
     * @return Messaggio di successo o errore in formato JSON
     */
    private String aggiungiLibro(String parametri) {
        String[] parti = parametri.split("\\s+", 2);
        if (parti.length < 2) {
            return ResponseFormatter.erroreJson("Formato non valido. Usa: AGGIUNGI_LIBRO libreriaID libroID");
        }
        try {
            int libreriaID = Integer.parseInt(parti[0]);
            int libroID = Integer.parseInt(parti[1]);
            Optional<Libreria> libreriaOpt = libreriaDAO.getLibreriaById(libreriaID);
            if (libreriaOpt.isEmpty() || libreriaOpt.get().userID() != utenteAutenticato.userID()) {
                return ResponseFormatter.erroreJson("Libreria non trovata o non hai i permessi per modificarla.");
            }
            libreriaDAO.aggiungiLibroALibreria(libreriaID, libroID);
            return ResponseFormatter.successoJson("Libro aggiunto alla libreria con successo.");
        } catch (NumberFormatException e) {
            return ResponseFormatter.erroreJson("ID non validi. Assicurati di inserire numeri interi.");
        } catch (SQLException e) {
            System.err.println("Errore durante l'aggiunta del libro alla libreria: " + e.getMessage());
            return ResponseFormatter.erroreJson("Errore durante l'aggiunta del libro. Il libro potrebbe non esistere, essere già presente, o la libreria non è tua.");
        }
    }

    /**
     * Rimuove un libro da una libreria specificata dall'utente autenticato.
     *
     * @param parametri I parametri per la rimozione del libro (libreriaID e libroID)
     * @return Messaggio di successo o errore in formato JSON
     */
    private String rimuoviLibro(String parametri) {
        String[] parti = parametri.split("\\s+", 2);
        if (parti.length < 2) {
            return ResponseFormatter.erroreJson("Formato non valido. Usa: RIMUOVI_LIBRO libreriaID libroID");
        }
        try {
            int libreriaID = Integer.parseInt(parti[0]);
            int libroID = Integer.parseInt(parti[1]);
            Optional<Libreria> libreriaOpt = libreriaDAO.getLibreriaById(libreriaID);
            if (libreriaOpt.isEmpty() || libreriaOpt.get().userID() != utenteAutenticato.userID()) {
                return ResponseFormatter.erroreJson("Libreria non trovata o non hai i permessi per modificarla.");
            }
            libreriaDAO.rimuoviLibroDaLibreria(libreriaID, libroID);
            return ResponseFormatter.successoJson("Libro rimosso dalla libreria con successo.");
        } catch (NumberFormatException e) {
            return ResponseFormatter.erroreJson("ID non validi. Assicurati di inserire numeri interi.");
        } catch (SQLException e) {
            System.err.println("Errore durante la rimozione del libro dalla libreria: " + e.getMessage());
            return ResponseFormatter.erroreJson("Errore durante la rimozione del libro. Il libro potrebbe non essere presente nella libreria.");
        }
    }

    /**
     * Visualizza i libri in una libreria specificata dall'utente autenticato.
     *
     * @param libreriaIDStr L'ID della libreria da visualizzare
     * @return Messaggio di successo o errore in formato JSON con i libri della libreria
     */
    private String visualizzaLibreria(String libreriaIDStr) {
        if (libreriaIDStr.isEmpty()) {
            return ResponseFormatter.erroreJson("Specifica l'ID della libreria da visualizzare.");
        }
        try {
            int libreriaID = Integer.parseInt(libreriaIDStr);
            Optional<Libreria> libreriaOpt = libreriaDAO.getLibreriaById(libreriaID);
            if (libreriaOpt.isEmpty() || libreriaOpt.get().userID() != utenteAutenticato.userID()) {
                return ResponseFormatter.erroreJson("Libreria non trovata o non hai i permessi per visualizzarla.");
            }

            List<Libro> libriObj = libreriaDAO.getLibriInLibreria(libreriaID);
            List<Map<String, Object>> libriData = new ArrayList<>();
            Map<String, Object> dataResponse = new HashMap<>();
            dataResponse.put("libreriaID", libreriaID);
            dataResponse.put("nomeLibreria", libreriaOpt.get().nomeLibreria());

            if (libriObj.isEmpty()) {
                dataResponse.put("libri", libriData); // Lista vuota
                return ResponseFormatter.successoJson("La libreria '" + libreriaOpt.get().nomeLibreria() + "' è vuota.", dataResponse);
            }

            for (Libro libro : libriObj) {
                Map<String, Object> libroMap = new HashMap<>();
                libroMap.put("libroID", libro.libroId());
                libroMap.put("titolo", libro.titolo());
                libroMap.put("autori", libro.autori());
                libroMap.put("categoria", libro.categoria());
                libroMap.put("prezzo", libro.prezzo());
                libriData.add(libroMap);
            }
            dataResponse.put("libri", libriData);
            return ResponseFormatter.successoJson("Libri nella libreria '" + libreriaOpt.get().nomeLibreria() + "'.", dataResponse);
        } catch (NumberFormatException e) {
            return ResponseFormatter.erroreJson("ID libreria non valido. Assicurati di inserire un numero intero.");
        } catch (SQLException e) {
            System.err.println("Errore durante il recupero dei libri dalla libreria: " + e.getMessage());
            return ResponseFormatter.erroreJson("Errore durante il recupero dei libri. Riprova più tardi.");
        }
    }

    /**
     * Elimina una libreria dell'utente autenticato.
     *
     * @param libreriaID L'ID della libreria da eliminare
     * @return Messaggio di successo o errore in formato JSON
     */
    private String eliminaLibreria(String libreriaID) {
        try {
            int libreriaIDInt = Integer.parseInt(libreriaID);
            Optional<Libreria> libreriaOpt = libreriaDAO.getLibreriaById(libreriaIDInt);
            if (libreriaOpt.isEmpty() || libreriaOpt.get().userID() != utenteAutenticato.userID()) {
                return ResponseFormatter.erroreJson("Libreria non trovata o non hai i permessi per eliminarla.");
            }

            String nomeLibreria = libreriaOpt.get().nomeLibreria();
            libreriaDAO.eliminaLibreria(libreriaIDInt);
            return ResponseFormatter.successoJson("Libreria '" + nomeLibreria + "' eliminata con successo.");
        } catch (NumberFormatException e) {
            return ResponseFormatter.erroreJson("ID libreria non valido. Assicurati di inserire un numero intero.");
        } catch (SQLException e) {
            System.err.println("Errore durante l'eliminazione della libreria: " + e.getMessage());
            return ResponseFormatter.erroreJson("Errore durante l'eliminazione della libreria. Riprova più tardi.");
        }
    }

    /**
     * Valuta un libro con i punteggi e le note fornite.
     *
     * @param parametri
     * @return
     */
    private String valutaLibro(String parametri) {
        Map<String, Object> paramsMap = null;
        if (parametri != null && parametri.trim().startsWith("{") && parametri.trim().endsWith("}")) {
            try {
                paramsMap = new ObjectMapper().readValue(parametri, HashMap.class);
            } catch (JsonProcessingException e) {
                System.err.println("Parametri per VALUTA_LIBRO sembravano JSON ma non parsabili: " + e.getMessage());
            }
        }

        try {
            int libroID;
            short scoreStile, scoreContenuto, scoreGradevolezza, scoreOriginalita, scoreEdizione;
            String noteStile, noteContenuto, noteGradevolezza, noteOriginalita, noteEdizione;

            if (paramsMap != null) {
                if (!paramsMap.containsKey("libroID") || !paramsMap.containsKey("scoreStile")) {
                    return ResponseFormatter.erroreJson("Parametri JSON incompleti per VALUTA_LIBRO.");
                }
                libroID = ((Number) paramsMap.get("libroID")).intValue();
                scoreStile = ((Number) paramsMap.get("scoreStile")).shortValue();
                noteStile = (String) paramsMap.get("noteStile");
                scoreContenuto = ((Number) paramsMap.get("scoreContenuto")).shortValue();
                noteContenuto = (String) paramsMap.get("noteContenuto");
                scoreGradevolezza = ((Number) paramsMap.get("scoreGradevolezza")).shortValue();
                noteGradevolezza = (String) paramsMap.get("noteGradevolezza");
                scoreOriginalita = ((Number) paramsMap.get("scoreOriginalita")).shortValue();
                noteOriginalita = (String) paramsMap.get("noteOriginalita");
                scoreEdizione = ((Number) paramsMap.get("scoreEdizione")).shortValue();
                noteEdizione = (String) paramsMap.get("noteEdizione");
            } else {
                String[] parti = parametri.split("\\s+");
                if (parti.length < 11) {
                    return ResponseFormatter.erroreJson("Formato non valido o parametri insufficienti. Devono esserci 11 parametri (libroID, 5 coppie score/nota). Le note non devono contenere spazi.");
                }
                libroID = Integer.parseInt(parti[0]);
                scoreStile = Short.parseShort(parti[1]);
                noteStile = parti[2];
                scoreContenuto = Short.parseShort(parti[3]);
                noteContenuto = parti[4];
                scoreGradevolezza = Short.parseShort(parti[5]);
                noteGradevolezza = parti[6];
                scoreOriginalita = Short.parseShort(parti[7]);
                noteOriginalita = parti[8];
                scoreEdizione = Short.parseShort(parti[9]);
                noteEdizione = parti[10];
            }

            if (scoreStile < 1 || scoreStile > 5 || scoreContenuto < 1 || scoreContenuto > 5 ||
                    scoreGradevolezza < 1 || scoreGradevolezza > 5 || scoreOriginalita < 1 || scoreOriginalita > 5 ||
                    scoreEdizione < 1 || scoreEdizione > 5) {
                return ResponseFormatter.erroreJson("I punteggi devono essere compresi tra 1 e 5.");
            }

            int valutazioneID = valutazioneDAO.salvaValutazione(
                    utenteAutenticato.userID(), libroID,
                    scoreStile, noteStile, scoreContenuto, noteContenuto,
                    scoreGradevolezza, noteGradevolezza, scoreOriginalita, noteOriginalita,
                    scoreEdizione, noteEdizione
            );
            return ResponseFormatter.successoJson("Valutazione salvata con successo.", ResponseFormatter.singletonMap("valutazioneID", valutazioneID));

        } catch (NumberFormatException e) {
            return ResponseFormatter.erroreJson("Formato parametri non valido. Assicurati che ID libro e punteggi siano numeri.");
        } catch (ClassCastException | NullPointerException e) {
            return ResponseFormatter.erroreJson("Errore nel formato dei parametri JSON per VALUTA_LIBRO.");
        } catch (SQLException e) {
            System.err.println("Errore durante il salvataggio della valutazione: " + e.getMessage());
            return ResponseFormatter.erroreJson("Errore durante il salvataggio della valutazione. Riprova più tardi.");
        }
    }

    /**
     * Visualizza le valutazioni di un libro specificato dall'utente.
     *
     * @param libroIDStr L'ID del libro di cui visualizzare le valutazioni
     * @return Messaggio di successo o errore in formato JSON con le valutazioni del libro
     */
    private String visualizzaValutazioniLibro(String libroIDStr) {
        if (libroIDStr.isEmpty()) {
            return ResponseFormatter.erroreJson("Specifica l'ID del libro di cui visualizzare le valutazioni.");
        }
        try {
            int libroID = Integer.parseInt(libroIDStr);
            List<Valutazione> valutazioniObj = valutazioneDAO.getValutazioniLibro(libroID);
            List<Map<String, Object>> valutazioniData = new ArrayList<>();
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("libroID", libroID);

            if (valutazioniObj.isEmpty()) {
                responseData.put("valutazioni", valutazioniData);
                return ResponseFormatter.successoJson("Nessuna valutazione trovata per il libro ID: " + libroID, responseData);
            }
            for (Valutazione val : valutazioniObj) {
                Map<String, Object> valMap = new HashMap<>();
                valMap.put("valutazioneID", val.valutazioneID());
                valMap.put("userID", val.userID());
                valMap.put("scoreStile", val.scoreStile());
                valMap.put("noteStile", val.noteStile());
                valMap.put("scoreContenuto", val.scoreContenuto());
                valMap.put("noteContenuto", val.noteContenuto());
                valMap.put("scoreGradevolezza", val.scoreGradevolezza());
                valMap.put("noteGradevolezza", val.noteGradevolezza());
                valMap.put("scoreOriginalita", val.scoreOriginalita());
                valMap.put("noteOriginalita", val.noteOriginalita());
                valMap.put("scoreEdizione", val.scoreEdizione());
                valMap.put("noteEdizione", val.noteEdizione());
                valMap.put("dataValutazione", val.dataValutazione().toString());
                valutazioniData.add(valMap);
            }
            responseData.put("valutazioni", valutazioniData);
            return ResponseFormatter.successoJson("Valutazioni per il libro ID: " + libroID, responseData);
        } catch (NumberFormatException e) {
            return ResponseFormatter.erroreJson("ID libro non valido. Assicurati di inserire un numero intero.");
        } catch (SQLException e) {
            System.err.println("Errore durante il recupero delle valutazioni: " + e.getMessage());
            return ResponseFormatter.erroreJson("Errore durante il recupero delle valutazioni. Riprova più tardi.");
        }
    }

    /**
     * Visualizza le valutazioni fatte dall'utente autenticato.
     *
     * @return Messaggio di successo o errore in formato JSON con le valutazioni dell'utente
     */
    private String visualizzaMieValutazioni() {
        try {
            List<Valutazione> valutazioniObj = valutazioneDAO.getValutazioniUtente(utenteAutenticato.userID());
            List<Map<String, Object>> valutazioniData = new ArrayList<>();
            if (valutazioniObj.isEmpty()) {
                return ResponseFormatter.successoJson("Non hai ancora valutato nessun libro.", ResponseFormatter.singletonMap("valutazioni", valutazioniData));
            }
            for (Valutazione val : valutazioniObj) {
                Map<String, Object> valMap = new HashMap<>();
                valMap.put("valutazioneID", val.valutazioneID());
                valMap.put("libroID", val.libroID());
                libroDAO.getLibroById(val.libroID()).ifPresent(l -> valMap.put("titoloLibro", l.titolo()));
                valMap.put("scoreStile", val.scoreStile());
                valMap.put("noteStile", val.noteStile());
                valMap.put("scoreContenuto", val.scoreContenuto());
                valMap.put("noteContenuto", val.noteContenuto());
                valMap.put("scoreGradevolezza", val.scoreGradevolezza());
                valMap.put("noteGradevolezza", val.noteGradevolezza());
                valMap.put("scoreOriginalita", val.scoreOriginalita());
                valMap.put("noteOriginalita", val.noteOriginalita());
                valMap.put("scoreEdizione", val.scoreEdizione());
                valMap.put("noteEdizione", val.noteEdizione());
                valMap.put("dataValutazione", val.dataValutazione().toString());
                valutazioniData.add(valMap);
            }
            return ResponseFormatter.successoJson("Le tue valutazioni.", ResponseFormatter.singletonMap("valutazioni", valutazioniData));
        } catch (SQLException e) {
            System.err.println("Errore durante il recupero delle tue valutazioni: " + e.getMessage());
            return ResponseFormatter.erroreJson("Errore durante il recupero delle tue valutazioni. Riprova più tardi.");
        }
    }

    /**
     * Genera consigli di libri basati su un libro specificato dall'utente.
     *
     * @param libroIDStr L'ID del libro per cui generare i consigli
     * @return Messaggio di successo o errore in formato JSON con i libri consigliati
     */
    private String generaConsigli(String libroIDStr) {
        if (libroIDStr.isEmpty()) {
            return ResponseFormatter.erroreJson("Specifica l'ID del libro per cui generare consigli.");
        }
        try {
            int libroID = Integer.parseInt(libroIDStr);
            List<Libro> consigliatiObj = consiglioDAO.generaConsigliPerLibro(libroID, 5);
            List<Map<String, Object>> libriConsigliatiData = new ArrayList<>();
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("libroRiferimentoID", libroID);

            if (consigliatiObj.isEmpty()) {
                responseData.put("consigli", libriConsigliatiData);
                return ResponseFormatter.successoJson("Nessun consiglio disponibile per il libro ID: " + libroID, responseData);
            }
            for (Libro libro : consigliatiObj) {
                Map<String, Object> libroMap = new HashMap<>();
                libroMap.put("libroID", libro.libroId());
                libroMap.put("titolo", libro.titolo());
                libroMap.put("autori", libro.autori());
                libroMap.put("categoria", libro.categoria());
                libroMap.put("prezzo", libro.prezzo());
                libriConsigliatiData.add(libroMap);
            }
            responseData.put("consigli", libriConsigliatiData);
            return ResponseFormatter.successoJson("Libri consigliati in base al libro ID: " + libroID, responseData);
        } catch (NumberFormatException e) {
            return ResponseFormatter.erroreJson("ID libro non valido. Assicurati di inserire un numero intero.");
        } catch (SQLException e) {
            System.err.println("Errore durante la generazione dei consigli: " + e.getMessage());
            return ResponseFormatter.erroreJson("Errore durante la generazione dei consigli. Riprova più tardi.");
        }
    }

    /**
     * Salva un consiglio di libro tra due libri specificati dall'utente autenticato.
     *
     * @param parametri I parametri per il salvataggio del consiglio (libroRiferimentoID e libroSuggeritoID)
     * @return Messaggio di successo o errore in formato JSON
     */
    private String salvaConsiglio(String parametri) {
        String[] parti = parametri.split("\\s+", 2);
        if (parti.length < 2) {
            return ResponseFormatter.erroreJson("Formato non valido. Usa: SALVA_CONSIGLIO libroRiferimentoID libroSuggeritoID");
        }
        try {
            int libroRiferimentoID = Integer.parseInt(parti[0]);
            int libroSuggeritoID = Integer.parseInt(parti[1]);
            if (libroRiferimentoID == libroSuggeritoID) {
                return ResponseFormatter.erroreJson("Un libro non può essere consigliato a se stesso.");
            }
            // Ulteriori controlli: esistenza libri
            if (libroDAO.getLibroById(libroRiferimentoID).isEmpty() || libroDAO.getLibroById(libroSuggeritoID).isEmpty()) {
                return ResponseFormatter.erroreJson("Uno o entrambi i libri specificati non esistono.");
            }

            int consiglioID = consiglioDAO.salvaConsiglio(utenteAutenticato.userID(), libroRiferimentoID, libroSuggeritoID);
            return ResponseFormatter.successoJson("Consiglio salvato con successo.", ResponseFormatter.singletonMap("consiglioID", consiglioID));
        } catch (NumberFormatException e) {
            return ResponseFormatter.erroreJson("ID non validi. Assicurati di inserire numeri interi.");
        } catch (SQLException e) {
            System.err.println("Errore durante il salvataggio del consiglio: " + e.getMessage());
            return ResponseFormatter.erroreJson("Errore durante il salvataggio del consiglio. Potrebbe essere un duplicato o i libri non validi.");
        }
    }

    /**
     * Visualizza i consigli salvati dall'utente autenticato.
     *
     * @return Messaggio di successo o errore in formato JSON con i consigli dell'utente
     */
    private String visualizzaMieiConsigli() {
        try {
            List<Consiglio> consigliObj = consiglioDAO.getConsigliUtente(utenteAutenticato.userID());
            List<Map<String, Object>> consigliData = new ArrayList<>();
            if (consigliObj.isEmpty()) {
                return ResponseFormatter.successoJson("Non hai ancora salvato nessun consiglio.", ResponseFormatter.singletonMap("consigli", consigliData));
            }
            for (Consiglio consiglio : consigliObj) {
                Map<String, Object> consiglioMap = new HashMap<>();
                consiglioMap.put("consiglioID", consiglio.consiglioID());
                consiglioMap.put("libroRiferimentoID", consiglio.libroRiferimentoID());
                libroDAO.getLibroById(consiglio.libroRiferimentoID()).ifPresent(l -> consiglioMap.put("titoloLibroRiferimento", l.titolo()));
                consiglioMap.put("libroSuggeritoID", consiglio.libroSuggeritoID());
                libroDAO.getLibroById(consiglio.libroSuggeritoID()).ifPresent(l -> consiglioMap.put("titoloLibroSuggerito", l.titolo()));
                consiglioMap.put("dataSuggerimento", consiglio.dataSuggerimento().toString());
                consigliData.add(consiglioMap);
            }
            return ResponseFormatter.successoJson("I tuoi consigli salvati.", ResponseFormatter.singletonMap("consigli", consigliData));
        } catch (SQLException e) {
            System.err.println("Errore durante il recupero dei tuoi consigli: " + e.getMessage());
            return ResponseFormatter.erroreJson("Errore durante il recupero dei tuoi consigli. Riprova più tardi.");
        }
    }
}