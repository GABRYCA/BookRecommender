package me.labb.bookrecommender.server;

import me.labb.bookrecommender.server.db.DatabaseManager;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Classe principale per l'avvio del server BookRecommender.
 *
 * @author Caretti Gabriele 756564 VA
 * @author Como Riccardo 758697 VA
 * @author Manicone Giorgia 758716 VA
 */
public class ServerMain {
    public static DatabaseManager dbm;

    /**
     * Metodo principale per l'avvio del server.
     * Inizializza la connessione al database e avvia il server in un thread separato.
     *
     * @param args Argomenti da linea di comando (non utilizzati)
     */
    public static void main(String[] args) {
        System.out.println("Avvio server...");

        dbm = DatabaseManager.getInstance();

        try {
            Connection c = dbm.getConnection();
            c.getSchema();
            c.close();
        } catch (SQLException e) {
            System.err.println("Errore di connessione al database: " + e.getMessage());
            return;
        }        // Creo e inizializzo il server
        Server server = new Server();

        Thread serverThread = new Thread(server);
        serverThread.start();
        System.out.println("Server avviato con successo. Premi CTRL+C per terminare.");

        // Arresto del server
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Arresto del server in corso...");
            server.arresta();
            System.out.println("Server arrestato con successo (ServerMain).");
        }));

        // Attendo il termine del thread del server
        try {
            serverThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

    }
}
