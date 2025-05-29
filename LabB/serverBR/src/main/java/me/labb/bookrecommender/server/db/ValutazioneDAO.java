package me.labb.bookrecommender.server.db;

import me.labb.bookrecommender.server.oggetti.Valutazione;

import java.sql.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Classe DAO per operazioni CRUD sulle valutazioni dei libri.
 * Gestisce tutte le operazioni di accesso ai dati relative alle valutazioni degli utenti.
 * 
 * @author Caretti Gabriele 756564 VA
 * @author Como Riccardo 758697 VA
 * @author Manicone Giorgia 758716 VA
 */
public class ValutazioneDAO {    private final DatabaseManager dbManager;

    /**
     * Costruttore della classe ValutazioneDAO.
     * Inizializza la connessione al database manager.
     */
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
     */    public int salvaValutazione(int userID, int libroID,
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
            
            // Disabilita autocommit per gestire la transazione manualmente
            conn.setAutoCommit(false);
            
            System.out.println("INFO: Salvando valutazione per userID=" + userID + ", libroID=" + libroID);
            
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
                        System.out.println("INFO: Aggiornando valutazione esistente ID=" + valutazioneID);
                        
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
                            
                            int rowsAffected = updateStmt.executeUpdate();
                            System.out.println("INFO: Update eseguito, righe modificate: " + rowsAffected);
                            
                            // Commit della transazione
                            conn.commit();
                            System.out.println("INFO: Transazione di aggiornamento committata con successo");
                            
                            return valutazioneID;
                        }
                    }
                }
            }
            
            // Inserisci una nuova valutazione
            System.out.println("INFO: Inserendo nuova valutazione");
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
                int newValutazioneID = rs.getInt(1);
                System.out.println("INFO: Nuova valutazione inserita con ID=" + newValutazioneID);
                
                // Commit della transazione
                conn.commit();
                System.out.println("INFO: Transazione di inserimento committata con successo");
                
                return newValutazioneID;
            } else {
                conn.rollback();
                throw new SQLException("Errore nel salvataggio della valutazione, nessun ID ritornato");
            }
        } catch (SQLException e) {
            System.err.println("ERRORE: Eccezione SQL durante il salvataggio della valutazione: " + e.getMessage());
            if (conn != null) {
                try {
                    conn.rollback();
                    System.out.println("INFO: Transazione rollback eseguito");
                } catch (SQLException rollbackEx) {
                    System.err.println("ERRORE: Impossibile eseguire rollback: " + rollbackEx.getMessage());
                }
            }
            throw e;
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) {
                    conn.setAutoCommit(true); // Ripristina autocommit
                    conn.close();
                }
            } catch (SQLException e) {
                System.err.println("ERRORE: Errore nella chiusura delle risorse: " + e.getMessage());
            }
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
                Timestamp ts = rs.getTimestamp("DataValutazione");
                ZonedDateTime dataValutazione = (ts != null) ? ts.toInstant().atZone(ZoneId.systemDefault()) : null;
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
                        dataValutazione
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
                Timestamp ts = rs.getTimestamp("DataValutazione");
                ZonedDateTime dataValutazione = (ts != null) ? ts.toInstant().atZone(ZoneId.systemDefault()) : null;
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
                        dataValutazione
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
                Timestamp ts = rs.getTimestamp("DataValutazione");
                ZonedDateTime dataValutazione = (ts != null) ? ts.toInstant().atZone(ZoneId.systemDefault()) : null;
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
                        dataValutazione
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