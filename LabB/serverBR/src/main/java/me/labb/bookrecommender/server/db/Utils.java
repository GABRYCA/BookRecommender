package me.labb.bookrecommender.server.db;

import me.labb.bookrecommender.server.oggetti.Libro;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * Classe Utils.
 */
public class Utils {
    
    private final DatabaseManager dbManager;
    
    public Utils() {
        this.dbManager = DatabaseManager.getInstance();
    }
    
    /**
     * Salva la lista di libri nella tabella Libri del database.
     * NON RIESEGUIRE! L'azione non è necessaria (crea duplicati), i libri sono stati già aggiunti.
     * 
     * @param libri Lista dei libri da salvare
     * @return Il numero di libri inseriti con successo
     * @throws SQLException Se si verifica un errore durante l'operazione sul database
     */
    public int salvaLibri(List<Libro> libri) throws SQLException {
        if (libri == null || libri.isEmpty()) {
            System.out.println("Nessun libro da salvare nel database.");
            return 0;
        }
        
        int contatoreSalvati = 0;
        Connection connection = null;
        PreparedStatement statement = null;
        
        try {
            connection = dbManager.getConnection();
            connection.setAutoCommit(false);
            
            String sql = """
                        INSERT INTO "Libri" ("Titolo", "Autori", "Descrizione", "Categoria", "Editore", "Prezzo", "MesePubblicazione", "AnnoPubblicazione")
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """;
            
            statement = connection.prepareStatement(sql);
            
            for (Libro libro : libri) {
                statement.setString(1, libro.titolo());
                statement.setString(2, libro.autori());
                statement.setString(3, libro.descrizione());
                statement.setString(4, libro.categoria());
                statement.setString(5, libro.editore());
                statement.setFloat(6, libro.prezzo());
                statement.setString(7, libro.mesePubblicazione());
                statement.setInt(8, libro.annoPubblicazione());
                statement.addBatch();
            }
            
            int[] risultati = statement.executeBatch();
            connection.commit();
            
            for (int risultato : risultati) {
                if (risultato > 0) {
                    contatoreSalvati++;
                }
            }
            
            System.out.println("Inseriti " + contatoreSalvati + " libri nel database su " + libri.size() + " totali.");
            return contatoreSalvati;
            
        } catch (SQLException e) {
            if (connection != null) {
                try {
                    connection.rollback();
                    System.err.println("Transazione annullata a causa di un errore.");
                } catch (SQLException ex) {
                    System.err.println("Errore durante il rollback: " + ex.getMessage());
                }
            }
            System.err.println("Errore durante il salvataggio dei libri: " + e.getMessage());
            throw e;
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    System.err.println("Errore durante la chiusura dello statement: " + e.getMessage());
                }
            }
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                    connection.close();
                } catch (SQLException e) {
                    System.err.println("Errore durante la chiusura della connessione: " + e.getMessage());
                }
            }
        }
    }
}
