package me.labb.bookrecommender.server.db;

import me.labb.bookrecommender.server.oggetti.Libreria;
import me.labb.bookrecommender.server.oggetti.Libro;

import java.sql.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Classe DAO per operazioni CRUD sulle librerie personali degli utenti.
 * Gestisce tutte le operazioni di accesso ai dati relative alle librerie utente.
 *
 * @author Caretti Gabriele 756564 VA
 * @author Como Riccardo 758697 VA
 * @author Manicone Giorgia 758716 VA
 */
public class LibreriaDAO {
    private final DatabaseManager dbManager;

    /**
     * Inizializza la connessione al database manager.
     */
    public LibreriaDAO() {
        this.dbManager = DatabaseManager.getInstance();
    }

    /**
     * @param userID       ID dell'utente proprietario
     * @param nomeLibreria Nome della libreria
     * @return ID della libreria appena creata
     * @throws SQLException In caso di errori SQL
     */
    public int creaLibreria(int userID, String nomeLibreria) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = dbManager.getConnection();
            String sql = """
                    INSERT INTO "Librerie" ("UserID", "NomeLibreria", "DataCreazione")
                    VALUES (?, ?, CURRENT_TIMESTAMP)
                    RETURNING "LibreriaID"
                    """;

            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userID);
            stmt.setString(2, nomeLibreria);

            rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            } else {
                throw new SQLException("Errore nella creazione della libreria, nessun ID ritornato");
            }
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        }
    }

    /**
     * Elimina una libreria e tutto il suo contenuto.
     *
     * @param libreriaID ID della libreria da eliminare
     * @throws SQLException In caso di errori SQL
     */
    public void eliminaLibreria(int libreriaID) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt1 = null;
        PreparedStatement stmt2 = null;
        boolean success = false;

        try {
            conn = dbManager.getConnection();
            conn.setAutoCommit(false); // Inizia transazione
            // Prima elimina tutti i contenuti della libreria
            String sqlContenuto = """
                    DELETE FROM "ContenutoLibreria"
                    WHERE "LibreriaID" = ?
                    """;

            stmt1 = conn.prepareStatement(sqlContenuto);
            stmt1.setInt(1, libreriaID);
            stmt1.executeUpdate();

            // Poi elimina la libreria stessa
            String sqlLibreria = """
                    DELETE FROM "Librerie"
                    WHERE "LibreriaID" = ?
                    """;

            stmt2 = conn.prepareStatement(sqlLibreria);
            stmt2.setInt(1, libreriaID);
            int rowsDeleted = stmt2.executeUpdate();

            conn.commit(); // Conferma la transazione
            success = (rowsDeleted > 0); // true se almeno una riga è stata eliminata

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback(); // Annulla la transazione in caso di errore
                } catch (SQLException rollbackEx) {
                    System.err.println("Errore durante il rollback: " + rollbackEx.getMessage());
                }
            }
            throw e;
        } finally {
            if (stmt1 != null) stmt1.close();
            if (stmt2 != null) stmt2.close();
            if (conn != null) {
                try {
                    conn.setAutoCommit(true); // Ripristina l'auto-commit
                } catch (SQLException autoCommitEx) {
                    System.err.println("Errore nel ripristino auto-commit: " + autoCommitEx.getMessage());
                }
                conn.close();
            }
        }

    }

    /**
     * Ottiene tutte le librerie di un utente.
     *
     * @param userID ID dell'utente
     * @return Lista di librerie dell'utente
     * @throws SQLException In caso di errori SQL
     */
    public List<Libreria> getLibrerieUtente(int userID) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<Libreria> librerie = new ArrayList<>();

        try {
            conn = dbManager.getConnection();
            String sql = """
                    SELECT * FROM "Librerie"
                    WHERE "UserID" = ?
                    ORDER BY "DataCreazione" DESC
                    """;

            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userID);

            rs = stmt.executeQuery();

            while (rs.next()) {
                Timestamp ts = rs.getTimestamp("DataCreazione");
                ZonedDateTime dataCreazione = (ts != null) ? ts.toInstant().atZone(ZoneId.systemDefault()) : null;
                Libreria libreria = new Libreria(
                        rs.getInt("LibreriaID"),
                        rs.getInt("UserID"),
                        rs.getString("NomeLibreria"),
                        dataCreazione
                );
                librerie.add(libreria);
            }

            return librerie;
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        }
    }

    /**
     * Ottiene una libreria specifica tramite ID.
     *
     * @param libreriaID ID della libreria da trovare
     * @return Optional contenente la libreria se trovata, altrimenti vuoto
     * @throws SQLException In caso di errori SQL
     */
    public Optional<Libreria> getLibreriaById(int libreriaID) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = dbManager.getConnection();
            String sql = """
                    SELECT * FROM "Librerie"
                    WHERE "LibreriaID" = ?
                    """;

            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, libreriaID);

            rs = stmt.executeQuery();

            if (rs.next()) {
                Timestamp ts = rs.getTimestamp("DataCreazione");
                ZonedDateTime dataCreazione = (ts != null) ? ts.toInstant().atZone(ZoneId.systemDefault()) : null;
                Libreria libreria = new Libreria(
                        rs.getInt("LibreriaID"),
                        rs.getInt("UserID"),
                        rs.getString("NomeLibreria"),
                        dataCreazione
                );
                return Optional.of(libreria);
            }

            return Optional.empty();
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        }
    }

    /**
     * Aggiunge un libro a una libreria.
     *
     * @param libreriaID ID della libreria
     * @param libroID    ID del libro da aggiungere
     * @throws SQLException In caso di errori SQL
     */
    public void aggiungiLibroALibreria(int libreriaID, int libroID) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = dbManager.getConnection();
            String sql = """
                    INSERT INTO "ContenutoLibreria" ("LibreriaID", "LibroID", "DataAggiunta")
                    VALUES (?, ?, CURRENT_TIMESTAMP)
                    ON CONFLICT ("LibreriaID", "LibroID") DO NOTHING
                    """;

            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, libreriaID);
            stmt.setInt(2, libroID);

            stmt.executeUpdate();
        } finally {
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        }
    }

    /**
     * Rimuove un libro da una libreria.
     *
     * @param libreriaID ID della libreria
     * @param libroID    ID del libro da rimuovere
     * @throws SQLException In caso di errori SQL
     */
    public void rimuoviLibroDaLibreria(int libreriaID, int libroID) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = dbManager.getConnection();
            String sql = """
                    DELETE FROM "ContenutoLibreria"
                    WHERE "LibreriaID" = ? AND "LibroID" = ?
                    """;

            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, libreriaID);
            stmt.setInt(2, libroID);

            stmt.executeUpdate();
        } finally {
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        }
    }

    /**
     * Ottiene tutti i libri in una libreria.
     *
     * @param libreriaID ID della libreria
     * @return Lista di libri nella libreria
     * @throws SQLException In caso di errori SQL
     */
    public List<Libro> getLibriInLibreria(int libreriaID) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<Libro> libri = new ArrayList<>();

        try {
            conn = dbManager.getConnection();
            String sql = """
                    SELECT l.* FROM "Libri" l
                    JOIN "ContenutoLibreria" lil ON l."LibroID" = lil."LibroID"
                    WHERE lil."LibreriaID" = ?
                    ORDER BY lil."DataAggiunta" DESC
                    """;

            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, libreriaID);

            rs = stmt.executeQuery();

            while (rs.next()) {
                Libro libro = new Libro(
                        rs.getInt("LibroID"),
                        rs.getString("Titolo"),
                        rs.getString("Autori"),
                        rs.getString("Descrizione"),
                        rs.getString("Categoria"),
                        rs.getString("Editore"),
                        rs.getFloat("Prezzo"),
                        rs.getString("MesePubblicazione"),
                        rs.getInt("AnnoPubblicazione")
                );
                libri.add(libro);
            }

            return libri;
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        }
    }

    /**
     * Rinomina una libreria esistente.
     *
     * @param libreriaID ID della libreria da rinominare
     * @param nuovoNome  Nuovo nome da assegnare alla libreria
     * @return true se la libreria è stata rinominata con successo, false altrimenti
     * @throws SQLException In caso di errori SQL
     */
    public boolean rinominaLibreria(int libreriaID, String nuovoNome) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = dbManager.getConnection();
            String sql = """
                    UPDATE "Librerie"
                    SET "NomeLibreria" = ?
                    WHERE "LibreriaID" = ?
                    """;

            stmt = conn.prepareStatement(sql);
            stmt.setString(1, nuovoNome);
            stmt.setInt(2, libreriaID);

            int rowsUpdated = stmt.executeUpdate();
            return rowsUpdated > 0;
        } finally {
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        }
    }
}