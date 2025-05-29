package me.labb.bookrecommender.server.db;

import me.labb.bookrecommender.server.oggetti.Consiglio;
import me.labb.bookrecommender.server.oggetti.Libro;

import java.sql.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Operazioni sui consigli.
 *
 * @author Caretti Gabriele 756564 VA
 * @author Como Riccardo 758697 VA
 * @author Manicone Giorgia 758716 VA
 */
public class ConsiglioDAO {
    private final DatabaseManager dbManager;
    private final LibroDAO libroDAO;

    /**
     *
     */
    public ConsiglioDAO() {
        this.dbManager = DatabaseManager.getInstance();
        this.libroDAO = new LibroDAO();
    }

    /**
     * @param userID             ID dell'utente che ha ricevuto il consiglio
     * @param libroRiferimentoID ID del libro di riferimento
     * @param libroSuggeritoID   ID del libro suggerito
     * @return ID del consiglio appena creato
     * @throws SQLException In caso di errori SQL
     */
    public int salvaConsiglio(int userID, int libroRiferimentoID, int libroSuggeritoID) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = dbManager.getConnection();

            // Verifica se esiste già un consiglio simile
            String checkSql = """
                    SELECT "ConsiglioID" FROM "ConsigliLibri"
                    WHERE "UserID" = ? AND "LibroRiferimentoID" = ? AND "LibroSuggeritoID" = ?
                    """;

            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setInt(1, userID);
                checkStmt.setInt(2, libroRiferimentoID);
                checkStmt.setInt(3, libroSuggeritoID);

                try (ResultSet checkRs = checkStmt.executeQuery()) {
                    if (checkRs.next()) {
                        // Aggiorna la data del consiglio esistente
                        int consiglioID = checkRs.getInt("ConsiglioID");

                        String updateSql = """
                                UPDATE "ConsigliLibri" SET
                                "DataSuggerimento" = CURRENT_TIMESTAMP
                                WHERE "ConsiglioID" = ?
                                """;

                        try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                            updateStmt.setInt(1, consiglioID);
                            updateStmt.executeUpdate();
                            return consiglioID;
                        }
                    }
                }
            }

            // Inserisci un nuovo consiglio
            String insertSql = """
                    INSERT INTO "ConsigliLibri" (
                        "UserID", "LibroRiferimentoID", "LibroSuggeritoID", "DataSuggerimento"
                    ) VALUES (?, ?, ?, CURRENT_TIMESTAMP)
                    RETURNING "ConsiglioID"
                    """;

            stmt = conn.prepareStatement(insertSql);
            stmt.setInt(1, userID);
            stmt.setInt(2, libroRiferimentoID);
            stmt.setInt(3, libroSuggeritoID);

            rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            } else {
                throw new SQLException("Errore nel salvataggio del consiglio, nessun ID ritornato");
            }
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        }
    }

    /**
     * Ottiene un consiglio specifico tramite ID.
     *
     * @param consiglioID ID del consiglio da trovare
     * @return Optional contenente il consiglio se trovato, altrimenti vuoto
     * @throws SQLException In caso di errori SQL
     */
    public Optional<Consiglio> getConsiglioById(int consiglioID) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = dbManager.getConnection();
            String sql = """
                    SELECT * FROM "ConsigliLibri"
                    WHERE "ConsiglioID" = ?
                    """;

            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, consiglioID);

            rs = stmt.executeQuery();

            if (rs.next()) {
                Timestamp ts = rs.getTimestamp("DataSuggerimento");
                ZonedDateTime dataSuggerimento = (ts != null) ? ts.toInstant().atZone(ZoneId.systemDefault()) : null;
                Consiglio consiglio = new Consiglio(
                        rs.getInt("ConsiglioID"),
                        rs.getInt("UserID"),
                        rs.getInt("LibroRiferimentoID"),
                        rs.getInt("LibroSuggeritoID"),
                        dataSuggerimento
                );
                return Optional.of(consiglio);
            }

            return Optional.empty();
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        }
    }

    /**
     * Ottiene tutti i consigli di un utente specifico.
     *
     * @param userID ID dell'utente
     * @return Lista di consigli per l'utente
     * @throws SQLException In caso di errori SQL
     */
    public List<Consiglio> getConsigliUtente(int userID) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<Consiglio> consigli = new ArrayList<>();

        try {
            conn = dbManager.getConnection();
            String sql = """
                    SELECT * FROM "ConsigliLibri"
                    WHERE "UserID" = ?
                    ORDER BY "DataSuggerimento" DESC
                    """;

            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userID);

            rs = stmt.executeQuery();

            while (rs.next()) {
                Timestamp ts = rs.getTimestamp("DataSuggerimento");
                ZonedDateTime dataSuggerimento = (ts != null) ? ts.toInstant().atZone(ZoneId.systemDefault()) : null;
                Consiglio consiglio = new Consiglio(
                        rs.getInt("ConsiglioID"),
                        rs.getInt("UserID"),
                        rs.getInt("LibroRiferimentoID"),
                        rs.getInt("LibroSuggeritoID"),
                        dataSuggerimento
                );
                consigli.add(consiglio);
            }

            return consigli;
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        }
    }

    /**
     * Genera consigli di libri basati su un libro di riferimento.
     * Questo metodo cerca libri nella stessa categoria del libro di riferimento.
     *
     * @param libroRiferimentoID ID del libro di riferimento
     * @param limit              Numero massimo di consigli da generare
     * @return Lista di libri consigliati
     * @throws SQLException In caso di errori SQL
     */
    public List<Libro> generaConsigliPerLibro(int libroRiferimentoID, int limit) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<Libro> libriConsigliati = new ArrayList<>();

        try {
            conn = dbManager.getConnection();

            // Ottengo cateogoria del libro (intendo libro di riferimento, stiamo guardando un libro per esempio
            // e vogliamo vederne i consigliati)
            String categoriaQuery = """
                    SELECT "Categoria" FROM "Libri"
                    WHERE "LibroID" = ?
                    """;

            String categoria = null;
            try (PreparedStatement catStmt = conn.prepareStatement(categoriaQuery)) {
                catStmt.setInt(1, libroRiferimentoID);
                try (ResultSet catRs = catStmt.executeQuery()) {
                    if (catRs.next()) {
                        categoria = catRs.getString("Categoria");
                    } else {
                        return libriConsigliati;
                    }
                }
            }

            // Trovo i libri della stessa categoria
            String sql = """
                    SELECT * FROM "Libri"
                    WHERE "Categoria" = ? AND "LibroID" != ?
                    ORDER BY RANDOM()
                    LIMIT ?
                    """;

            stmt = conn.prepareStatement(sql);
            stmt.setString(1, categoria);
            stmt.setInt(2, libroRiferimentoID);
            stmt.setInt(3, limit);

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
                libriConsigliati.add(libro);
            }

            return libriConsigliati;
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        }
    }

    /**
     * Elimina un consiglio.
     *
     * @param consiglioID ID del consiglio da eliminare
     * @throws SQLException In caso di errori SQL
     */
    public void eliminaConsiglio(int consiglioID) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = dbManager.getConnection();
            String sql = """
                    DELETE FROM "ConsigliLibri"
                    WHERE "ConsiglioID" = ?
                    """;

            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, consiglioID);

            stmt.executeUpdate();
        } finally {
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        }
    }
}