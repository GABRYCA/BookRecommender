package me.labb.bookrecommender.server.db;

import me.labb.bookrecommender.server.oggetti.Libro;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Classe DAO per operazioni CRUD sui libri nel database.
 * Gestisce tutte le operazioni di accesso ai dati relative ai libri.
 *
 * @author Caretti Gabriele 756564 VA
 * @author Como Riccardo 758697 VA
 * @author Manicone Giorgia 758716 VA
 */
public class LibroDAO {
    private final DatabaseManager dbManager;

    /**
     * Costruttore della classe LibroDAO.
     */
    public LibroDAO() {
        this.dbManager = DatabaseManager.getInstance();
    }

    /**
     * Ottiene un libro dal database tramite ID.
     *
     * @param libroID ID del libro da trovare
     * @return Optional contenente il libro se trovato, altrimenti vuoto
     * @throws SQLException In caso di errori SQL
     */
    public Optional<Libro> getLibroById(int libroID) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = dbManager.getConnection();
            String sql = """
                    SELECT * FROM "Libri" WHERE "LibroID" = ?
                    """;
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, libroID);
            rs = stmt.executeQuery();

            if (rs.next()) {
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
                return Optional.of(libro);
            }

            return Optional.empty();
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        }
    }

    /**
     * Cerca libri per titolo o autore.
     *
     * @param query Testo da cercare nel titolo o autore
     * @param limit Numero massimo di risultati
     * @return Lista di libri trovati
     * @throws SQLException In caso di errori SQL
     */
    public List<Libro> cercaLibri(String query, int limit) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<Libro> risultati = new ArrayList<>();

        try {
            conn = dbManager.getConnection();
            String sql = """
                    SELECT * FROM "Libri" 
                    WHERE "Titolo" ILIKE ? OR "Autori" ILIKE ?
                    ORDER BY "Titolo" ASC
                    LIMIT ?
                    """;
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, "%" + query + "%");
            stmt.setString(2, "%" + query + "%");
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
                risultati.add(libro);
            }

            return risultati;
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        }
    }

    /**
     * Filtra libri per categoria.
     *
     * @param categoria Categoria da filtrare
     * @param limit     Numero massimo di risultati
     * @return Lista di libri nella categoria specificata
     * @throws SQLException In caso di errori SQL
     */
    public List<Libro> getLibriByCategoria(String categoria, int limit) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<Libro> risultati = new ArrayList<>();

        try {
            conn = dbManager.getConnection();
            String sql = """
                    SELECT * FROM "Libri" 
                    WHERE "Categoria" = ?
                    ORDER BY "Titolo" ASC
                    LIMIT ?
                    """;
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, categoria);
            stmt.setInt(2, limit);
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
                risultati.add(libro);
            }

            return risultati;
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        }
    }

    /**
     * Ottiene tutte le categorie uniche presenti nel database.
     *
     * @return Lista di categorie
     * @throws SQLException In caso di errori SQL
     */
    public List<String> getAllCategorie() throws SQLException {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        List<String> categorie = new ArrayList<>();

        try {
            conn = dbManager.getConnection();
            String sql = """
                    SELECT DISTINCT "Categoria" FROM "Libri" 
                    ORDER BY "Categoria" ASC
                    """;
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);

            while (rs.next()) {
                categorie.add(rs.getString("Categoria"));
            }

            return categorie;
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        }
    }
}