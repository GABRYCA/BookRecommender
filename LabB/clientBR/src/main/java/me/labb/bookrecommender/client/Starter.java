package me.labb.bookrecommender.client;

/**
 * Classe di avvio che delega al ClientMain.
 * Fornisce un punto di ingresso alternativo per l'applicazione.
 *
 * @author Caretti Gabriele 756564 VA
 * @author Como Riccardo 758697 VA
 * @author Manicone Giorgia 758716 VA
 */
public class Starter {

    /**
     * Metodo principale che avvia l'applicazione client.
     * 
     * @param args Argomenti da linea di comando passati al ClientMain
     */
    public static void main(final String[] args) {
        ClientMain.main(args);
    }

}
