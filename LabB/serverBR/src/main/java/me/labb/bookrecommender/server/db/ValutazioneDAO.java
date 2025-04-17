package me.labb.bookrecommender.server.db;

import me.labb.bookrecommender.server.oggetti.Valutazione;

import java.sql.*;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Classe DAO per operazioni CRUD sulle valutazioni dei libri.
 */
public class ValutazioneDAO {
    private final DatabaseManager dbManager;

    public ValutazioneDAO() {
        this.dbManager = DatabaseManager.getInstance();
    }

    /**
     * Salva una nuova valutazione di un libro.
     *
     * @param userID ID dell'utente che valuta
     * @param libroID ID del libro valutato
     * @param scoreStile Punteggio per lo stile (1-5)
     * @param noteStile Note sullo stile
     * @param scoreContenuto Punteggio per il contenuto (1-5)
     * @param noteContenuto Note sul contenuto
     * @param scoreGradevolezza Punteggio per la gradevolezza (1-5)
     * @param noteGradevolezza Note sulla gradevolezza
     * @param scoreOriginalita Punteggio per l'originalità (1-5)
     * @param noteOriginalita Note sull'originalità
     * @param scoreEdizione Punteggio per l'edizione (1-5)
     * @param noteEdizione Note sull'edizione
     * @return ID della valutazione appena creata
     * @throws SQLException In caso di errori SQL
     */
    public int salvaValutazione(int userID, int libroID,
                                short scoreStile, String noteStile,
                                short scoreContenuto, String noteContenuto,
                                short scoreGradevolezza, String noteGradevolezza,
                                short scoreOriginalita, String noteOriginalita,
                                short scoreEdizione, String noteEdizione) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = dbManager.getConnection();
            
            // Verifica se esiste già una valutazione per questo utente e libro
            String checkSql = """
                    SELECT "ValutazioneID" FROM "ValutazioniLibri"
                    WHERE "UserID" = ? AND "LibroID" = ?
                    """;
            
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setInt(1, userID);
                checkStmt.setInt(2, libroID);
                
