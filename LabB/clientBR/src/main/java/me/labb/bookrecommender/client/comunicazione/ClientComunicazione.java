package me.labb.bookrecommender.client.comunicazione;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 * Classe che gestisce la comunicazione tra il client e il server.
 * Supporta sia il formato testuale che JSON per le comunicazioni.
 *
 * @author Caretti Gabriele 756564 VA
 * @author Como Riccardo 758697 VA
 * @author Manicone Giorgia 758716 VA
 */
public class ClientComunicazione {

    // Format
    public static final String FORMAT_TEXT = "TEXT";
    public static final String FORMAT_JSON = "JSON";

    // Format di default
    private String formatoDefault = FORMAT_JSON;

    // Informazioni di connessione
    private final String serverAddress;
    private final int serverPort;

    // Socket e stream per la comunicazione
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    // Jackson ObjectMapper (JSON)
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Stato connessione
    private boolean connesso = false;

    /**
     * @param serverAddress Indirizzo del server
     * @param serverPort    Porta del server
     */
    public ClientComunicazione(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }

    /**
     * Stabilisce una connessione con il server.
     *
     * @return true se la connessione è stata stabilita con successo, false altrimenti
     * @throws IOException se si verifica un errore durante la connessione
     */
    public boolean connetti() throws IOException {
        if (connesso) {
            return true;
        }

        try {
            socket = new Socket(serverAddress, serverPort);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Legge il messaggio di benvenuto dal server
            String welcomeMessage = in.readLine();
            System.out.println("Server: " + welcomeMessage);

            connesso = true;
            return true;
        } catch (IOException e) {
            chiudi();
            throw e;
        }
    }

    /**
     * Chiude la connessione con il server.
     */
    public void chiudi() {
        try {
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Errore durante la chiusura della connessione: " + e.getMessage());
        } finally {
            connesso = false;
            socket = null;
            out = null;
            in = null;
        }
    }

    /**
     * Verifica se il client è connesso al server.
     *
     * @return true se il client è connesso, false altrimenti
     */
    public boolean isConnesso() {
        return connesso && socket != null && !socket.isClosed();
    }

    /**
     * Invia un comando al server e riceve la risposta.
     *
     * @param comando   Il comando da inviare
     * @param parametri I parametri del comando (può essere null)
     * @return La risposta del server
     * @throws IOException           se si verifica un errore durante la comunicazione
     * @throws IllegalStateException se il client non è connesso al server
     */
    public synchronized String inviaComando(String comando, String parametri) throws IOException {
        if (!isConnesso()) {
            throw new IllegalStateException("Il client non è connesso al server");
        }
        String richiesta;
        if (formatoDefault.equals(FORMAT_JSON)) {
            richiesta = formattaRichiestaJSON(comando, parametri);
        } else {
            richiesta = formattaRichiestaTesto(comando, parametri);
        }
        out.println(richiesta);
        return in.readLine();
    }

    /**
     * Invia un comando al server con parametri strutturati e riceve la risposta.
     *
     * @param comando   Il comando da inviare
     * @param parametri I parametri del comando come mappa chiave-valore
     * @return La risposta del server
     * @throws IOException           se si verifica un errore durante la comunicazione
     * @throws IllegalStateException se il client non è connesso al server
     */
    public String inviaComando(String comando, Map<String, Object> parametri) throws IOException {
        if (!isConnesso()) {
            throw new IllegalStateException("Il client non è connesso al server");
        }

        String richiesta;
        if (formatoDefault.equals(FORMAT_JSON)) {
            richiesta = formattaRichiestaJSONStrutturata(comando, parametri);
        } else {
            // Converti i parametri in una stringa
            StringBuilder paramsStr = new StringBuilder();
            if (parametri != null) {
                for (Map.Entry<String, Object> entry : parametri.entrySet()) {
                    paramsStr.append(entry.getValue()).append(" ");
                }
            }
            richiesta = formattaRichiestaTesto(comando, paramsStr.toString().trim());
        }

        out.println(richiesta);
        return in.readLine();
    }

    /**
     * Formatta una richiesta in formato testo.
     *
     * @param comando   Il comando da inviare
     * @param parametri I parametri del comando (può essere null)
     * @return La richiesta formattata
     */
    private String formattaRichiestaTesto(String comando, String parametri) {
        if (parametri == null || parametri.isEmpty()) {
            return comando;
        } else {
            return comando + " " + parametri;
        }
    }

