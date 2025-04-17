package me.labb.bookrecommender.server;

import me.labb.bookrecommender.server.csv.CsvReader;
import me.labb.bookrecommender.server.db.DatabaseManager;
import me.labb.bookrecommender.server.oggetti.Libro;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class ServerMain {

    public static DatabaseManager dbm;

    public static void main(String[] args) {

        System.out.println("Avvio server...");

        // Inizializzo database.
        dbm = DatabaseManager.getInstance();

        // Faccio una prova
        try {
            Connection c = dbm.getConnection();
            System.out.println("Schema letto: " + c.getSchema());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        // Temporaneo, una volta messi nel database elimineremo questo dal main!
        CsvReader reader = new CsvReader();
        List<Libro> libroList = reader.readLibri();

        if (libroList.isEmpty()) {
            System.out.println("Nessun libro letto o file non trovato.");
        } else {
            System.out.println("Primi 5 libri letti:");
            libroList.stream().limit(5).forEach((libro) -> System.out.println(libro));
        }

        System.out.println("Chiusura server...");
    }
}