                try (ResultSet checkRs = checkStmt.executeQuery()) {
                    if (checkRs.next()) {
                        // Aggiorna la valutazione esistente
                        int valutazioneID = checkRs.getInt("ValutazioneID");
                        
                        String updateSql = """
                                UPDATE "ValutazioniLibri" SET
                                "ScoreStile" = ?, "NoteStile" = ?,
                                "ScoreContenuto" = ?, "NoteContenuto" = ?,
                                "ScoreGradevolezza" = ?, "NoteGradevolezza" = ?,
                                "ScoreOriginalita" = ?, "NoteOriginalita" = ?,
                                "ScoreEdizione" = ?, "NoteEdizione" = ?,
                                "DataValutazione" = CURRENT_TIMESTAMP
                                WHERE "ValutazioneID" = ?
                                """;
                        
                        try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                            updateStmt.setShort(1, scoreStile);
                            updateStmt.setString(2, noteStile);
                            updateStmt.setShort(3, scoreContenuto);
                            updateStmt.setString(4, noteContenuto);
                            updateStmt.setShort(5, scoreGradevolezza);
                            updateStmt.setString(6, noteGradevolezza);
                            updateStmt.setShort(7, scoreOriginalita);
                            updateStmt.setString(8, noteOriginalita);
                            updateStmt.setShort(9, scoreEdizione);
                            updateStmt.setString(10, noteEdizione);
                            updateStmt.setInt(11, valutazioneID);
                            
                            updateStmt.executeUpdate();
                            return valutazioneID;
                        }
                    }
                }
            }
            
            // Inserisci una nuova valutazione
            String insertSql = """
                    INSERT INTO "ValutazioniLibri" (
                        "UserID", "LibroID",
                        "ScoreStile", "NoteStile",
                        "ScoreContenuto", "NoteContenuto",
                        "ScoreGradevolezza", "NoteGradevolezza",
                        "ScoreOriginalita", "NoteOriginalita",
                        "ScoreEdizione", "NoteEdizione",
                        "DataValutazione"
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                    RETURNING "ValutazioneID"
                    """;
            
            stmt = conn.prepareStatement(insertSql);
            stmt.setInt(1, userID);
            stmt.setInt(2, libroID);
            stmt.setShort(3, scoreStile);
            stmt.setString(4, noteStile);
            stmt.setShort(5, scoreContenuto);
            stmt.setString(6, noteContenuto);
            stmt.setShort(7, scoreGradevolezza);
            stmt.setString(8, noteGradevolezza);
            stmt.setShort(9, scoreOriginalita);
            stmt.setString(10, noteOriginalita);
            stmt.setShort(11, scoreEdizione);
            stmt.setString(12, noteEdizione);
            
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1);
            } else {
                throw new SQLException("Errore nel salvataggio della valutazione, nessun ID ritornato");
            }
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        }
    }

    /**
     * Ottiene una valutazione specifica tramite ID.
     *
     * @param valutazioneID ID della valutazione da trovare
     * @return Optional contenente la valutazione se trovata, altrimenti vuoto
     * @throws SQLException In caso di errori SQL
     */
    public Optional<Valutazione> getValutazioneById(int valutazioneID) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = dbManager.getConnection();
            String sql = """
                    SELECT * FROM "ValutazioniLibri"
                    WHERE "ValutazioneID" = ?
                    """;

            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, valutazioneID);

            rs = stmt.executeQuery();

            if (rs.next()) {
                Valutazione valutazione = new Valutazione(
                        rs.getInt("ValutazioneID"),
                        rs.getInt("UserID"),
                        rs.getInt("LibroID"),
                        rs.getShort("ScoreStile"),
                        rs.getString("NoteStile"),
                        rs.getShort("ScoreContenuto"),
                        rs.getString("NoteContenuto"),
                        rs.getShort("ScoreGradevolezza"),
                        rs.getString("NoteGradevolezza"),
                        rs.getShort("ScoreOriginalita"),
                        rs.getString("NoteOriginalita"),
                        rs.getShort("ScoreEdizione"),
                        rs.getString("NoteEdizione"),
                        rs.getObject("DataValutazione", ZonedDateTime.class)
                );
                return Optional.of(valutazione);
            }

            return Optional.empty();
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        }
    }

    /**
     * Ottiene tutte le valutazioni di un utente.
     *
     * @param userID ID dell'utente
     * @return Lista di valutazioni dell'utente
     * @throws SQLException In caso di errori SQL
     */
    public List<Valutazione> getValutazioniUtente(int userID) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<Valutazione> valutazioni = new ArrayList<>();

        try {
            conn = dbManager.getConnection();
            String sql = """
                    SELECT * FROM "ValutazioniLibri"
                    WHERE "UserID" = ?
                    ORDER BY "DataValutazione" DESC
                    """;

            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userID);

            rs = stmt.executeQuery();

            while (rs.next()) {
                Valutazione valutazione = new Valutazione(
                        rs.getInt("ValutazioneID"),
                        rs.getInt("UserID"),
                        rs.getInt("LibroID"),
                        rs.getShort("ScoreStile"),
                        rs.getString("NoteStile"),
                        rs.getShort("ScoreContenuto"),
                        rs.getString("NoteContenuto"),
                        rs.getShort("ScoreGradevolezza"),
                        rs.getString("NoteGradevolezza"),
                        rs.getShort("ScoreOriginalita"),
                        rs.getString("NoteOriginalita"),
                        rs.getShort("ScoreEdizione"),
                        rs.getString("NoteEdizione"),
                        rs.getObject("DataValutazione", ZonedDateTime.class)
                );
                valutazioni.add(valutazione);
            }

            return valutazioni;
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        }
    }

    /**
     * Ottiene tutte le valutazioni per un libro specifico.
     *
     * @param libroID ID del libro
     * @return Lista di valutazioni per il libro
     * @throws SQLException In caso di errori SQL
     */
    public List<Valutazione> getValutazioniLibro(int libroID) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<Valutazione> valutazioni = new ArrayList<>();

        try {
            conn = dbManager.getConnection();
            String sql = """
                    SELECT * FROM "ValutazioniLibri"
                    WHERE "LibroID" = ?
                    ORDER BY "DataValutazione" DESC
                    """;

            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, libroID);

            rs = stmt.executeQuery();

            while (rs.next()) {
                Valutazione valutazione = new Valutazione(
                        rs.getInt("ValutazioneID"),
                        rs.getInt("UserID"),
                        rs.getInt("LibroID"),
                        rs.getShort("ScoreStile"),
                        rs.getString("NoteStile"),
                        rs.getShort("ScoreContenuto"),
                        rs.getString("NoteContenuto"),
                        rs.getShort("ScoreGradevolezza"),
                        rs.getString("NoteGradevolezza"),
                        rs.getShort("ScoreOriginalita"),
                        rs.getString("NoteOriginalita"),
                        rs.getShort("ScoreEdizione"),
                        rs.getString("NoteEdizione"),
                        rs.getObject("DataValutazione", ZonedDateTime.class)
                );
                valutazioni.add(valutazione);
            }

            return valutazioni;
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        }
    }

    /**
     * Elimina una valutazione.
     *
     * @param valutazioneID ID della valutazione da eliminare
     * @throws SQLException In caso di errori SQL
     */
    public void eliminaValutazione(int valutazioneID) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = dbManager.getConnection();
            String sql = """
                    DELETE FROM "ValutazioniLibri"
                    WHERE "ValutazioneID" = ?
                    """;

            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, valutazioneID);

            stmt.executeUpdate();
        } finally {
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        }
    }
}