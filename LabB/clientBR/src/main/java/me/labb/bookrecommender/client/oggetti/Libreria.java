package me.labb.bookrecommender.client.oggetti;

import java.time.ZonedDateTime;

/**
 * Record per rappresentare una libreria personale di un utente.
 */
public record Libreria (
        int libreriaID,
        int userID,
        String nomeLibreria,
        ZonedDateTime dataCreazione
) {}
