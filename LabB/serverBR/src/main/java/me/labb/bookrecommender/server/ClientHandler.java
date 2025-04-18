package me.labb.bookrecommender.server;

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

            out.println("Benvenuto al server di BookRecommender - LabB!");

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                if ("EXIT".equalsIgnoreCase(inputLine.trim())) {
                    out.println("Arrivederci!");
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
     * Supporta sia il formato testuale che JSON.
     * 
     * @param input Il comando da elaborare (testo o JSON)
     * @return La risposta da inviare al client
     */
    private String elaboraComando(String input) {
        // Utilizza RequestParser per analizzare l'input (testo o JSON)
        RequestParser.ParsedRequest parsedRequest = RequestParser.parseRequest(input);
        String azione = parsedRequest.getComando();
        String parametri = parsedRequest.getParametri();

        // Log per debug
        if (RequestParser.isJsonRequest(input)) {
            System.out.println("Ricevuta richiesta JSON: comando=" + azione + ", parametri=" + parametri);
        }

        // Comandi disponibili a tutti
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

        // Comandi utenti autenticati.
        if (isAutenticato()) {
            switch (azione) {
                case "LOGOUT":
                    return logout();
                case "PROFILO":
                    return visualizzaProfilo();

                // Comandi per la gestione delle librerie
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

                // Comandi per la gestione delle valutazioni
                case "VALUTA_LIBRO":
                    return valutaLibro(parametri);
                case "VALUTAZIONI_LIBRO":
                    return visualizzaValutazioniLibro(parametri);
                case "MIE_VALUTAZIONI":
                    return visualizzaMieValutazioni();

                // Comandi per la gestione dei consigli
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
                  azione.equals("VISUALIZZA_LIBRERIA") || azione.equals("VALUTA_LIBRO") || 
                  azione.equals("VALUTAZIONI_LIBRO") || azione.equals("MIE_VALUTAZIONI") || 
                  azione.equals("GENERA_CONSIGLI") || azione.equals("SALVA_CONSIGLIO") || 
                  azione.equals("MIEI_CONSIGLI")) {
            return ResponseFormatter.errore("Devi effettuare il login per utilizzare questo comando.");
        }

        return ResponseFormatter.errore("Comando non riconosciuto. Digita HELP per la lista dei comandi.");
    }

    /**
     * Gestisce il logout di un utente.
     * 
     * @return Messaggio di risposta
     */
    private String logout() {
        String nomeUtente = utenteAutenticato.nomeCompleto();
        utenteAutenticato = null;
        return ResponseFormatter.successo("Logout effettuato con successo. Arrivederci, " + nomeUtente + "!");
    }

    /**
     * Visualizza informazioni profile utente.
     * 
     * @return Informazioni del profilo
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

        Map<String, Object> data = new HashMap<>();
        data.put("profilo", profiloData);

        return ResponseFormatter.successo("Informazioni profilo", data);
    }

    /**
     * Cerca libri nel database in base a dei parametri.
     * 
     * @param parametri Criteri di ricerca
     * @return Risultati della ricerca
     */
    private String cercaLibri(String parametri) {
        if (parametri.isEmpty()) {
            return ResponseFormatter.errore("Specifica un termine di ricerca.");
        }

        try (Connection conn = dbManager.getConnection()) {
            // Ricerca con Titolo e Descrizione (simili).
            String sql = """
                    SELECT "LibroID", "Titolo", "Autori", "Categoria", "Prezzo" 
                    FROM "Libri" 
                    WHERE "Titolo" ILIKE ? OR "Descrizione" ILIKE ? 
                    LIMIT 10
                    """;

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
                        return ResponseFormatter.errore("Nessun libro trovato per: " + parametri);
                    }

                    Map<String, Object> data = new HashMap<>();
                    data.put("libri", libri);
                    return ResponseFormatter.successo("Trovati " + libri.size() + " libri per: " + parametri, data);
                }
            }

        } catch (SQLException e) {
            System.err.println("Errore durante la ricerca dei libri: " + e.getMessage());
            return ResponseFormatter.errore("Errore durante la ricerca. Riprova più tardi.");
        }
    }

    /**
     * Fornisce consigli di libri in base alla categoria specificata.
     * 
     * @param categoria La categoria per cui suggerire libri
     * @return Lista di libri consigliati
     */
    private String consigliaLibri(String categoria) {
        if (categoria.isEmpty()) {
            return ResponseFormatter.errore("Specifica una categoria per le raccomandazioni.");
        }

        try (Connection conn = dbManager.getConnection()) {
            // Ricerca libri nella stessa categoria
            String sql = """
                    SELECT "LibroID", "Titolo", "Autori", "Categoria", "Prezzo" 
                    FROM "Libri" 
                    WHERE "Categoria" ILIKE ? 
                    ORDER BY RANDOM() 
                    LIMIT 5
                    """;

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                String termine = "%" + categoria + "%";
                stmt.setString(1, termine);

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
                        return ResponseFormatter.errore("Nessun libro trovato nella categoria: " + categoria);
                    }

                    Map<String, Object> data = new HashMap<>();
                    data.put("libri", libri);
                    return ResponseFormatter.successo("Libri consigliati nella categoria '" + categoria + "'", data);
                }
            }

        } catch (SQLException e) {
            System.err.println("Errore durante la ricerca di consigli: " + e.getMessage());
            return ResponseFormatter.errore("Errore durante la ricerca di consigli. Riprova più tardi.");
        }
    }

    /**
     * Login utente.
     * 
     * @param parametri Parametri del comando nel formato "username password"
     * @return Messaggio di risposta
     */
    private String login(String parametri) {
        String[] parti = parametri.split("\\s+", 2);
        if (parti.length < 2) {
            return ResponseFormatter.errore("Formato non valido. Usa: LOGIN username password");
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

                Map<String, Object> data = new HashMap<>();
                data.put("utente", userData);

                return ResponseFormatter.successo("Login effettuato con successo. Benvenuto, " + utenteAutenticato.nomeCompleto() + "!", data);
            } else {
                return ResponseFormatter.errore("Credenziali non valide. Riprova.");
            }
        } catch (SQLException e) {
            System.err.println("Errore durante il login: " + e.getMessage());
            return ResponseFormatter.errore("Errore durante il login. Riprova più tardi.");
        }
    }

    /**
     * Gestisce la registrazione di un nuovo utente.
     * 
     * @param parametri Parametri del comando nel formato "nomeCompleto email username password [codiceFiscale]"
     * @return Messaggio di risposta
     */
    private String registra(String parametri) {
        String[] parti = parametri.split("\\s+", 5);
        if (parti.length < 4) {
            return ResponseFormatter.errore("Formato non valido. Usa: REGISTRA nomeCompleto email username password [codiceFiscale]");
        }

        String nomeCompleto = parti[0];
        String email = parti[1];
        String username = parti[2];
        String password = parti[3];
        String codiceFiscale = parti.length > 4 ? parti[4] : null;

        try {
            int userID = utenteDAO.registraUtente(nomeCompleto, codiceFiscale, email, username, password);

            Map<String, Object> data = new HashMap<>();
            data.put("userID", userID);

            return ResponseFormatter.successo("Registrazione completata con successo! Ora puoi effettuare il login.", data);
        } catch (SQLException e) {
            System.err.println("Errore durante la registrazione: " + e.getMessage());
            return ResponseFormatter.errore("Errore durante la registrazione. L'username o l'email potrebbero essere già in uso.");
        }
    }

    /**
     * Verifica se l'utente è autenticato.
     * 
     * @return true se l'utente è autenticato, false altrimenti
     */
    private boolean isAutenticato() {
        return utenteAutenticato != null;
    }

    /**
     * Restituisce la lista dei comandi disponibili.
     * 
     * @return Lista dei comandi disponibili
     */
    private String getComandi() {
        List<Map<String, Object>> comandiGenerali = new ArrayList<>();
        List<Map<String, Object>> comandiAutenticazione = new ArrayList<>();
        List<Map<String, Object>> comandiAccount = new ArrayList<>();
        List<Map<String, Object>> comandiLibrerie = new ArrayList<>();
        List<Map<String, Object>> comandiValutazioni = new ArrayList<>();
        List<Map<String, Object>> comandiConsigli = new ArrayList<>();

        // Comandi generali (disponibili a tutti)
        comandiGenerali.add(createCommandInfo("CERCA", "Cerca libri con il termine indicato nel titolo o nella descrizione", "<termine>"));
        comandiGenerali.add(createCommandInfo("CONSIGLIA", "Ottieni consigli di libri in una determinata categoria", "<categoria>"));
        comandiGenerali.add(createCommandInfo("FORMAT", "Imposta il formato di risposta (TEXT o JSON)", "<formato>"));
        comandiGenerali.add(createCommandInfo("HELP", "Mostra questa lista di comandi", ""));
        comandiGenerali.add(createCommandInfo("EXIT", "Chiudi la connessione", ""));

        // Comandi di autenticazione
        if (!isAutenticato()) {
            comandiAutenticazione.add(createCommandInfo("LOGIN", "Accedi al tuo account", "<username> <password>"));
            comandiAutenticazione.add(createCommandInfo("REGISTRA", "Crea un nuovo account", "<nomeCompleto> <email> <username> <password> [codiceFiscale]"));
        } else {
            // Comandi per la gestione dell'account
            comandiAccount.add(createCommandInfo("LOGOUT", "Esci dal tuo account", ""));
            comandiAccount.add(createCommandInfo("PROFILO", "Visualizza i dettagli del tuo profilo", ""));

            // Comandi per la gestione delle librerie
            comandiLibrerie.add(createCommandInfo("CREA_LIBRERIA", "Crea una nuova libreria personale", "<nomeLibreria>"));
            comandiLibrerie.add(createCommandInfo("LIBRERIE", "Visualizza tutte le tue librerie", ""));
            comandiLibrerie.add(createCommandInfo("AGGIUNGI_LIBRO", "Aggiungi un libro a una libreria", "<libreriaID> <libroID>"));
            comandiLibrerie.add(createCommandInfo("RIMUOVI_LIBRO", "Rimuovi un libro da una libreria", "<libreriaID> <libroID>"));
            comandiLibrerie.add(createCommandInfo("VISUALIZZA_LIBRERIA", "Visualizza i libri in una libreria", "<libreriaID>"));

            // Comandi per la gestione delle valutazioni
            comandiValutazioni.add(createCommandInfo("VALUTA_LIBRO", "Valuta un libro", "<libroID> <scoreStile> <noteStile> <scoreContenuto> <noteContenuto> <scoreGradevolezza> <noteGradevolezza> <scoreOriginalita> <noteOriginalita> <scoreEdizione> <noteEdizione>"));
            comandiValutazioni.add(createCommandInfo("VALUTAZIONI_LIBRO", "Visualizza le valutazioni di un libro", "<libroID>"));
            comandiValutazioni.add(createCommandInfo("MIE_VALUTAZIONI", "Visualizza le tue valutazioni", ""));

            // Comandi per la gestione dei consigli
            comandiConsigli.add(createCommandInfo("GENERA_CONSIGLI", "Genera consigli personalizzati per un libro", "<libroID>"));
            comandiConsigli.add(createCommandInfo("SALVA_CONSIGLIO", "Salva un consiglio di libro", "<libroRiferimentoID> <libroSuggeritoID>"));
            comandiConsigli.add(createCommandInfo("MIEI_CONSIGLI", "Visualizza i tuoi consigli salvati", ""));
        }

        Map<String, Object> data = new HashMap<>();
        data.put("comandiGenerali", comandiGenerali);

        // Comunico supporto JSON
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

        return ResponseFormatter.successo("Comandi disponibili", data);
    }

    /**
     * Crea una mappa con le informazioni di un comando.
     * 
     * @param nome Nome del comando
     * @param descrizione Descrizione del comando
     * @param parametri Parametri del comando
     * @return Mappa con le informazioni del comando
     */
    private Map<String, Object> createCommandInfo(String nome, String descrizione, String parametri) {
        Map<String, Object> comando = new HashMap<>();
        comando.put("nome", nome);
        comando.put("descrizione", descrizione);
        comando.put("parametri", parametri);
        return comando;
    }

    /**
     * Imposta il formato di risposta (TEXT o JSON).
     * 
     * @param formato Il formato da impostare
     * @return Messaggio di conferma
     */
    private String impostaFormato(String formato) {
        if (formato == null || formato.isEmpty()) {
            return ResponseFormatter.errore("Specifica un formato (TEXT o JSON).");
        }

        formato = formato.toUpperCase();
        if (formato.equals(ResponseFormatter.FORMAT_TEXT) || formato.equals(ResponseFormatter.FORMAT_JSON)) {
            ResponseFormatter.setDefaultFormat(formato);
            return ResponseFormatter.successo("Formato di risposta impostato a " + formato);
        } else {
            return ResponseFormatter.errore("Formato non valido. Usa TEXT o JSON.");
        }
    }

    /**
     * Chiude tutte le risorse associate alla connessione client.
     */
    private void chiudiConnessione() {
        try {
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
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
     * @param nomeLibreria Nome della libreria da creare
     * @return Messaggio di risposta
     */
    private String creaLibreria(String nomeLibreria) {
        if (nomeLibreria.isEmpty()) {
            return "Specifica un nome per la libreria.";
        }

        try {
            int libreriaID = libreriaDAO.creaLibreria(utenteAutenticato.userID(), nomeLibreria);
            return "Libreria '" + nomeLibreria + "' creata con successo (ID: " + libreriaID + ").";
        } catch (SQLException e) {
            System.err.println("Errore durante la creazione della libreria: " + e.getMessage());
            return "Errore durante la creazione della libreria. Riprova più tardi.";
        }
    }

    /**
     * Elenca tutte le librerie dell'utente autenticato.
     * 
     * @return Lista delle librerie dell'utente
     */
    private String elencaLibrerie() {
        try {
            List<Libreria> librerie = libreriaDAO.getLibrerieUtente(utenteAutenticato.userID());

            if (librerie.isEmpty()) {
                return "Non hai ancora creato librerie. Usa il comando CREA_LIBRERIA per crearne una.";
            }

            StringBuilder risultato = new StringBuilder("Le tue librerie:\n");
            for (Libreria libreria : librerie) {
                risultato.append("ID: ").append(libreria.libreriaID())
                        .append(" | Nome: ").append(libreria.nomeLibreria())
                        .append(" | Creata il: ").append(libreria.dataCreazione())
                        .append("\n");
            }

            return risultato.toString();
        } catch (SQLException e) {
            System.err.println("Errore durante il recupero delle librerie: " + e.getMessage());
            return "Errore durante il recupero delle librerie. Riprova più tardi.";
        }
    }

    /**
     * Aggiunge un libro a una libreria dell'utente.
     * 
     * @param parametri Parametri nel formato "libreriaID libroID"
     * @return Messaggio di risposta
     */
    private String aggiungiLibro(String parametri) {
        String[] parti = parametri.split("\\s+", 2);
        if (parti.length < 2) {
            return "Formato non valido. Usa: AGGIUNGI_LIBRO libreriaID libroID";
        }

        try {
            int libreriaID = Integer.parseInt(parti[0]);
            int libroID = Integer.parseInt(parti[1]);

            // Verifica che la libreria appartenga all'utente
            Optional<Libreria> libreriaOpt = libreriaDAO.getLibreriaById(libreriaID);
            if (libreriaOpt.isEmpty() || libreriaOpt.get().userID() != utenteAutenticato.userID()) {
                return "Libreria non trovata o non hai i permessi per modificarla.";
            }

            libreriaDAO.aggiungiLibroALibreria(libreriaID, libroID);
            return "Libro aggiunto alla libreria con successo.";
        } catch (NumberFormatException e) {
            return "ID non validi. Assicurati di inserire numeri interi.";
        } catch (SQLException e) {
            System.err.println("Errore durante l'aggiunta del libro alla libreria: " + e.getMessage());
            return "Errore durante l'aggiunta del libro. Il libro potrebbe non esistere o essere già presente nella libreria.";
        }
    }

    /**
     * Rimuove un libro da una libreria dell'utente.
     * 
     * @param parametri Parametri nel formato "libreriaID libroID"
     * @return Messaggio di risposta
     */
    private String rimuoviLibro(String parametri) {
        String[] parti = parametri.split("\\s+", 2);
        if (parti.length < 2) {
            return "Formato non valido. Usa: RIMUOVI_LIBRO libreriaID libroID";
        }

        try {
            int libreriaID = Integer.parseInt(parti[0]);
            int libroID = Integer.parseInt(parti[1]);

            // Verifica che la libreria appartenga all'utente
            Optional<Libreria> libreriaOpt = libreriaDAO.getLibreriaById(libreriaID);
            if (libreriaOpt.isEmpty() || libreriaOpt.get().userID() != utenteAutenticato.userID()) {
                return "Libreria non trovata o non hai i permessi per modificarla.";
            }

            libreriaDAO.rimuoviLibroDaLibreria(libreriaID, libroID);
            return "Libro rimosso dalla libreria con successo.";
        } catch (NumberFormatException e) {
            return "ID non validi. Assicurati di inserire numeri interi.";
        } catch (SQLException e) {
            System.err.println("Errore durante la rimozione del libro dalla libreria: " + e.getMessage());
            return "Errore durante la rimozione del libro. Il libro potrebbe non essere presente nella libreria.";
        }
    }

    /**
     * Visualizza i libri in una libreria.
     * 
     * @param libreriaIDStr ID della libreria da visualizzare
     * @return Lista dei libri nella libreria
     */
    private String visualizzaLibreria(String libreriaIDStr) {
        if (libreriaIDStr.isEmpty()) {
            return "Specifica l'ID della libreria da visualizzare.";
        }

        try {
            int libreriaID = Integer.parseInt(libreriaIDStr);

            // Verifica che la libreria appartenga all'utente
            Optional<Libreria> libreriaOpt = libreriaDAO.getLibreriaById(libreriaID);
            if (libreriaOpt.isEmpty() || libreriaOpt.get().userID() != utenteAutenticato.userID()) {
                return "Libreria non trovata o non hai i permessi per visualizzarla.";
            }

            List<Libro> libri = libreriaDAO.getLibriInLibreria(libreriaID);

            if (libri.isEmpty()) {
                return "La libreria '" + libreriaOpt.get().nomeLibreria() + "' è vuota.";
            }

            StringBuilder risultato = new StringBuilder("Libri nella libreria '" + libreriaOpt.get().nomeLibreria() + "':\n\n");
            for (Libro libro : libri) {
                risultato.append("Titolo: ").append(libro.titolo()).append("\n");
                risultato.append("Autori: ").append(libro.autori()).append("\n");
                risultato.append("Categoria: ").append(libro.categoria()).append("\n");
                risultato.append("Prezzo: $").append(libro.prezzo()).append("\n");
                risultato.append("-------------------------\n");
            }

            return risultato.toString();
        } catch (NumberFormatException e) {
            return "ID non valido. Assicurati di inserire un numero intero.";
        } catch (SQLException e) {
            System.err.println("Errore durante il recupero dei libri dalla libreria: " + e.getMessage());
            return "Errore durante il recupero dei libri. Riprova più tardi.";
        }
    }

    /**
     * Valuta un libro con punteggi e note per diversi aspetti.
     * 
     * @param parametri Parametri della valutazione
     * @return Messaggio di risposta
     */
    private String valutaLibro(String parametri) {
        String[] parti = parametri.split("\\s+", 12);
        if (parti.length < 11) {
            return "Formato non valido. Usa: VALUTA_LIBRO libroID scoreStile noteStile scoreContenuto noteContenuto scoreGradevolezza noteGradevolezza scoreOriginalita noteOriginalita scoreEdizione noteEdizione";
        }

        try {
            int libroID = Integer.parseInt(parti[0]);
            short scoreStile = Short.parseShort(parti[1]);
            String noteStile = parti[2];
            short scoreContenuto = Short.parseShort(parti[3]);
            String noteContenuto = parti[4];
            short scoreGradevolezza = Short.parseShort(parti[5]);
            String noteGradevolezza = parti[6];
            short scoreOriginalita = Short.parseShort(parti[7]);
            String noteOriginalita = parti[8];
            short scoreEdizione = Short.parseShort(parti[9]);
            String noteEdizione = parti[10];

            // Verifica che i punteggi siano validi (1-5)
            if (scoreStile < 1 || scoreStile > 5 || 
                scoreContenuto < 1 || scoreContenuto > 5 || 
                scoreGradevolezza < 1 || scoreGradevolezza > 5 || 
                scoreOriginalita < 1 || scoreOriginalita > 5 || 
                scoreEdizione < 1 || scoreEdizione > 5) {
                return "I punteggi devono essere compresi tra 1 e 5.";
            }

            int valutazioneID = valutazioneDAO.salvaValutazione(
                utenteAutenticato.userID(), libroID, 
                scoreStile, noteStile, 
                scoreContenuto, noteContenuto, 
                scoreGradevolezza, noteGradevolezza, 
                scoreOriginalita, noteOriginalita, 
                scoreEdizione, noteEdizione
            );

            return "Valutazione salvata con successo (ID: " + valutazioneID + ").";
        } catch (NumberFormatException e) {
            return "Formato non valido. Assicurati che i punteggi siano numeri interi.";
        } catch (SQLException e) {
            System.err.println("Errore durante il salvataggio della valutazione: " + e.getMessage());
            return "Errore durante il salvataggio della valutazione. Riprova più tardi.";
        }
    }

    /**
     * Visualizza le valutazioni di un libro.
     * 
     * @param libroIDStr ID del libro di cui visualizzare le valutazioni
     * @return Lista delle valutazioni del libro
     */
    private String visualizzaValutazioniLibro(String libroIDStr) {
        if (libroIDStr.isEmpty()) {
            return "Specifica l'ID del libro di cui visualizzare le valutazioni.";
        }

        try {
            int libroID = Integer.parseInt(libroIDStr);
            List<Valutazione> valutazioni = valutazioneDAO.getValutazioniLibro(libroID);

            if (valutazioni.isEmpty()) {
                return "Nessuna valutazione trovata per questo libro.";
            }

            StringBuilder risultato = new StringBuilder("Valutazioni per il libro (ID: " + libroID + "):\n\n");
            for (Valutazione val : valutazioni) {
                risultato.append("Valutazione ID: ").append(val.valutazioneID()).append("\n");
                risultato.append("Stile: ").append(val.scoreStile()).append("/5 - ").append(val.noteStile()).append("\n");
                risultato.append("Contenuto: ").append(val.scoreContenuto()).append("/5 - ").append(val.noteContenuto()).append("\n");
                risultato.append("Gradevolezza: ").append(val.scoreGradevolezza()).append("/5 - ").append(val.noteGradevolezza()).append("\n");
                risultato.append("Originalità: ").append(val.scoreOriginalita()).append("/5 - ").append(val.noteOriginalita()).append("\n");
                risultato.append("Edizione: ").append(val.scoreEdizione()).append("/5 - ").append(val.noteEdizione()).append("\n");
                risultato.append("Data: ").append(val.dataValutazione()).append("\n");
                risultato.append("-------------------------\n");
            }

            return risultato.toString();
        } catch (NumberFormatException e) {
            return "ID non valido. Assicurati di inserire un numero intero.";
        } catch (SQLException e) {
            System.err.println("Errore durante il recupero delle valutazioni: " + e.getMessage());
            return "Errore durante il recupero delle valutazioni. Riprova più tardi.";
        }
    }

    /**
     * Visualizza le valutazioni dell'utente autenticato.
     * 
     * @return Lista delle valutazioni dell'utente
     */
    private String visualizzaMieValutazioni() {
        try {
            List<Valutazione> valutazioni = valutazioneDAO.getValutazioniUtente(utenteAutenticato.userID());

            if (valutazioni.isEmpty()) {
                return "Non hai ancora valutato nessun libro.";
            }

            StringBuilder risultato = new StringBuilder("Le tue valutazioni:\n\n");
            for (Valutazione val : valutazioni) {
                risultato.append("Libro ID: ").append(val.libroID()).append("\n");
                risultato.append("Stile: ").append(val.scoreStile()).append("/5 - ").append(val.noteStile()).append("\n");
                risultato.append("Contenuto: ").append(val.scoreContenuto()).append("/5 - ").append(val.noteContenuto()).append("\n");
                risultato.append("Gradevolezza: ").append(val.scoreGradevolezza()).append("/5 - ").append(val.noteGradevolezza()).append("\n");
                risultato.append("Originalità: ").append(val.scoreOriginalita()).append("/5 - ").append(val.noteOriginalita()).append("\n");
                risultato.append("Edizione: ").append(val.scoreEdizione()).append("/5 - ").append(val.noteEdizione()).append("\n");
                risultato.append("Data: ").append(val.dataValutazione()).append("\n");
                risultato.append("-------------------------\n");
            }

            return risultato.toString();
        } catch (SQLException e) {
            System.err.println("Errore durante il recupero delle valutazioni: " + e.getMessage());
            return "Errore durante il recupero delle valutazioni. Riprova più tardi.";
        }
    }

    /**
     * Genera consigli personalizzati per un libro specifico.
     * 
     * @param libroIDStr ID del libro per cui generare consigli
     * @return Lista di libri consigliati
     */
    private String generaConsigli(String libroIDStr) {
        if (libroIDStr.isEmpty()) {
            return "Specifica l'ID del libro per cui generare consigli.";
        }

        try {
            int libroID = Integer.parseInt(libroIDStr);
            int limit = 5; // Numero di consigli da generare

            List<Libro> consigliati = consiglioDAO.generaConsigliPerLibro(libroID, limit);

            if (consigliati.isEmpty()) {
                return "Nessun consiglio disponibile per questo libro.";
            }

            StringBuilder risultato = new StringBuilder("Libri consigliati in base al libro (ID: " + libroID + "):\n\n");
            for (Libro libro : consigliati) {
                risultato.append("Titolo: ").append(libro.titolo()).append("\n");
                risultato.append("Autori: ").append(libro.autori()).append("\n");
                risultato.append("Categoria: ").append(libro.categoria()).append("\n");
                risultato.append("Prezzo: $").append(libro.prezzo()).append("\n");
                risultato.append("-------------------------\n");
            }

            return risultato.toString();
        } catch (NumberFormatException e) {
            return "ID non valido. Assicurati di inserire un numero intero.";
        } catch (SQLException e) {
            System.err.println("Errore durante la generazione dei consigli: " + e.getMessage());
            return "Errore durante la generazione dei consigli. Riprova più tardi.";
        }
    }

    /**
     * Salva un consiglio di libro.
     * 
     * @param parametri Parametri nel formato "libroRiferimentoID libroSuggeritoID"
     * @return Messaggio di risposta
     */
    private String salvaConsiglio(String parametri) {
        String[] parti = parametri.split("\\s+", 2);
        if (parti.length < 2) {
            return "Formato non valido. Usa: SALVA_CONSIGLIO libroRiferimentoID libroSuggeritoID";
        }

        try {
            int libroRiferimentoID = Integer.parseInt(parti[0]);
            int libroSuggeritoID = Integer.parseInt(parti[1]);

            int consiglioID = consiglioDAO.salvaConsiglio(utenteAutenticato.userID(), libroRiferimentoID, libroSuggeritoID);
            return "Consiglio salvato con successo (ID: " + consiglioID + ").";
        } catch (NumberFormatException e) {
            return "ID non validi. Assicurati di inserire numeri interi.";
        } catch (SQLException e) {
            System.err.println("Errore durante il salvataggio del consiglio: " + e.getMessage());
            return "Errore durante il salvataggio del consiglio. Riprova più tardi.";
        }
    }

    /**
     * Visualizza i consigli salvati dall'utente autenticato.
     * 
     * @return Lista dei consigli dell'utente
     */
    private String visualizzaMieiConsigli() {
        try {
            List<Consiglio> consigli = consiglioDAO.getConsigliUtente(utenteAutenticato.userID());

            if (consigli.isEmpty()) {
                return "Non hai ancora salvato nessun consiglio.";
            }

            StringBuilder risultato = new StringBuilder("I tuoi consigli salvati:\n\n");
            for (Consiglio consiglio : consigli) {
                risultato.append("Consiglio ID: ").append(consiglio.consiglioID()).append("\n");
                risultato.append("Libro di riferimento ID: ").append(consiglio.libroRiferimentoID()).append("\n");
                risultato.append("Libro suggerito ID: ").append(consiglio.libroSuggeritoID()).append("\n");
                risultato.append("Data: ").append(consiglio.dataSuggerimento()).append("\n");
                risultato.append("-------------------------\n");
            }

            return risultato.toString();
        } catch (SQLException e) {
            System.err.println("Errore durante il recupero dei consigli: " + e.getMessage());
            return "Errore durante il recupero dei consigli. Riprova più tardi.";
        }
    }
}