    /**
     * Formatta una richiesta in formato JSON.
     *
     * @param comando   Il comando da inviare
     * @param parametri I parametri del comando (può essere null)
     * @return La richiesta formattata in JSON
     */
    private String formattaRichiestaJSON(String comando, String parametri) {
        Map<String, Object> richiesta = new HashMap<>();
        richiesta.put("comando", comando);
        if (parametri != null && !parametri.isEmpty()) {
            richiesta.put("parametri", parametri);
        }

        try {
            return objectMapper.writeValueAsString(richiesta);
        } catch (JsonProcessingException e) {
            System.err.println("Errore nella serializzazione JSON: " + e.getMessage());
            return formattaRichiestaTesto(comando, parametri);
        }
    }

    /**
     * Formatta una richiesta in formato JSON con parametri strutturati.
     *
     * @param comando   Il comando da inviare
     * @param parametri I parametri del comando come mappa chiave-valore
     * @return La richiesta formattata in JSON
     */
    private String formattaRichiestaJSONStrutturata(String comando, Map<String, Object> parametri) {
        Map<String, Object> richiesta = new HashMap<>();
        richiesta.put("comando", comando);
        if (parametri != null && !parametri.isEmpty()) {
            richiesta.put("parametri", parametri);
        }

        try {
            return objectMapper.writeValueAsString(richiesta);
        } catch (JsonProcessingException e) {
            System.err.println("Errore nella serializzazione JSON: " + e.getMessage());
            return formattaRichiestaTesto(comando, "");
        }
    }

    /**
     * Analizza una risposta dal server e verifica se è un successo.
     *
     * @param risposta La risposta dal server
     * @return true se la risposta indica successo, false altrimenti
     */
    public boolean isSuccesso(String risposta) {
        if (risposta == null || risposta.isEmpty()) {
            return false;
        }

        if (formatoDefault.equals(FORMAT_JSON)) {
            try {
                JsonNode rootNode = objectMapper.readTree(risposta);
                JsonNode statusNode = rootNode.get("status");
                return statusNode != null && "SUCCESS".equals(statusNode.asText());
            } catch (JsonProcessingException e) {
                System.err.println("Errore nell'analisi della risposta JSON: " + e.getMessage());
                return false;
            }
        } else {
            return risposta.contains("STATUS: SUCCESS");
        }
    }

    /**
     * Estrae il messaggio da una risposta del server.
     *
     * @param risposta La risposta dal server
     * @return Il messaggio contenuto nella risposta
     */
    public String estraiMessaggio(String risposta) {
        if (risposta == null || risposta.isEmpty()) {
            return "";
        }

        if (formatoDefault.equals(FORMAT_JSON)) {
            try {
                JsonNode rootNode = objectMapper.readTree(risposta);
                JsonNode messageNode = rootNode.get("message");
                return messageNode != null ? messageNode.asText() : "";
            } catch (JsonProcessingException e) {
                System.err.println("Errore nell'analisi della risposta JSON: " + e.getMessage());
                return "";
            }
        } else {
            // Estrai il messaggio dal formato testo
            String[] lines = risposta.split("\n");
            for (String line : lines) {
                if (line.startsWith("MESSAGE: ")) {
                    return line.substring("MESSAGE: ".length());
                }
            }
            return "";
        }
    }

    /**
     * Estrae i dati da una risposta del server.
     *
     * @param risposta La risposta dal server
     * @return I dati contenuti nella risposta come mappa chiave-valore, o null se non ci sono dati
     */
    public Map<String, Object> estraiDati(String risposta) {
        if (risposta == null || risposta.isEmpty()) {
            return null;
        }

        if (formatoDefault.equals(FORMAT_JSON)) {
            try {
                JsonNode rootNode = objectMapper.readTree(risposta);
                JsonNode dataNode = rootNode.get("data");
                if (dataNode == null || dataNode.isEmpty()) {
                    return null;
                }

                return objectMapper.convertValue(dataNode, Map.class);
            } catch (JsonProcessingException e) {
                System.err.println("Errore nell'analisi della risposta JSON: " + e.getMessage());
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Imposta il formato di default per le comunicazioni.
     *
     * @param formato Il formato da utilizzare (FORMAT_TEXT o FORMAT_JSON)
     */
    public void setFormatoDefault(String formato) {
        if (FORMAT_TEXT.equals(formato) || FORMAT_JSON.equals(formato)) {
            this.formatoDefault = formato;
        } else {
            throw new IllegalArgumentException("Formato non valido: " + formato);
        }
    }

    /**
     * Ottiene il formato di default attuale per le comunicazioni.
     *
     * @return Il formato di default attuale
     */
    public String getFormatoDefault() {
        return formatoDefault;
    }
}