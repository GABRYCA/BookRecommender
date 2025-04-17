package me.labb.bookrecommender.server.oggetti;

/**
 * Record per rappresentare un libro letto dal file CSV.
 */
public record Libro (
        String titolo,
        String autori,
        String descrizione,
        String categoria,
        String editore,
        float prezzo,
        String mesePubblicazione,
        int annoPubblicazione
) {}

