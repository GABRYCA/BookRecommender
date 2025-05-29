package me.labb.bookrecommender.client;

import javafx.application.Application;
import javafx.application.Preloader;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.animation.FadeTransition;
import javafx.util.Duration;

import java.io.IOException;

/**
 * Classe principale dell'applicazione client del sistema BookRecommender.
 * Utilizza JavaFX per l'interfaccia grafica e implementa un preloader per il caricamento.
 * 
 * @author Caretti Gabriele 756564 VA
 * @author Como Riccardo 758697 VA
 * @author Manicone Giorgia 758716 VA
 */
public class ClientMain extends Application {
    
    /**
     * Metodo principale per l'avvio dell'applicazione.
     * Configura il preloader e lancia l'applicazione JavaFX.
     * 
     * @param args Argomenti da linea di comando
     */
    public static void main(String[] args) {
        System.setProperty("javafx.preloader", AppPreloader.class.getCanonicalName());
        Application.launch(ClientMain.class, args);    }

    /**
     * Metodo di inizializzazione chiamato prima dell'avvio dell'interfaccia.
     * Simula il caricamento dell'applicazione aggiornando il preloader.
     * 
     * @throws Exception Se si verifica un errore durante l'inizializzazione
     */
    @Override
    public void init() throws Exception {

        for (int i = 1; i <= 10; i++) {
            double progress = i / 10.0;
            notifyPreloader(new Preloader.ProgressNotification(progress));
            Thread.sleep(200);
        }    }

    /**
     * Metodo di avvio dell'interfaccia grafica JavaFX.
     * Carica il file FXML, configura la scena e visualizza la finestra principale.
     * 
     * @param stage Lo stage principale dell'applicazione
     */
    @Override
    public void start(Stage stage) {
        try {
            // Carica il file FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ClientMain.fxml"));
            Parent root = loader.load();

            // Configura la scena
            Scene scene = new Scene(root, 900, 600);
            scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

            // Configura lo stage
            stage.setTitle("Book Recommender");
            stage.setScene(scene);
            stage.setMinWidth(1300);
            stage.setMinHeight(800);

            // Aggiungi l'icona dell'applicazione
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/SplashLogo.png")));

            // Aggiungi animazione di apertura
            root.setOpacity(0);
            stage.show();

            // Esegui l'animazione di fade in dopo che la finestra è visibile
            FadeTransition fadeIn = new FadeTransition(Duration.millis(800), root);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Errore nel caricamento dell'interfaccia utente: " + e.getMessage());
        }
    }

    @Override
    public void stop() {
        // Esegui operazioni di pulizia alla chiusura dell'applicazione
        System.out.println("Applicazione in chiusura...");
    }
}