package me.labb.bookrecommender.server.oggetti;

import java.time.ZonedDateTime;

/**
 * Record per rappresentare un consiglio di un libro basato su un altro libro.
 * I consigli sono generati dagli utenti per suggerire libri simili ad altri.
 * 
 * @param consiglioID Identificativo univoco del consiglio
 * @param userID Identificativo dell'utente che ha fatto il consiglio
 * @param libroRiferimentoID Identificativo del libro di riferimento
 * @param libroSuggeritoID Identificativo del libro suggerito
 * @param dataSuggerimento Data e ora in cui è stato fatto il suggerimento
 * 
 * @author Caretti Gabriele 756564 VA
 * @author Como Riccardo 758697 VA
 * @author Manicone Giorgia 758716 VA
 */
public record Consiglio (
        int consiglioID,
        int userID,
        int libroRiferimentoID,
        int libroSuggeritoID,
        ZonedDateTime dataSuggerimento
) {}
