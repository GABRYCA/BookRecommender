package me.labb.bookrecommender.server.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {

    private static String dbUrl = "jdbc:postgresql://localhost:5432/bookrecommender_db";
    private static String dbUser = "user";
    private static String dbPassword = "password";
    private static String dbHost = "localhost";

    public static void configure(String host, String databaseName, String user, String password) {
        dbHost = host;
        dbUrl = "jdbc:postgresql://" + host + ":5432/" + databaseName;
        dbUser = user;
        dbPassword = password;
        try {
            Class.forName("org.postgresql.Driver");
            System.out.println("Driver PostgreSQL caricato.");
        } catch (ClassNotFoundException e) {
            System.err.println("Errore: Driver PostgreSQL non trovato! Assicurati che sia nel classpath.");
            throw new RuntimeException(e);
        }
    }

    public static Connection getConnection() throws SQLException {
        System.out.println("Tentativo connessione a: " + dbUrl + " con utente: " + dbUser);
        Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        System.out.println("Connessione al database stabilita.");
        return conn;
    }

    public static String getDbHost() {
        return dbHost;
    }

    public static String getDbUser() {
        return dbUser;
    }

    public static String getDbPassword() {
        return dbPassword;
    }
}