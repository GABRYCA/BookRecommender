package me.labb.bookrecommender.client.oggetti;

import java.time.ZonedDateTime;

/**
 * Record per rappresentare un consiglio di un libro basato su un altro libro.
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
     */
    public Consiglio(int consiglioID, int userID, int libroRiferimentoID, int libroSuggeritoID, ZonedDateTime dataSuggerimento) {
        this(consiglioID, userID, libroRiferimentoID, libroSuggeritoID, dataSuggerimento, null, null);
    }
}
