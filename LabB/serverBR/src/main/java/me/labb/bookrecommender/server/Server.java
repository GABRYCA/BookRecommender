package me.labb.bookrecommender.server;

import me.labb.bookrecommender.server.db.DatabaseManager;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Classe Server che gestisce le connessioni client e le richieste per il sistema di raccomandazione libri.
 * Implementa il pattern Runnable per permettere l'esecuzione in thread separati.
 * 
 * @author Caretti Gabriele 756564 VA
 * @author Como Riccardo 758697 VA
 * @author Manicone Giorgia 758716 VA
 */
public class Server implements Runnable {
    private static final int PORTA_DEFAULT = 8080;
    private final int porta;
    private ServerSocket serverSocket;
    private boolean attivo = false;
    private final ExecutorService threadPool;    private final DatabaseManager dbManager;

    /**
     * Esegue il server nel thread corrente.
     * Chiama il metodo avvia() per iniziare l'ascolto delle connessioni.
     */
    @Override
    public void run() {
        this.avvia();
    }

    /**
     * Costruttore server con porta predefinita (o da config.properties).
     * Legge la porta dal file config.properties, se disponibile.
     */
    public Server() {
        int portaConfig = PORTA_DEFAULT;

        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input != null) {
                Properties props = new Properties();
                props.load(input);

                String portaStr = props.getProperty("server.port");
                if (portaStr != null && !portaStr.isEmpty()) {
                    try {
                        portaConfig = Integer.parseInt(portaStr);
                        System.out.println("Porta letta da config.properties: " + portaConfig);
                    } catch (NumberFormatException e) {
                        System.err.println("Errore nel parsing della porta da config.properties. Usando porta di default: " + PORTA_DEFAULT);
                    }
                }
            } else {
                System.out.println("File config.properties non trovato. Usando porta di default: " + PORTA_DEFAULT);
            }
        } catch (IOException e) {
            System.err.println("Errore nella lettura del file config.properties: " + e.getMessage());
            System.out.println("Usando porta di default: " + PORTA_DEFAULT);
        }

        this.porta = portaConfig;
        this.threadPool = Executors.newCachedThreadPool();
        this.dbManager = DatabaseManager.getInstance();
    }

    /**
     * Costruttore server con porta specifica.
     * 
     * @param porta La porta su cui il server ascolterà le connessioni
     */
    public Server(int porta) {
        this.porta = porta;
        this.threadPool = Executors.newCachedThreadPool();
        this.dbManager = DatabaseManager.getInstance();
    }

    /**
     * Avvia il server.
     */
    public void avvia() {
        if (attivo) {
            System.out.println("Il server è già in esecuzione sulla porta " + porta);
            return;
        }

        try {
            serverSocket = new ServerSocket(porta);
            attivo = true;
            System.out.println("Server avviato sulla porta " + porta);

            // Ciclo principale del server
            while (attivo) {
                try {
                    System.out.println("In attesa di connessioni client...");
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Nuovo client connesso: " + clientSocket.getInetAddress().getHostAddress());

                    // Gestione client in un thread separato
                    threadPool.execute(new ClientHandler(clientSocket));

                } catch (IOException e) {
                    if (attivo) {
                        System.err.println("Errore nell'accettare la connessione client: " + e.getMessage());
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("Errore nell'avvio del server sulla porta " + porta + ": " + e.getMessage());
        } finally {
            arresta();
        }
    }

    /**
     * Arresta il server.
     */
    public void arresta() {
        if (!attivo) {
            return;
        }

        attivo = false;
        threadPool.shutdown();

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                System.out.println("Server arrestato.");
            }
        } catch (IOException e) {
            System.err.println("Errore durante la chiusura del server: " + e.getMessage());
        }
    }

    /**
     * Restituisce lo stato attuale del server.
     * 
     * @return true se il server è attivo, false altrimenti
     */
    public boolean isAttivo() {
        return attivo;
    }
}
