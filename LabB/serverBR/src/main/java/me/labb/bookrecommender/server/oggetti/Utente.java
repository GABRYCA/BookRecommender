package me.labb.bookrecommender.server.oggetti;

import java.time.ZonedDateTime;

/**
 * Record per rappresentare un utente registrato nel sistema BookRecommender.
 * Contiene tutte le informazioni necessarie per l'autenticazione e la gestione dell'utente.
 * 
 * @param userID Identificativo univoco dell'utente
 * @param nomeCompleto Nome e cognome completo dell'utente
 * @param codiceFiscale Codice fiscale dell'utente
 * @param email Indirizzo email dell'utente
 * @param username Nome utente per l'accesso
 * @param passwordHash Hash della password per la sicurezza
 * @param dataRegistrazione Data e ora di registrazione dell'utente
 * 
 * @author Caretti Gabriele 756564 VA
 * @author Como Riccardo 758697 VA
 * @author Manicone Giorgia 758716 VA
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