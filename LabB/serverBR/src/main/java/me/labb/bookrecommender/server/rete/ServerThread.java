package me.labb.bookrecommender.server.rete;

import me.labb.bookrecommender.server.db.LibroDAO;
import me.labb.bookrecommender.server.db.UtenteDAO;
import me.labb.bookrecommender.server.oggetti.Libro;
import me.labb.bookrecommender.server.oggetti.Utente;

import java.io.*;
import java.net.Socket;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Thread per gestire una connessione ad un client.
 *
 * @author Caretti Gabriele 756564 VA
 * @author Como Riccardo 758697 VA
 * @author Manicone Giorgia 758716 VA
 */
public class ServerThread extends Thread {
    private final Socket socket;
    private final int clientID;
    private Utente utenteLoggato = null;

    private ObjectInputStream in;
    private ObjectOutputStream out;

    private final UtenteDAO utenteDAO;
    private final LibroDAO libroDAO;

    /**
     * Costruttore ServerThread
     *
     * @param socket socket del server
     * @param clientID ID del client
     * */
    public ServerThread(Socket socket, int clientID) {
        this.socket = socket;
        this.clientID = clientID;
        this.utenteDAO = new UtenteDAO();
        this.libroDAO = new LibroDAO();
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            System.out.println("Client #" + clientID + " connesso da " + socket.getInetAddress().getHostAddress());

            boolean running = true;
            while (running) {
                String comando = (String) in.readObject();
                System.out.println("Client #" + clientID + " ha inviato il comando: " + comando);

                switch (comando) {
                    case "LOGIN" -> gestisciLogin();
                    case "REGISTRA" -> gestisciRegistrazione();
                    case "CERCA_LIBRI" -> gestisciCercaLibri();
                    case "CATEGORIE" -> gestisciGetCategorie();
                    case "LIBRI_PER_CATEGORIA" -> gestisciLibriPerCategoria();
                    case "LOGOUT" -> gestisciLogout();
                    case "DISCONNETTI" -> running = false;
                    default -> inviaRisposta("ERRORE", "Comando non trovato: " + comando);
                }
            }

        } catch (EOFException e) {
            System.out.println("Client #" + clientID + " si è disconnesso");
        } catch (IOException | ClassNotFoundException | SQLException e) {
            System.err.println("Errore con il client #" + clientID + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            chiudiConnessione();
        }
    }

    private void gestisciLogin() throws IOException, ClassNotFoundException, SQLException {
        String usernameOrEmail = (String) in.readObject();
        String password = (String) in.readObject();

        Optional<Utente> risultato = utenteDAO.login(usernameOrEmail, password);

        if (risultato.isPresent()) {
            this.utenteLoggato = risultato.get();
            inviaRisposta("LOGIN_OK", "Login effettuato con successo");
        } else {
            inviaRisposta("LOGIN_FALLITO", "Credenziali non valide");
        }
    }

    private void gestisciRegistrazione() throws IOException, ClassNotFoundException, SQLException {
        String nomeCompleto = (String) in.readObject();
        String codiceFiscale = (String) in.readObject();
        String email = (String) in.readObject();
        String username = (String) in.readObject();
        String password = (String) in.readObject();

        try {
            int userID = utenteDAO.registraUtente(nomeCompleto, codiceFiscale, email, username, password);
            inviaRisposta("REGISTRAZIONE_OK", "Registrazione completata con successo");
        } catch (SQLException e) {
            if (e.getMessage().contains("unique constraint") || e.getMessage().contains("duplicate key")) {
                if (e.getMessage().contains("Email")) {
                    inviaRisposta("REGISTRAZIONE_FALLITA", "Email già registrata");
                } else if (e.getMessage().contains("Username")) {
                    inviaRisposta("REGISTRAZIONE_FALLITA", "Username già in uso");
                } else if (e.getMessage().contains("CodiceFiscale")) {
                    inviaRisposta("REGISTRAZIONE_FALLITA", "Codice fiscale già registrato");
                } else {
                    inviaRisposta("REGISTRAZIONE_FALLITA", "Dati già presenti nel sistema");
                }
            } else {
                inviaRisposta("REGISTRAZIONE_FALLITA", "Errore durante la registrazione: " + e.getMessage());
            }
        }
    }

    private void gestisciCercaLibri() throws IOException, ClassNotFoundException, SQLException {
        String query = (String) in.readObject();
        int limit = (int) in.readObject();

        List<Libro> risultati = libroDAO.cercaLibri(query, limit);
        inviaRisposta("RISULTATI_RICERCA", risultati);
    }

    private void gestisciGetCategorie() throws IOException, SQLException {
        List<String> categorie = libroDAO.getAllCategorie();
        inviaRisposta("CATEGORIE", categorie);
    }

    private void gestisciLibriPerCategoria() throws IOException, ClassNotFoundException, SQLException {
        String categoria = (String) in.readObject();
        int limit = (int) in.readObject();

        List<Libro> risultati = libroDAO.getLibriByCategoria(categoria, limit);
        inviaRisposta("LIBRI_CATEGORIA", risultati);
    }

    private void gestisciLogout() throws IOException {
        this.utenteLoggato = null;
        inviaRisposta("LOGOUT_OK", "Logout effettuato con successo");
    }

    private void inviaRisposta(String tipo, Object contenuto) throws IOException {
        out.writeObject(tipo);
        out.writeObject(contenuto);
        out.flush();
    }

    private void chiudiConnessione() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
            System.out.println("Connessione con client #" + clientID + " chiusa");
        } catch (IOException e) {
            System.err.println("Errore durante la chiusura della connessione con client #" + clientID);
        }
    }
}
