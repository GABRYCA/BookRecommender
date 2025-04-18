package me.labb.bookrecommender.server.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utility per analizzare le richieste in arrivo dai client.
 * Supporta sia il formato TEXT che JSON.
 */
public class RequestParser {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Richiesta con comando e parametri.
     */
    public static class ParsedRequest {
        private final String comando;
        private final String parametri;

        public ParsedRequest(String comando, String parametri) {
            this.comando = comando;
            this.parametri = parametri != null ? parametri : "";
        }

        public String getComando() {
            return comando;
        }

        public String getParametri() {
            return parametri;
        }
    }

    /**
     * Analizza input e determina se sia in formato JSON o TEXT.
     * Estrae il comando e i parametri dalla richiesta.
     *
     * @param input Richiesta in ingresso
     * @return Oggetto ParsedRequest contenente comando e parametri
     */
    public static ParsedRequest parseRequest(String input) {
        if (input == null || input.trim().isEmpty()) {
            return new ParsedRequest("", "");
        }

        // Verifica se l'input sia in formato JSON (o TEXT)
        if (isJsonRequest(input)) {
            return parseJsonRequest(input);
        } else {
            return parseTextRequest(input);
        }
    }

    /**
     * Verifica se una richiesta sia in formato JSON.
     *
     * @param input Richiesta da verificare
     * @return true se la richiesta è JSON, false altrimenti
     */
    public static boolean isJsonRequest(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }

        String trimmed = input.trim();
        return (trimmed.startsWith("{") && trimmed.endsWith("}"));
    }

    /**
     * Analizza una richiesta in formato TEXT.
     *
     * @param input Richiesta in formato TEXT
     * @return Oggetto ParsedRequest contenente comando e parametri
     */
    private static ParsedRequest parseTextRequest(String input) {
        String[] parti = input.split("\\s+", 2);
        String comando = parti[0].toUpperCase();
        String parametri = parti.length > 1 ? parti[1] : "";
        return new ParsedRequest(comando, parametri);
    }

    /**
     * Analizza una richiesta in formato JSON.
     *
     * @param jsonInput La richiesta in formato JSON
     * @return Oggetto ParsedRequest contenente comando e parametri
     */
    private static ParsedRequest parseJsonRequest(String jsonInput) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonInput);

            // Estraggo il comando dal JSON
            JsonNode commandNode = rootNode.get("comando");
            if (commandNode == null) {
                return new ParsedRequest("", "");
            }

            String comando = commandNode.asText().toUpperCase();

            // Estraggo i parametri dal JSON
            JsonNode paramsNode = rootNode.get("parametri");
            String parametri = "";

            if (paramsNode != null) {
                if (paramsNode.isTextual()) {
                    // Se i parametri sono una stringa
                    parametri = paramsNode.asText();
                } else {
                    // Gestione speciale per comandi specifici con parametri
                    parametri = switch (comando) {
                        case "LOGIN" -> formatLoginParams(paramsNode);
                        case "REGISTRA" -> formatRegistraParams(paramsNode);
                        case "VALUTA_LIBRO" -> formatValutaLibroParams(paramsNode);
                        case "AGGIUNGI_LIBRO", "RIMUOVI_LIBRO" -> formatLibroParams(paramsNode);
                        case "SALVA_CONSIGLIO" -> formatConsiglioParams(paramsNode);
                        default -> objectMapper.writeValueAsString(paramsNode);
                    };
                }
            }

            return new ParsedRequest(comando, parametri);
        } catch (JsonProcessingException e) {
            System.err.println("Errore nell'analisi della richiesta JSON: " + e.getMessage());
            return new ParsedRequest("", "");
        }
    }

    /**
     * Formatta i parametri per il comando LOGIN.
     *
     * @param paramsNode JSON dei parametri
     * @return Stringa formattata dei parametri
     */
    private static String formatLoginParams(JsonNode paramsNode) {
        String username = paramsNode.has("username") ? paramsNode.get("username").asText() : "";
        String password = paramsNode.has("password") ? paramsNode.get("password").asText() : "";
        return username + " " + password;
    }

    /**
     * Formatta i parametri per il comando REGISTRA.
     *
     * @param paramsNode JSON dei parametri
     * @return Stringa formattata dei parametri
     */
    private static String formatRegistraParams(JsonNode paramsNode) {
        String nomeCompleto = paramsNode.has("nomeCompleto") ? paramsNode.get("nomeCompleto").asText() : "";
        String email = paramsNode.has("email") ? paramsNode.get("email").asText() : "";
        String username = paramsNode.has("username") ? paramsNode.get("username").asText() : "";
        String password = paramsNode.has("password") ? paramsNode.get("password").asText() : "";
        String codiceFiscale = paramsNode.has("codiceFiscale") ? paramsNode.get("codiceFiscale").asText() : "";

        return nomeCompleto + " " + email + " " + username + " " + password +
                (codiceFiscale.isEmpty() ? "" : " " + codiceFiscale);
    }

    /**
     * Formatta i parametri per il comando VALUTA_LIBRO.
     *
     * @param paramsNode JSON dei parametri
     * @return Stringa formattata dei parametri
     */
    private static String formatValutaLibroParams(JsonNode paramsNode) {
        StringBuilder params = new StringBuilder();

        // Parametri
        params.append(paramsNode.has("libroID") ? paramsNode.get("libroID").asText() : "0").append(" ");
        params.append(paramsNode.has("scoreStile") ? paramsNode.get("scoreStile").asText() : "0").append(" ");
        params.append(paramsNode.has("noteStile") ? paramsNode.get("noteStile").asText() : "").append(" ");
        params.append(paramsNode.has("scoreContenuto") ? paramsNode.get("scoreContenuto").asText() : "0").append(" ");
        params.append(paramsNode.has("noteContenuto") ? paramsNode.get("noteContenuto").asText() : "").append(" ");
        params.append(paramsNode.has("scoreGradevolezza") ? paramsNode.get("scoreGradevolezza").asText() : "0").append(" ");
        params.append(paramsNode.has("noteGradevolezza") ? paramsNode.get("noteGradevolezza").asText() : "").append(" ");
        params.append(paramsNode.has("scoreOriginalita") ? paramsNode.get("scoreOriginalita").asText() : "0").append(" ");
        params.append(paramsNode.has("noteOriginalita") ? paramsNode.get("noteOriginalita").asText() : "").append(" ");
        params.append(paramsNode.has("scoreEdizione") ? paramsNode.get("scoreEdizione").asText() : "0").append(" ");
        params.append(paramsNode.has("noteEdizione") ? paramsNode.get("noteEdizione").asText() : "");

        return params.toString();
    }

    /**
     * Formatta i parametri per i comandi AGGIUNGI_LIBRO e RIMUOVI_LIBRO.
     *
     * @param paramsNode JSON dei parametri
     * @return Stringa formattata dei parametri
     */
    private static String formatLibroParams(JsonNode paramsNode) {
        String libreriaID = paramsNode.has("libreriaID") ? paramsNode.get("libreriaID").asText() : "";
        String libroID = paramsNode.has("libroID") ? paramsNode.get("libroID").asText() : "";
        return libreriaID + " " + libroID;
    }

    /**
     * Formatta i parametri per il comando SALVA_CONSIGLIO.
     *
     * @param paramsNode JSON dei parametri
     * @return Stringa formattata dei parametri
     */
    private static String formatConsiglioParams(JsonNode paramsNode) {
        String libroRiferimentoID = paramsNode.has("libroRiferimentoID") ? paramsNode.get("libroRiferimentoID").asText() : "";
        String libroSuggeritoID = paramsNode.has("libroSuggeritoID") ? paramsNode.get("libroSuggeritoID").asText() : "";
        return libroRiferimentoID + " " + libroSuggeritoID;
    }
}
