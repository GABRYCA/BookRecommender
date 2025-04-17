package me.labb.bookrecommender.server.db;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseManager {

    private static volatile DatabaseManager instance;

    private final Properties dbProperties;
    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;

    /**
     * Costruttore privato, usare getInstance() per inizializzare.
     * NB: Singleton
     */
    private DatabaseManager() {
        dbProperties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                System.err.println("ERRORE: Impossibile trovare il file config.properties nel classpath.");
                throw new RuntimeException("File config.properties non trovato.");
            }

            dbProperties.load(input);

            this.dbUrl = dbProperties.getProperty("db.url");
            this.dbUser = dbProperties.getProperty("db.username");
            this.dbPassword = dbProperties.getProperty("db.password");

            if (this.dbUrl == null || this.dbUser == null || this.dbPassword == null) {
                throw new RuntimeException("Proprietà del database mancanti in config.properties (db.url, db.username, db.password)");
            }

        } catch (IOException e) {
            System.err.println("Errore durante la lettura del file config.properties.");
            throw new RuntimeException("Errore I/O durante la lettura di config.properties", e);
        } catch (Exception e) {
            System.err.println("Errore imprevisto durante l'inizializzazione di DatabaseManager.");
            throw new RuntimeException("Errore inizializzazione DatabaseManager", e);
        }
    }

    /**
     * Restituisce l'unica istanza di DatabaseManager (creandola se non esiste ancora).
     * NB: Singleton
     *
     * @return L'istanza Singleton di DatabaseManager.
     */
    public static DatabaseManager getInstance() {
        if (instance == null) {
            synchronized (DatabaseManager.class) {
                if (instance == null) {
                    instance = new DatabaseManager();
                }
            }
        }
        return instance;
    }

    /**
     * Restituisce una nuova connessione al database utilizzando le credenziali
     * caricate durante l'inizializzazione dell'istanza.
     *
     * @return Una connessione SQL al database.
     * @throws SQLException Se si verifica un errore durante la connessione al database.
     */
    public Connection getConnection() throws SQLException {
        System.out.println("Tentativo connessione a: " + this.dbUrl + " con utente: " + this.dbUser);
        Connection conn = DriverManager.getConnection(this.dbUrl, this.dbUser, this.dbPassword);
        System.out.println("Connessione al database stabilita.");
        return conn;
    }

    /**
     * Restituisce una copia delle proprietà del database caricate.
     *
     * @return Un oggetto Properties contenente la configurazione.
     */
    public Properties getDbProperties() {
        return (Properties) dbProperties.clone();
    }

    public String getDbUrl() {
        return dbUrl;
    }

    public String getDbUsername() {
        return dbUser;
    }
}