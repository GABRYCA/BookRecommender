package me.labb.bookrecommender.server;

import me.labb.bookrecommender.server.db.DatabaseManager;
import me.labb.bookrecommender.server.db.LibroDAO;
import me.labb.bookrecommender.server.db.UtenteDAO;
import me.labb.bookrecommender.server.oggetti.Libro;
import me.labb.bookrecommender.server.oggetti.Utente;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
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
     * 
     * @param comando Il comando da elaborare
     * @return La risposta da inviare al client
     */
    private String elaboraComando(String comando) {
        // Dividi comando in parti (comando + parametri)
        String[] parti = comando.split("\\s+", 2);
        String azione = parti[0].toUpperCase();
        String parametri = parti.length > 1 ? parti[1] : "";

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
        }

        // Comandi utenti autenticati.
        if (isAutenticato()) {
            switch (azione) {
                case "LOGOUT":
                    return logout();
                case "PROFILO":
                    return visualizzaProfilo();
            }
        } else if (azione.equals("LOGOUT") || azione.equals("PROFILO")) {
            return "Devi effettuare il login per utilizzare questo comando.";
        }

        return "Comando non riconosciuto. Digita HELP per la lista dei comandi.";
    }

    /**
     * Gestisce il logout di un utente.
     * 
     * @return Messaggio di risposta
     */
    private String logout() {
        String nomeUtente = utenteAutenticato.nomeCompleto();
        utenteAutenticato = null;
        return "Logout effettuato con successo. Arrivederci, " + nomeUtente + "!";
    }

    /**
     * Visualizza informazioni profile utente.
     * 
     * @return Informazioni del profilo
     */
    private String visualizzaProfilo() {
        StringBuilder profilo = new StringBuilder();
        profilo.append("Informazioni profilo:\n");
        profilo.append("Nome: ").append(utenteAutenticato.nomeCompleto()).append("\n");
        profilo.append("Username: ").append(utenteAutenticato.username()).append("\n");
        profilo.append("Email: ").append(utenteAutenticato.email()).append("\n");

        if (utenteAutenticato.codiceFiscale() != null && !utenteAutenticato.codiceFiscale().isEmpty()) {
            profilo.append("Codice Fiscale: ").append(utenteAutenticato.codiceFiscale()).append("\n");
        }

        profilo.append("Data registrazione: ").append(utenteAutenticato.dataRegistrazione()).append("\n");

        return profilo.toString();
    }

    /**
     * Cerca libri nel database in base a dei parametri.
     * 
     * @param parametri Criteri di ricerca
     * @return Risultati della ricerca
     */
    private String cercaLibri(String parametri) {
        if (parametri.isEmpty()) {
            return "Specifica un termine di ricerca.";
        }

        StringBuilder risultati = new StringBuilder();
        try (Connection conn = dbManager.getConnection()) {
            // Ricerca con Titolo e Descrizione (simili).
            String sql = """
                    SELECT "Titolo", "Autori", "Categoria", "Prezzo" 
                    FROM "Libri" 
                    WHERE "Titolo" ILIKE ? OR "Descrizione" ILIKE ? 
                    LIMIT 10
                    """;

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                String termine = "%" + parametri + "%";
                stmt.setString(1, termine);
                stmt.setString(2, termine);

                try (ResultSet rs = stmt.executeQuery()) {
                    boolean trovato = false;

                    while (rs.next()) {
                        trovato = true;
                        risultati.append("Titolo: ").append(rs.getString("Titolo")).append("\n");
                        risultati.append("Autori: ").append(rs.getString("Autori")).append("\n");
                        risultati.append("Categoria: ").append(rs.getString("Categoria")).append("\n");
                        risultati.append("Prezzo: $").append(rs.getFloat("Prezzo")).append("\n");
                        risultati.append("-------------------------\n");
                    }

                    if (!trovato) {
                        return "Nessun libro trovato per: " + parametri;
                    }
                }
            }

            return risultati.toString();

        } catch (SQLException e) {
            System.err.println("Errore durante la ricerca dei libri: " + e.getMessage());
            return "Errore durante la ricerca. Riprova più tardi.";
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
            return "Specifica una categoria per le raccomandazioni.";
        }

        StringBuilder consigli = new StringBuilder();
        try (Connection conn = dbManager.getConnection()) {
            // Ricerca libri nella stessa categoria
            String sql = """
                    SELECT "Titolo", "Autori", "Prezzo" 
                    FROM "Libri" 
                    WHERE "Categoria" ILIKE ? 
                    ORDER BY RANDOM() 
                    LIMIT 5
                    """;

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                String termine = "%" + categoria + "%";
                stmt.setString(1, termine);

                try (ResultSet rs = stmt.executeQuery()) {
                    boolean trovato = false;

                    consigli.append("Libri consigliati nella categoria '").append(categoria).append("':\n\n");

                    while (rs.next()) {
                        trovato = true;
                        consigli.append("Titolo: ").append(rs.getString("Titolo")).append("\n");
                        consigli.append("Autori: ").append(rs.getString("Autori")).append("\n");
                        consigli.append("Prezzo: $").append(rs.getFloat("Prezzo")).append("\n");
                        consigli.append("-------------------------\n");
                    }

                    if (!trovato) {
                        return "Nessun libro trovato nella categoria: " + categoria;
                    }
                }
            }

            return consigli.toString();

        } catch (SQLException e) {
            System.err.println("Errore durante la ricerca di consigli: " + e.getMessage());
            return "Errore durante la ricerca di consigli. Riprova più tardi.";
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
            return "Formato non valido. Usa: LOGIN username password";
        }

        String usernameOrEmail = parti[0];
        String password = parti[1];

        try {
            Optional<Utente> utenteOpt = utenteDAO.login(usernameOrEmail, password);

            if (utenteOpt.isPresent()) {
                this.utenteAutenticato = utenteOpt.get();
                return "Login effettuato con successo. Benvenuto, " + utenteAutenticato.nomeCompleto() + "!";
            } else {
                return "Credenziali non valide. Riprova.";
            }
        } catch (SQLException e) {
            System.err.println("Errore durante il login: " + e.getMessage());
            return "Errore durante il login. Riprova più tardi.";
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
            return "Formato non valido. Usa: REGISTRA nomeCompleto email username password [codiceFiscale]";
        }

        String nomeCompleto = parti[0];
        String email = parti[1];
        String username = parti[2];
        String password = parti[3];
        String codiceFiscale = parti.length > 4 ? parti[4] : null;

        try {
            int userID = utenteDAO.registraUtente(nomeCompleto, codiceFiscale, email, username, password);
            return "Registrazione completata con successo! Ora puoi effettuare il login.";
        } catch (SQLException e) {
            System.err.println("Errore durante la registrazione: " + e.getMessage());
            return "Errore durante la registrazione. L'username o l'email potrebbero essere già in uso.";
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
        StringBuilder comandi = new StringBuilder("Comandi disponibili:\n");

        // Comandi base (disponibili a tutti)
        comandi.append("CERCA <termine> - Cerca libri con il termine indicato nel titolo o nella descrizione\n");
        comandi.append("CONSIGLIA <categoria> - Ottieni consigli di libri in una determinata categoria\n");

        // Comandi di autenticazione
        if (!isAutenticato()) {
            comandi.append("LOGIN <username> <password> - Accedi al tuo account\n");
            comandi.append("REGISTRA <nomeCompleto> <email> <username> <password> [codiceFiscale] - Crea un nuovo account\n");
        } else { // Comandi per soli utenti autenticati
            comandi.append("LOGOUT - Esci dal tuo account\n");
            comandi.append("PROFILO - Visualizza i dettagli del tuo profilo\n");
        }

        comandi.append("HELP - Mostra questa lista di comandi\n");
        comandi.append("EXIT - Chiudi la connessione");

        return comandi.toString();
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
}
