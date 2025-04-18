package me.labb.bookrecommender.client.oggetti;

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