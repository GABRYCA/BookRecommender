package me.labb.bookrecommender.server.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility per formattare i dati di risposta.
 * Per comunicazione client-server.
 * Sperimentale: supporto risposte testo e JSON.
 *
 * @author Caretti Gabriele 756564 VA
 * @author Como Riccardo 758697 VA
 * @author Manicone Giorgia 758716 VA
 */
public class ResponseFormatter {

    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_ERROR = "ERROR";

    public static final String FORMAT_TEXT = "TEXT";
    public static final String FORMAT_JSON = "JSON";

    private static String defaultFormat = FORMAT_JSON;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Crea messaggio di successo con eventuali dati.
     * Supporta sia TEXT che JSON.
     * 
     * @param message Messaggio di successo
     * @param data Dati opzionali inclusi nella risposta
     * @return String formattata di successo
     */
    public static String successo(String message, Map<String, Object> data) {
        if (FORMAT_JSON.equals(defaultFormat)) {
            return successoJson(message, data);
        } else {
            return successoText(message, data);
        }
    }

    /**
     * Crea messaggio di successo (senza dati).
     * Supporta sia TEXT che JSON.
     * 
     * @param message Messaggio di successo
     * @return String formattata di successo
     */
    public static String successo(String message) {
        return successo(message, null);
    }

    /**
     * Crea messaggio d'errore (senza dati).
     * Supporta sia TEXT che JSON.
     * 
     * @param message Messaggio d'errore
     * @return String formattata di errore
     */
    public static String errore(String message) {
        if (FORMAT_JSON.equals(defaultFormat)) {
            return erroreJson(message);
        } else {
            return erroreText(message);
        }
    }

    /**
     * Crea messaggio TEXT di successo con eventuali dati.
     * 
     * @param message Messaggio di successo
     * @param data Dati opzionali inclusi nella risposta
     * @return String formattato TEXT
     */
    public static String successoText(String message, Map<String, Object> data) {
        StringBuilder response = new StringBuilder();
        response.append("STATUS: ").append(STATUS_SUCCESS).append("\n");
        response.append("MESSAGE: ").append(message).append("\n");

        if (data != null && !data.isEmpty()) {
            response.append("DATA_BEGIN\n");
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                formattaDataEntry(response, entry.getKey(), entry.getValue());
            }
            response.append("DATA_END\n");
        }

        return response.toString();
    }

    /**
     * Crea messaggio TEXT d'errore
     * 
     * @param message Messaggio d'errore
     * @return String formattato TEXT
     */
    public static String erroreText(String message) {
        StringBuilder response = new StringBuilder();
        response.append("STATUS: ").append(STATUS_ERROR).append("\n");
        response.append("MESSAGE: ").append(message).append("\n");
        return response.toString();
    }

    /**
     * Formatta un dato per la risposta
     * 
     * @param response StringBuilder
     * @param key Chiave Data
     * @param value Valore Data
     */
    private static void formattaDataEntry(StringBuilder response, String key, Object value) {
        if (value instanceof List<?>) {
            formattaListEntry(response, key, (List<?>) value);
        } else if (value instanceof Map<?, ?>) {
            formattaMapEntry(response, key, (Map<?, ?>) value);
        } else {
            response.append(key).append(": ").append(value).append("\n");
        }
    }

    /**
     * Formatta una lista per la risposta
     * 
     * @param response StringBuilder
     * @param key Chiave Data
     * @param list Valore List
     */
    private static void formattaListEntry(StringBuilder response, String key, List<?> list) {
        response.append(key).append("_COUNT: ").append(list.size()).append("\n");
        response.append(key).append("_BEGIN\n");

        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            response.append("ITEM_").append(i).append("_BEGIN\n");

            if (item instanceof Map<?, ?> itemMap) {
                for (Map.Entry<?, ?> entry : itemMap.entrySet()) {
                    response.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                }
            } else {
                response.append("VALUE: ").append(item).append("\n");
            }

            response.append("ITEM_").append(i).append("_END\n");
        }

        response.append(key).append("_END\n");
    }

    /**
     * Formatta una map per la risposta
     * 
     * @param response StringBuilder
     * @param key Chiave Data
     * @param map Valore Map
     */
    private static void formattaMapEntry(StringBuilder response, String key, Map<?, ?> map) {
        response.append(key).append("_BEGIN\n");

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            response.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }

        response.append(key).append("_END\n");
    }

    /**
     * Crea map con singolo valore (oggetto)
     * 
     * @param key Chiave Entry
     * @param value Valore Entry
     * @return Map con il singolo valore
     */
    public static Map<String, Object> singletonMap(String key, Object value) {
        Map<String, Object> map = new HashMap<>();
        map.put(key, value);
        return map;
    }

    /**
     * Importa il formato di risposta di default
     * 
     * @param format Formato (FORMAT_TEXT o FORMAT_JSON)
     */
    public static void setDefaultFormat(String format) {
        if (FORMAT_TEXT.equals(format) || FORMAT_JSON.equals(format)) {
            defaultFormat = format;
        } else {
            throw new IllegalArgumentException("Invalid format: " + format);
        }
    }

    /**
     * Formato di default attuale
     * 
     * @return Formato attuale di default
     */
    public static String getDefaultFormat() {
        return defaultFormat;
    }

    /**
     * Crea risposta JSON di successo con eventuali dati
     * 
     * @param message Messaggio di successo
     * @param data Dati opzionali
     * @return String in formato JSON
     */
    public static String successoJson(String message, Map<String, Object> data) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", STATUS_SUCCESS);
        response.put("message", message);

        if (data != null && !data.isEmpty()) {
            response.put("data", data);
        }

        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            System.err.println("Error serializing JSON response: " + e.getMessage());
            return successo(message, data);
        }
    }

    /**
     * Crea risposta JSON di successo senza dati
     * 
     * @param message Messaggio di successo
     * @return String in formato JSON
     */
    public static String successoJson(String message) {
        return successoJson(message, null);
    }

    /**
     * Crea risposta JSON di errore
     * 
     * @param message Messaggio
     * @return String in formato JSON
     */
    public static String erroreJson(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", STATUS_ERROR);
        response.put("message", message);

        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            System.err.println("Error serializing JSON response: " + e.getMessage());
            return errore(message);
        }
    }
}
