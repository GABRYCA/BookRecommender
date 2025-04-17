package me.labb.bookrecommender.server.oggetti;

/**
 * Record per rappresentare un libro letto dal file CSV.
 */
public record Libro (
        String title,
        String authors,
        String description,
        String category,
        String publisher,
        String price,
        String publishMonth,
        String publishYear
) {}

