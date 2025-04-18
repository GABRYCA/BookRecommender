package me.labb.bookrecommender.server.db;

import me.labb.bookrecommender.server.oggetti.Libreria;
import me.labb.bookrecommender.server.oggetti.Libro;

import java.sql.*;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Classe DAO per operazioni CRUD sulle librerie personali degli utenti.
 */
public class LibreriaDAO {
    private final DatabaseManager dbManager;

    public LibreriaDAO() {
        this.dbManager = DatabaseManager.getInstance();
    }

    /**
     * Crea una nuova libreria per un utente.
     *
     * @param userID ID dell'utente proprietario
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
                Libreria libreria = new Libreria(
                        rs.getInt("LibreriaID"),
                        rs.getInt("UserID"),
                        rs.getString("NomeLibreria"),
                        rs.getObject("DataCreazione", ZonedDateTime.class)
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
                Libreria libreria = new Libreria(
                        rs.getInt("LibreriaID"),
                        rs.getInt("UserID"),
                        rs.getString("NomeLibreria"),
                        rs.getObject("DataCreazione", ZonedDateTime.class)
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
     * @param libroID ID del libro da aggiungere
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
     * @param libroID ID del libro da rimuovere
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
                        rs.getString("Titolo"),
                        rs.getString("Autori"),
                        rs.getString("Descrizione"),
                        rs.getString("Categoria"),
                        rs.getString("Editore"),
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
}