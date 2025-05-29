package me.labb.bookrecommender.server.oggetti;

/**
 * Record per rappresentare un libro nel sistema BookRecommender.
 * Contiene tutte le informazioni necessarie per identificare e descrivere un libro.
 *
 * @param libroId           Identificativo univoco del libro
 * @param titolo            Titolo del libro
 * @param autori            Lista degli autori del libro
 * @param descrizione       Descrizione dettagliata del contenuto del libro
 * @param categoria         Categoria o genere del libro
 * @param editore           Casa editrice che ha pubblicato il libro
 * @param prezzo            Prezzo del libro
 * @param mesePubblicazione Mese di pubblicazione
 * @param annoPubblicazione Anno di pubblicazione
 * @author Caretti Gabriele 756564 VA
 * @author Como Riccardo 758697 VA
 * @author Manicone Giorgia 758716 VA
 */
public record Libro(
        int libroId,
        String titolo,
        String autori,
        String descrizione,
        String categoria,
        String editore,
        float prezzo,
        String mesePubblicazione,
        int annoPubblicazione
) {
}