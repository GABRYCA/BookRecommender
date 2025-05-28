package me.labb.bookrecommender.client;

import com.sun.javafx.application.LauncherImpl;
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

public class ClientMain extends Application {
    public static void main(String[] args) {
        LauncherImpl.launchApplication(ClientMain.class, AppPreloader.class, args);
    }

    @Override
    public void init() throws Exception {

        for (int i = 1; i <= 10; i++) {
            double progress = i / 10.0;
            notifyPreloader(new Preloader.ProgressNotification(progress));
            Thread.sleep(200);
        }
    }

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