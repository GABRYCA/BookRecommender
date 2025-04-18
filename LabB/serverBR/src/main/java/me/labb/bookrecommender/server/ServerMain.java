package me.labb.bookrecommender.server;

import me.labb.bookrecommender.server.db.DatabaseManager;

import java.sql.Connection;
import java.sql.SQLException;

public class ServerMain {

    /**
     * NB: è possibile chiamare da qualsiasi classe del codice:
     * DatabaseManager.getInstance() e ritornerà un oggetto DatabaseManager valido.
     * Non serve passare dbm come parametro nei metodi dal main!
     * */
    public static DatabaseManager dbm;

    public static void main(String[] args) {
        System.out.println("Avvio server...");

        dbm = DatabaseManager.getInstance();

        try {
            Connection c = dbm.getConnection();
            c.getSchema(); // Leggo lo schema così verifico subito se ho accesso a esso.
            c.close();
        } catch (SQLException e) {
            System.err.println("Errore di connessione al database: " + e.getMessage());
            return;
        }

        // Creo e inizializzo il server
        Server server = new Server();

        
        // Avvio il server in un thread separato
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
