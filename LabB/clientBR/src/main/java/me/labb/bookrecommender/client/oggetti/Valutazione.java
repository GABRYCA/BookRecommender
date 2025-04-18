package me.labb.bookrecommender.client.oggetti;

import java.time.ZonedDateTime;

/**
 * Record per rappresentare una valutazione di un libro da parte di un utente.
 */
public record Valutazione (
        int valutazioneID,
        int userID,
        int libroID,
        short scoreStile,
        String noteStile,
        short scoreContenuto,
        String noteContenuto,
        short scoreGradevolezza,
        String noteGradevolezza,
        short scoreOriginalita,
        String noteOriginalita,
        short scoreEdizione,
        String noteEdizione,
        ZonedDateTime dataValutazione
) {}
