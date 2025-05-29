package me.labb.bookrecommender.client.oggetti;

import java.time.ZonedDateTime;

/**
 * Record per rappresentare un consiglio di un libro basato su un altro libro nel client.
 * I consigli sono generati dagli utenti per suggerire libri simili ad altri.
 * 
 * @param consiglioID Identificativo univoco del consiglio
 * @param userID Identificativo dell'utente che ha fatto il consiglio
 * @param libroRiferimentoID Identificativo del libro di riferimento
 * @param libroSuggeritoID Identificativo del libro suggerito
 * @param dataSuggerimento Data e ora in cui è stato fatto il suggerimento
 * @param titoloLibroRiferimento Titolo del libro di riferimento (opzionale)
 * @param titoloLibroSuggerito Titolo del libro suggerito (opzionale)
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
        ZonedDateTime dataSuggerimento,
        String titoloLibroRiferimento,
        String titoloLibroSuggerito
) {
    /**
     * Costruttore per compatibilità con codice esistente (senza titoli).
     * 
     * @param consiglioID Identificativo univoco del consiglio
     * @param userID Identificativo dell'utente che ha fatto il consiglio
     * @param libroRiferimentoID Identificativo del libro di riferimento
     * @param libroSuggeritoID Identificativo del libro suggerito
     * @param dataSuggerimento Data e ora in cui è stato fatto il suggerimento
     */
    public Consiglio(int consiglioID, int userID, int libroRiferimentoID, int libroSuggeritoID, ZonedDateTime dataSuggerimento) {
        this(consiglioID, userID, libroRiferimentoID, libroSuggeritoID, dataSuggerimento, null, null);
    }
}
