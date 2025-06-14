package me.labb.bookrecommender.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
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
 *
 * @author Caretti Gabriele 756564 VA
 * @author Como Riccardo 758697 VA
 * @author Manicone Giorgia 758716 VA
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
            case "DETTAGLI_LIBRO":
                return dettagliLibro(parametri);
            case "CATEGORIE":
                return getCategorie();
            case "CERCA_PER_CATEGORIA":
                return gestisciCercaPerCategoria(parametri);
            case "CERCA_PER_AUTORE":
                return cercaPerAutore(parametri);
            case "CERCA_PER_ANNO":
                return cercaPerAnno(parametri);
            case "CERCA_PER_AUTORE_E_ANNO":
                return cercaPerAutoreEAnno(parametri);
            case "HELP":
                return getComandi();
            case "LOGIN":
                return login(parametri);
            case "REGISTRA":
                // Per REGISTRA manteniamo il JSON originale se la richiesta è JSON
                if (RequestParser.isJsonRequest(input)) {
                    // Estraiamo la parte JSON dai parametri originali
                    String jsonParams = estraiJsonDaInput(input);
                    return registra(jsonParams);
                } else {
                    // Se non è JSON, usa i parametri normali
                    return registra(parametri);
                }
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
                case "SPOSTA_LIBRO":
                    return spostaLibro(parametri);
                case "VISUALIZZA_LIBRERIA":
                    return visualizzaLibreria(parametri);
                case "ELIMINA_LIBRERIA":
                    return eliminaLibreria(parametri);
                case "RINOMINA_LIBRERIA":
                    return rinominaLibreria(parametri);
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
                azione.equals("RINOMINA_LIBRERIA") ||
                azione.equals("VALUTA_LIBRO") || azione.equals("VALUTAZIONI_LIBRO") ||
                azione.equals("MIE_VALUTAZIONI") || azione.equals("GENERA_CONSIGLI") ||
                azione.equals("SALVA_CONSIGLIO") || azione.equals("MIEI_CONSIGLI")) {
            return ResponseFormatter.erroreJson("Devi effettuare il login per utilizzare questo comando.");
        }

        return ResponseFormatter.erroreJson("Comando non riconosciuto. Digita HELP per la lista dei comandi.");
    }

    // Metodo helper per estrarre il JSON dai parametri
    private String estraiJsonDaInput(String input) {
        try {
            // Trova l'inizio del JSON (dopo "parametri":")
            int startIndex = input.indexOf("\"parametri\":") + "\"parametri\":".length();

            // Trova la fine del JSON (l'ultima })
            int endIndex = input.lastIndexOf("}");

            if (startIndex > 0 && endIndex > startIndex) {
                return input.substring(startIndex, endIndex + 1).trim();
            }

            // Fallback: restituisce i parametri normali se non riesce ad estrarre il JSON
            RequestParser.ParsedRequest parsedRequest = RequestParser.parseRequest(input);
            return parsedRequest.getParametri();

        } catch (Exception e) {
            System.err.println("Errore nell'estrazione del JSON: " + e.getMessage());
            RequestParser.ParsedRequest parsedRequest = RequestParser.parseRequest(input);
            return parsedRequest.getParametri();
        }
    }

    private String cercaPerAnno(String parametri) {
        if (parametri.isEmpty()) {
            return ResponseFormatter.erroreJson("Specifica un anno per la ricerca.");
        }
        try (Connection conn = dbManager.getConnection()) {
            String sql = """
                    SELECT "LibroID", "Titolo", "Autori", "Categoria", "Prezzo"
                    FROM "Libri"
                    WHERE "AnnoPubblicazione" = ?
                    LIMIT 10
                    """;
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, Integer.parseInt(parametri));
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
                        return ResponseFormatter.erroreJson("Nessun libro trovato per l'anno: " + parametri);
                    }
                    return ResponseFormatter.successoJson("Trovati " + libri.size() + " libri per l'anno: " + parametri, ResponseFormatter.singletonMap("libri", libri));
                }
            }
        } catch (SQLException e) {
            System.err.println("Errore durante la ricerca dei libri per anno: " + e.getMessage());
            return ResponseFormatter.erroreJson("Errore durante la ricerca. Riprova più tardi.");
        }
    }

    private String cercaPerAutoreEAnno(String parametri) {
        if (parametri.isEmpty()) {
            return ResponseFormatter.erroreJson("Specifica un autore e un anno per la ricerca.");
        }
        String[] parts = parametri.split("\\s+");
        if (parts.length < 2) {
            return ResponseFormatter.erroreJson("Formato non valido. Usa: AUTORE ANNO");
        }
        String autore = parts[0];
        int anno = Integer.parseInt(parts[1]); // Da testare

        try (Connection conn = dbManager.getConnection()) {
            String sql = """
                    SELECT "LibroID", "Titolo", "Autori", "Categoria", "Prezzo"
                    FROM "Libri"
                    WHERE "Autori" ILIKE ? AND "AnnoPubblicazione" = ?
                    LIMIT 10
                    """;
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, "%" + autore + "%");
                stmt.setInt(2, anno);
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
                        return ResponseFormatter.erroreJson("Nessun libro trovato per l'autore: " + autore + " e l'anno: " + anno);
                    }
                    return ResponseFormatter.successoJson("Trovati " + libri.size() + " libri per l'autore: " + autore + " e l'anno: " + anno, ResponseFormatter.singletonMap("libri", libri));
                }
            }
        } catch (SQLException e) {
            System.err.println("Errore durante la ricerca dei libri per autore e anno: " + e.getMessage());
            return ResponseFormatter.erroreJson("Errore durante la ricerca. Riprova più tardi.");
        }
    }

    private String cercaPerAutore(String parametri) {
        if (parametri.isEmpty()) {
            return ResponseFormatter.erroreJson("Specifica un autore per la ricerca.");
        }
        try (Connection conn = dbManager.getConnection()) {
            String sql = """
                    SELECT "LibroID", "Titolo", "Autori", "Categoria", "Prezzo"
                    FROM "Libri"
                    WHERE "Autori" ILIKE ?
                    LIMIT 10
                    """;
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                String termine = "%" + parametri + "%";
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
                        return ResponseFormatter.erroreJson("Nessun libro trovato per l'autore: " + parametri);
                    }
                    return ResponseFormatter.successoJson("Trovati " + libri.size() + " libri per l'autore: " + parametri, ResponseFormatter.singletonMap("libri", libri));
                }
            }
        } catch (SQLException e) {
            System.err.println("Errore durante la ricerca dei libri per autore: " + e.getMessage());
            return ResponseFormatter.erroreJson("Errore durante la ricerca. Riprova più tardi.");
        }
    }

    /**
     * Logout dell'utente autenticato.
     *
     * @return Messaggio di successo in formato JSON
     */
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
     * @param parametri I parametri di ricerca (titolo)
     * @return Messaggio di successo o errore in formato JSON
     */
    private String cercaLibri(String parametri) {
        if (parametri.isEmpty()) {
            return ResponseFormatter.erroreJson("Specifica un termine di ricerca.");
        }
        try (Connection conn = dbManager.getConnection()) {
            String sql = """
                    SELECT "LibroID", "Titolo", "Autori", "Categoria", "Prezzo"
                    FROM "Libri"
                    WHERE "Titolo" ILIKE ?
                    LIMIT 10
                    """;
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                String termine = "%" + parametri + "%";
                stmt.setString(1, termine);
                //stmt.setString(2, termine);
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
     * Gestisce la richiesta di ricerca libri in base alla categoria e, opzionalmente, al titolo.
     * <p>
     * La stringa in input deve contenere la categoria come primo termine,
     * seguita opzionalmente dal titolo, separati dal carattere ">".
     * Se viene specificato solo un valore, la ricerca sarà effettuata solo per categoria.
     * Se viene specificato anche un secondo valore, la ricerca sarà effettuata per categoria e titolo.
     *
     * @param categoriaEAltro Stringa nel formato "Categoria>Titolo" (es. "Horror>Dracula")
     * @return JSON con i libri trovati o un messaggio di errore
     */
    private String gestisciCercaPerCategoria(String categoriaEAltro) {
        if (categoriaEAltro == null || categoriaEAltro.trim().isEmpty()) {
            return ResponseFormatter.erroreJson("Specifica almeno una categoria.");
        }

        // Split dei parametri: separatore ">"
        String[] parts = categoriaEAltro.trim().split(">", 2);
        String categoria = parts[0].trim();
        String titolo = (parts.length > 1) ? parts[1].trim() : "";

        try (Connection conn = dbManager.getConnection()) {
            StringBuilder sqlBuilder = new StringBuilder(
                    "SELECT \"LibroID\", \"Titolo\", \"Autori\", \"Categoria\", \"Prezzo\" " +
                            "FROM \"Libri\" WHERE \"Categoria\" ILIKE ?"
            );

            if (!titolo.isEmpty()) {
                sqlBuilder.append(" AND \"Titolo\" ILIKE ?");
            }

            sqlBuilder.append(" ORDER BY \"Titolo\" LIMIT 10");

            try (PreparedStatement stmt = conn.prepareStatement(sqlBuilder.toString())) {
                stmt.setString(1, "%" + categoria + "%");
                if (!titolo.isEmpty()) {
                    stmt.setString(2, "%" + titolo + "%");
                }

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
                        return ResponseFormatter.erroreJson(
                                "Nessun libro trovato per categoria: " + categoria +
                                        (titolo.isEmpty() ? "" : " e titolo: " + titolo)
                        );
                    }

                    return ResponseFormatter.successoJson(
                            "Trovati " + libri.size() + " libri per categoria: " + categoria +
                                    (titolo.isEmpty() ? "" : " e titolo: " + titolo),
                            ResponseFormatter.singletonMap("libri", libri)
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Errore nella ricerca per categoria e titolo: " + e.getMessage());
            return ResponseFormatter.erroreJson("Errore durante la ricerca. Riprova più tardi.");
        }
    }


    /**
     * Metodo helper per fare l'escape di caratteri speciali nel JSON
     * (se non esiste già nella tua classe)
     */
    private String escapeJson(String str) {
        if (str == null) return "";

        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Ottiene i dettagli di un libro specifico dal suo ID.
     *
     * @param libroIDStr L'ID del libro di cui ottenere i dettagli
     * @return Messaggio di successo o errore in formato JSON con i dettagli del libro
     */
    private String dettagliLibro(String libroIDStr) {
        if (libroIDStr.isEmpty()) {
            return ResponseFormatter.erroreJson("Specifica l'ID del libro di cui visualizzare i dettagli.");
        }
        try {
            int libroID = Integer.parseInt(libroIDStr);
            Optional<Libro> libroOpt = libroDAO.getLibroById(libroID);

            if (libroOpt.isEmpty()) {
                return ResponseFormatter.erroreJson("Libro con ID " + libroID + " non trovato.");
            }

            Libro libro = libroOpt.get();
            Map<String, Object> libroMap = new HashMap<>();
            libroMap.put("libroID", libro.libroId());
            libroMap.put("titolo", libro.titolo());
            libroMap.put("autori", libro.autori());
            libroMap.put("categoria", libro.categoria());
            libroMap.put("prezzo", libro.prezzo());
            libroMap.put("descrizione", libro.descrizione());
            libroMap.put("annoPubblicazione", libro.annoPubblicazione());
            libroMap.put("editore", libro.editore());
            libroMap.put("mesePubblicazione", libro.mesePubblicazione());

            return ResponseFormatter.successoJson("Dettagli libro ID: " + libroID, ResponseFormatter.singletonMap("libro", libroMap));
        } catch (NumberFormatException e) {
            return ResponseFormatter.erroreJson("ID libro non valido. Assicurati di inserire un numero intero.");
        } catch (SQLException e) {
            System.err.println("Errore durante il recupero dei dettagli del libro: " + e.getMessage());
            return ResponseFormatter.erroreJson("Errore durante il recupero dei dettagli del libro. Riprova più tardi.");
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
     * Ottiene tutte le categorie di libri disponibili nel database.
     * Gestisce categorie separate da virgola dividendole e rimuovendo spazi eccessivi.
     *
     * @return Messaggio di successo o errore in formato JSON con la lista delle categorie
     */
    private String getCategorie() {
        try {
            List<String> categorie = libroDAO.getAllCategorie();
            if (categorie.isEmpty()) {
                return ResponseFormatter.erroreJson("Nessuna categoria trovata nel database.");
            }
            return ResponseFormatter.successoJson("Lista di tutte le categorie disponibili",
                    ResponseFormatter.singletonMap("categorie", categorie));
        } catch (SQLException e) {
            System.err.println("Errore durante il recupero delle categorie: " + e.getMessage());
            return ResponseFormatter.erroreJson("Errore durante il recupero delle categorie. Riprova più tardi.");
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
        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> dati;

        try {
            dati = mapper.readValue(parametri, new TypeReference<Map<String, String>>() {});
        } catch (IOException e) {
            return ResponseFormatter.erroreJson("Formato JSON non valido.");
        }

        String nomeCompleto = dati.get("nomeCompleto");
        String email = dati.get("email");
        String username = dati.get("username");
        String password = dati.get("password");
        String codiceFiscale = dati.get("codiceFiscale"); // può essere null

        // Verifica campi obbligatori
        if (nomeCompleto == null || email == null || username == null || password == null) {
            return ResponseFormatter.erroreJson("Parametri mancanti. Richiesti: nomeCompleto, email, username, password.");
        }

        try {
            int userID = utenteDAO.registraUtente(nomeCompleto, codiceFiscale, email, username, password);
            return ResponseFormatter.successoJson(
                    "Registrazione completata con successo! Ora puoi effettuare il login.",
                    ResponseFormatter.singletonMap("userID", userID)
            );
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
        comandiGenerali.add(createCommandInfo("DETTAGLI_LIBRO", "Ottieni i dettagli completi di un libro specifico", "<libroID>"));
        comandiGenerali.add(createCommandInfo("CATEGORIE", "Ottieni la lista completa di tutte le categorie di libri disponibili", ""));
        comandiGenerali.add(createCommandInfo("CERCA_PER_CATEGORIA", "Cerca libri con una specifica categoria", "<categoria>"));
        comandiGenerali.add(createCommandInfo("CERCA_PER_AUTORE", "Cerca libri di un autore specifico", "<autore>"));
        comandiGenerali.add(createCommandInfo("CERCA_PER_ANNO", "Cerca libri pubblicati in un anno specifico", "<anno>"));
        comandiGenerali.add(createCommandInfo("CERCA_PER_AUTORE_E_ANNO", "Cerca libri di un autore specifico pubblicati in un anno specifico", "<autore> <anno>"));
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
            comandiLibrerie.add(createCommandInfo("SPOSTA_LIBRO", "Sposta un libro da una libreria a un'altra", "<libreriaOrigineID> <libreriaDestinazioneID> <libroID>"));
            comandiLibrerie.add(createCommandInfo("VISUALIZZA_LIBRERIA", "Visualizza i libri in una libreria", "<libreriaID>"));
            comandiLibrerie.add(createCommandInfo("ELIMINA_LIBRERIA", "Elimina una libreria personale", "<libreriaID>"));
            comandiLibrerie.add(createCommandInfo("RINOMINA_LIBRERIA", "Rinomina una libreria personale", "<libreriaID> <nuovoNome>"));
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
     * @param nome        Il nome del comando
     * @param descrizione La descrizione del comando
     * @param parametri   I parametri richiesti dal comando
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
     */
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
     * Sposta un libro da una libreria all'altra per l'utente autenticato.
     *
     * @param parametri I parametri per lo spostamento (libreriaOrigineID libreriaDestinazioneID libroID)
     * @return Messaggio di successo o errore in formato JSON
     */
    private String spostaLibro(String parametri) {
        String[] parti = parametri.split("\\s+", 3);
        if (parti.length < 3) {
            return ResponseFormatter.erroreJson("Formato non valido. Usa: SPOSTA_LIBRO libreriaOrigineID libreriaDestinazioneID libroID");
        }
        try {
            int libreriaOrigineID = Integer.parseInt(parti[0]);
            int libreriaDestinazioneID = Integer.parseInt(parti[1]);
            int libroID = Integer.parseInt(parti[2]);

            // Verifica che entrambe le librerie esistano e appartengano all'utente
            Optional<Libreria> libreriaOrigineOpt = libreriaDAO.getLibreriaById(libreriaOrigineID);
            Optional<Libreria> libreriaDestinazioneOpt = libreriaDAO.getLibreriaById(libreriaDestinazioneID);

            if (libreriaOrigineOpt.isEmpty() || libreriaOrigineOpt.get().userID() != utenteAutenticato.userID()) {
                return ResponseFormatter.erroreJson("Libreria di origine non trovata o non hai i permessi per modificarla.");
            }

            if (libreriaDestinazioneOpt.isEmpty() || libreriaDestinazioneOpt.get().userID() != utenteAutenticato.userID()) {
                return ResponseFormatter.erroreJson("Libreria di destinazione non trovata o non hai i permessi per modificarla.");
            }

            if (libreriaOrigineID == libreriaDestinazioneID) {
                return ResponseFormatter.erroreJson("Le librerie di origine e destinazione devono essere diverse.");
            }

            // Rimuovi il libro dalla libreria di origine
            libreriaDAO.rimuoviLibroDaLibreria(libreriaOrigineID, libroID);

            // Aggiungi il libro alla libreria di destinazione
            libreriaDAO.aggiungiLibroALibreria(libreriaDestinazioneID, libroID);

            return ResponseFormatter.successoJson("Libro spostato con successo dalla libreria '" +
                    libreriaOrigineOpt.get().nomeLibreria() + "' alla libreria '" +
                    libreriaDestinazioneOpt.get().nomeLibreria() + "'.");
        } catch (NumberFormatException e) {
            return ResponseFormatter.erroreJson("ID non validi. Assicurati di inserire numeri interi.");
        } catch (SQLException e) {
            System.err.println("Errore durante lo spostamento del libro: " + e.getMessage());
            return ResponseFormatter.erroreJson("Errore durante lo spostamento del libro. Il libro potrebbe non essere presente nella libreria di origine o già presente in quella di destinazione.");
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

                // Gestione sicura dei valori null per le note - conversione a stringa vuota
                noteStile = String.valueOf(paramsMap.getOrDefault("noteStile", ""));

                scoreContenuto = paramsMap.containsKey("scoreContenuto") ?
                        ((Number) paramsMap.get("scoreContenuto")).shortValue() : 0;
                noteContenuto = String.valueOf(paramsMap.getOrDefault("noteContenuto", ""));

                scoreGradevolezza = paramsMap.containsKey("scoreGradevolezza") ?
                        ((Number) paramsMap.get("scoreGradevolezza")).shortValue() : 0;
                noteGradevolezza = String.valueOf(paramsMap.getOrDefault("noteGradevolezza", ""));

                scoreOriginalita = paramsMap.containsKey("scoreOriginalita") ?
                        ((Number) paramsMap.get("scoreOriginalita")).shortValue() : 0;
                noteOriginalita = String.valueOf(paramsMap.getOrDefault("noteOriginalita", ""));

                scoreEdizione = paramsMap.containsKey("scoreEdizione") ?
                        ((Number) paramsMap.get("scoreEdizione")).shortValue() : 0;
                noteEdizione = String.valueOf(paramsMap.getOrDefault("noteEdizione", ""));

            } else {
                String[] parti = parametri.split("\\s+");
                if (parti.length < 11) {
                    return ResponseFormatter.erroreJson("Formato non valido o parametri insufficienti. Devono esserci 11 parametri (libroID, 5 coppie score/nota). Le note non devono contenere spazi.");
                }
                libroID = Integer.parseInt(parti[0]);
                scoreStile = Short.parseShort(parti[1]);
                noteStile = parti[2].isEmpty() ? "-" : parti[2];
                scoreContenuto = Short.parseShort(parti[3]);
                noteContenuto = parti[4].isEmpty() ? "-" : parti[4];
                scoreGradevolezza = Short.parseShort(parti[5]);
                noteGradevolezza = parti[6].isEmpty() ? "-" : parti[6];
                scoreOriginalita = Short.parseShort(parti[7]);
                noteOriginalita = parti[8].isEmpty() ? "-" : parti[8];
                scoreEdizione = Short.parseShort(parti[9]);
                noteEdizione = parti[10].isEmpty() ? "-" : parti[10];
            }

            // Validazione dei punteggi (permettendo 0 per indicare "non valutato")
            if ((scoreStile != 0 && (scoreStile < 1 || scoreStile > 5)) ||
                    (scoreContenuto != 0 && (scoreContenuto < 1 || scoreContenuto > 5)) ||
                    (scoreGradevolezza != 0 && (scoreGradevolezza < 1 || scoreGradevolezza > 5)) ||
                    (scoreOriginalita != 0 && (scoreOriginalita < 1 || scoreOriginalita > 5)) ||
                    (scoreEdizione != 0 && (scoreEdizione < 1 || scoreEdizione > 5))) {
                return ResponseFormatter.erroreJson("I punteggi devono essere compresi tra 1 e 5 (o 0 per non valutato).");
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
                valMap.put("libroID", val.libroID()); // AGGIUNTO per compatibilità client
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
                valMap.put("userID", val.userID());
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
                libroMap.put("descrizione", libro.descrizione());
                libroMap.put("editore", libro.editore());
                libroMap.put("mesePubblicazione", libro.mesePubblicazione());
                libroMap.put("annoPubblicazione", libro.annoPubblicazione());
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
            // Verifico se il libro ha già più di 3 consigli dallo stesso utente
            // Prendo i miei consigli e conto
            List<Consiglio> mieiConsigli = consiglioDAO.getConsigliUtente(utenteAutenticato.userID());
            long conteggioConsigli = mieiConsigli.stream()
                    .filter(c -> c.libroRiferimentoID() == libroRiferimentoID)
                    .count();
            if (conteggioConsigli >= 3) {
                return ResponseFormatter.erroreJson("Hai raggiunto il numero massimo di consigli per questo libro.");
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
                consiglioMap.put("userID", consiglio.userID());
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

    /**
     * Rinomina una libreria esistente.
     *
     * @param parametri Parametri per il comando di rinomina nel formato "libreriaID nuovoNome" o JSON
     * @return Messaggio di successo o errore in formato JSON
     */
    private String rinominaLibreria(String parametri) {
        if (parametri == null || parametri.trim().isEmpty()) {
            return ResponseFormatter.erroreJson("Specifica l'ID della libreria e il nuovo nome.");
        }

        // Verifica se i parametri sono in formato JSON
        Map<String, Object> paramsMap = null;
        if (parametri.trim().startsWith("{") && parametri.trim().endsWith("}")) {
            try {
                paramsMap = new ObjectMapper().readValue(parametri, HashMap.class);
            } catch (JsonProcessingException e) {
                System.err.println("Parametri per RINOMINA_LIBRERIA sembravano JSON ma non parsabili: " + e.getMessage());
            }
        }

        try {
            int libreriaID;
            String nuovoNome;

            if (paramsMap != null) {
                // Parametri in formato JSON
                if (!paramsMap.containsKey("libreriaID") || !paramsMap.containsKey("nuovoNome")) {
                    return ResponseFormatter.erroreJson("Parametri JSON incompleti per RINOMINA_LIBRERIA. Servono libreriaID e nuovoNome.");
                }
                libreriaID = ((Number) paramsMap.get("libreriaID")).intValue();
                nuovoNome = (String) paramsMap.get("nuovoNome");
            } else {
                // Parametri in formato stringa: "libreriaID nuovoNome"
                String[] parti = parametri.trim().split("\\s+", 2);
                if (parti.length < 2) {
                    return ResponseFormatter.erroreJson("Formato parametri non valido. Usa: <libreriaID> <nuovoNome>");
                }
                libreriaID = Integer.parseInt(parti[0]);
                nuovoNome = parti[1];
            }

            if (nuovoNome.trim().isEmpty()) {
                return ResponseFormatter.erroreJson("Il nuovo nome della libreria non può essere vuoto.");
            }

            // Verifica che la libreria esista e appartenga all'utente
            Optional<Libreria> libreriaOpt = libreriaDAO.getLibreriaById(libreriaID);
            if (libreriaOpt.isEmpty() || libreriaOpt.get().userID() != utenteAutenticato.userID()) {
                return ResponseFormatter.erroreJson("Libreria non trovata o non hai i permessi per rinominarla.");
            }

            String vecchioNome = libreriaOpt.get().nomeLibreria();
            boolean successo = libreriaDAO.rinominaLibreria(libreriaID, nuovoNome.trim());

            if (successo) {
                Map<String, Object> data = new HashMap<>();
                data.put("libreriaID", libreriaID);
                data.put("vecchioNome", vecchioNome);
                data.put("nuovoNome", nuovoNome.trim());
                return ResponseFormatter.successoJson("Libreria rinominata da '" + vecchioNome + "' a '" + nuovoNome.trim() + "'.", data);
            } else {
                return ResponseFormatter.erroreJson("Errore durante la rinomina della libreria.");
            }

        } catch (NumberFormatException e) {
            return ResponseFormatter.erroreJson("ID libreria non valido. Assicurati di inserire un numero intero.");
        } catch (SQLException e) {
            System.err.println("Errore durante la rinomina della libreria: " + e.getMessage());
            return ResponseFormatter.erroreJson("Errore durante la rinomina della libreria. Riprova più tardi.");
        }
    }
}