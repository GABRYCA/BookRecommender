package me.labb.bookrecommender.server.oggetti;

import java.time.ZonedDateTime;

/**
 * Record per rappresentare un utente registrato.
 */
public record Utente (
        int userID,
        String nomeCompleto,
        String codiceFiscale,
        String email,
        String username,
        String passwordHash,
        ZonedDateTime dataRegistrazione
) {}