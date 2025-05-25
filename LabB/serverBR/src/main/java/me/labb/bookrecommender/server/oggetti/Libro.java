package me.labb.bookrecommender.server.oggetti;

/**
 * Record per rappresentare un libro.
 */
public record Libro (
        int libroId,
        String titolo,
        String autori,
        String descrizione,
        String categoria,
        String editore,
        float prezzo,
        String mesePubblicazione,
        int annoPubblicazione
) {}