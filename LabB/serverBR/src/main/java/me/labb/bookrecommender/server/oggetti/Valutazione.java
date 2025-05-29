package me.labb.bookrecommender.server.oggetti;

import java.time.ZonedDateTime;

/**
 * Record per rappresentare una valutazione di un libro da parte di un utente.
 * Contiene tutti i punteggi e commenti relativi ai diversi aspetti di un libro.
 *
 * @param valutazioneID     Identificativo univoco della valutazione
 * @param userID            Identificativo dell'utente che ha fatto la valutazione
 * @param libroID           Identificativo del libro valutato
 * @param scoreStile        Punteggio per lo stile del libro (1-5)
 * @param noteStile         Note aggiuntive sullo stile
 * @param scoreContenuto    Punteggio per il contenuto del libro (1-5)
 * @param noteContenuto     Note aggiuntive sul contenuto
 * @param scoreGradevolezza Punteggio per la gradevolezza del libro (1-5)
 * @param noteGradevolezza  Note aggiuntive sulla gradevolezza
 * @param scoreOriginalita  Punteggio per l'originalità del libro (1-5)
 * @param noteOriginalita   Note aggiuntive sull'originalità
 * @param scoreEdizione     Punteggio per l'edizione del libro (1-5)
 * @param noteEdizione      Note aggiuntive sull'edizione
 * @param dataValutazione   Data e ora in cui è stata creata la valutazione
 * @author Caretti Gabriele 756564 VA
 * @author Como Riccardo 758697 VA
 * @author Manicone Giorgia 758716 VA
 */
public record Valutazione(
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
) {
}
