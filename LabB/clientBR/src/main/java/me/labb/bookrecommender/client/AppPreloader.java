package me.labb.bookrecommender.client;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Preloader;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.BoxBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/**
 * Preloader dell'applicazione BookRecommender che mostra una schermata di caricamento
 * mentre l'applicazione principale si sta avviando.
 *
 * @author Caretti Gabriele 756564 VA
 * @author Como Riccardo 758697 VA
 * @author Manicone Giorgia 758716 VA
 */
public class AppPreloader extends Preloader {

    /**
     * Stage del preloader
     */
    private Stage preloaderStage;
    /**
     * Barra di progresso del caricamento
     */
    private ProgressBar progressBar;
    /**
     * Etichetta di stato del caricamento
     */
    private Label statusLabel;

    /**
     * @param stage Lo stage del preloader
     * @throws Exception Se si verifica un errore durante il caricamento dell'interfaccia
     */
    @Override
    public void start(Stage stage) throws Exception {
        this.preloaderStage = stage;

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/Splash.fxml"));
        Parent root = loader.load();

        // Carica immagine nel logo
        ImageView logoImage = (ImageView) loader.getNamespace().get("logoImage");
        logoImage.setImage(new Image(getClass().getResourceAsStream("/SplashLogo.png")));

        progressBar = (ProgressBar) loader.getNamespace().get("progressBar");
        statusLabel = (Label) loader.getNamespace().get("statusLabel");

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/SplashScreen.css").toExternalForm());
        stage.initStyle(StageStyle.TRANSPARENT);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);
        stage.show();

        simulateProgress();
    }

    /**
     * Simula il progresso del caricamento attraverso un'animazione fluida.
     * Crea una timeline che aggiorna progressivamente la barra di progresso
     * e l'etichetta di stato dal 0% al 100%.
     */
    private void simulateProgress() {
        Timeline timeline = new Timeline();
        final int maxSteps = 100;

        for (int i = 0; i <= maxSteps; i++) {
            final double progress = i / (double) maxSteps;
            final int currentStep = i;

            KeyFrame keyFrame = new KeyFrame(Duration.millis(i * 20), event -> {
                progressBar.setProgress(progress);

                // Messaggi di stato più dettagliati e realistici
                String statusMessage = getLoadingMessage(currentStep);
                statusLabel.setText(statusMessage + " " + currentStep + "%");
            });
            timeline.getKeyFrames().add(keyFrame);
        }

        timeline.play();
    }

    private String getLoadingMessage(int progress) {
        if (progress < 20) {
            return "Inizializzazione sistema...";
        } else if (progress < 40) {
            return "Caricamento database...";
        } else if (progress < 60) {
            return "Connessione al server...";
        } else if (progress < 80) {
            return "Preparazione interfaccia...";
        } else if (progress < 95) {
            return "Finalizzazione...";
        } else {
            return "Completamento...";
        }
    }

    /**
     * Gestisce le notifiche di progresso dall'applicazione principale.
     * Può essere utilizzato per sincronizzare il progresso del preloader
     * con il caricamento effettivo dell'applicazione.
     *
     * @param info Le informazioni di notifica dal preloader
     */
    @Override
    public void handleApplicationNotification(PreloaderNotification info) {
        if (info instanceof ProgressNotification pn) {
            // progressBar.setProgress(pn.getProgress());
            // statusLabel.setText("Caricamento... " + (int)(pn.getProgress() * 100) + "%");
        }
    }

    /**
     * Gestisce i cambiamenti di stato dell'applicazione principale.
     * Nasconde il preloader quando l'applicazione è pronta per l'avvio.
     *
     * @param evt L'evento di cambiamento di stato
     */
    @Override
    public void handleStateChangeNotification(StateChangeNotification evt) {
        if (evt.getType() == StateChangeNotification.Type.BEFORE_START) {
            preloaderStage.hide();
        }
    }
}
