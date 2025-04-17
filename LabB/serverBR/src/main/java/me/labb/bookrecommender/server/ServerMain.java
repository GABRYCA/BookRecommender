package me.labb.bookrecommender.server;

import me.labb.bookrecommender.server.db.DatabaseManager;

import java.sql.Connection;
import java.sql.SQLException;

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

        System.out.println("Chiusura server...");
    }
}
