package me.labb.bookrecommender.server.db;

import me.labb.bookrecommender.server.oggetti.Utente;
import me.labb.bookrecommender.server.sicurezza.PasswordManager;

import java.sql.*;
import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * Classe DAO per gestire le operazioni CRUD sugli utenti.
 */
public class UtenteDAO {
    private final DatabaseManager dbManager;

    public UtenteDAO() {
        this.dbManager = DatabaseManager.getInstance();
    }

    /**
     * Registra un nuovo utente nel sistema.
     *
     * @param nomeCompleto Nome completo dell'utente
     * @param codiceFiscale Codice fiscale dell'utente (può essere null)
     * @param email Email dell'utente
     * @param username Username scelto dall'utente
     * @param password Password in chiaro (verrà hashata prima del salvataggio)
     * @return ID dell'utente appena creato
     * @throws SQLException In caso di errori SQL
     */
    public int registraUtente(String nomeCompleto, String codiceFiscale,
                              String email, String username, String password) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = dbManager.getConnection();
            String sql = """
                    INSERT INTO "UtentiRegistrati" ("NomeCompleto", "CodiceFiscale", "Email", "Username", "PasswordHash")
                    VALUES (?, ?, ?, ?, ?)
                    RETURNING "UserID"
                    """;

            String passwordHash = PasswordManager.hashPassword(password);

            stmt = conn.prepareStatement(sql);
            stmt.setString(1, nomeCompleto);
            stmt.setString(2, codiceFiscale);
            stmt.setString(3, email);
            stmt.setString(4, username);
            stmt.setString(5, passwordHash);

            rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            } else {
                throw new SQLException("Errore nella registrazione utente, nessun ID ritornato");
            }
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        }
    }

    /**
     * Verifica le credenziali di accesso di un utente.
     *
     * @param usernameOrEmail Username o email dell'utente
     * @param password Password in chiaro
     * @return Optional contenente l'utente se l'autenticazione è riuscita, altrimenti vuoto
     * @throws SQLException In caso di errori SQL
     */
    public Optional<Utente> login(String usernameOrEmail, String password) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = dbManager.getConnection();
            String sql = """
                    SELECT * FROM "UtentiRegistrati"
                    WHERE "Username" = ? OR "Email" = ?
                    """;

            stmt = conn.prepareStatement(sql);
            stmt.setString(1, usernameOrEmail);
            stmt.setString(2, usernameOrEmail);

            rs = stmt.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("PasswordHash");

                if (PasswordManager.verificaPassword(password, storedHash)) {
                    Utente utente = new Utente(
                            rs.getInt("UserID"),
                            rs.getString("NomeCompleto"),
                            rs.getString("CodiceFiscale"),
                            rs.getString("Email"),
                            rs.getString("Username"),
                            rs.getString("PasswordHash"),
                            rs.getObject("DataRegistrazione", ZonedDateTime.class)
                    );
                    return Optional.of(utente);
                }
            }

            return Optional.empty();
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        }
    }

    /**
     * Ottiene un utente tramite ID.
     *
     * @param userID ID dell'utente da trovare
     * @return Optional contenente l'utente se trovato, altrimenti vuoto
     * @throws SQLException In caso di errori SQL
     */
    public Optional<Utente> getUtenteById(int userID) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = dbManager.getConnection();
            String sql = """
                    SELECT * FROM "UtentiRegistrati"
                    WHERE "UserID" = ?
                    """;

            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userID);

            rs = stmt.executeQuery();

            if (rs.next()) {
                Utente utente = new Utente(
                        rs.getInt("UserID"),
                        rs.getString("NomeCompleto"),
                        rs.getString("CodiceFiscale"),
                        rs.getString("Email"),
                        rs.getString("Username"),
                        rs.getString("PasswordHash"),
                        rs.getObject("DataRegistrazione", ZonedDateTime.class)
                );
                return Optional.of(utente);
            }

            return Optional.empty();
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        }
    }
}