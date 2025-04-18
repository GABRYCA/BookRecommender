package me.labb.bookrecommender.client.comunicazione;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Classe di utilità per analizzare le risposte dal server.
 * Fornisce metodi per estrarre dati specifici dalle risposte.
 */
public class RispostaParser {
    
    // Formati
    public static final String FORMAT_TEXT = "TEXT";
    public static final String FORMAT_JSON = "JSON";
    
    // Jackson ObjectMapper (JSON)
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Verifica se una risposta è di successo.
     * 
     * @param risposta La risposta dal server
     * @param formato Il formato della risposta (FORMAT_TEXT o FORMAT_JSON)
     * @return true se la risposta indica successo, false altrimenti
     */
    public static boolean isSuccesso(String risposta, String formato) {
        if (risposta == null || risposta.isEmpty()) {
            return false;
        }
        
        if (FORMAT_JSON.equals(formato)) {
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
     * Estrae il messaggio da una risposta.
     * 
     * @param risposta La risposta dal server
     * @param formato Il formato della risposta (FORMAT_TEXT o FORMAT_JSON)
     * @return Il messaggio contenuto nella risposta
     */
    public static String estraiMessaggio(String risposta, String formato) {
        if (risposta == null || risposta.isEmpty()) {
            return "";
        }
        
        if (FORMAT_JSON.equals(formato)) {
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
     * Estrae i dati da una risposta.
     *
     * @param risposta La risposta dal server
     * @param formato Il formato della risposta (FORMAT_TEXT o FORMAT_JSON)
     * @return I dati contenuti nella risposta come mappa chiave-valore, o null se non ci sono dati
     */
    public static Map<String, Object> estraiDati(String risposta, String formato) {
        if (risposta == null || risposta.isEmpty()) {
            return null;
        }
        
        if (FORMAT_JSON.equals(formato)) {
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
            // Estrai i dati dal formato testo
            Map<String, Object> dati = new HashMap<>();
            boolean inData = false;
            String currentKey = null;
            StringBuilder currentValue = new StringBuilder();
            
            String[] lines = risposta.split("\n");
            for (String line : lines) {
                if (line.equals("DATA_BEGIN")) {
                    inData = true;
                } else if (line.equals("DATA_END")) {
                    inData = false;
                } else if (inData) {
                    int colonIndex = line.indexOf(": ");
                    if (colonIndex > 0) {
                        if (currentKey != null) {
                            dati.put(currentKey, currentValue.toString());
                            currentValue = new StringBuilder();
                        }
                        currentKey = line.substring(0, colonIndex);
                        currentValue.append(line.substring(colonIndex + 2));
                    } else {
                        currentValue.append("\n").append(line);
                    }
                }
            }
            
            if (currentKey != null) {
                dati.put(currentKey, currentValue.toString());
            }
            
            return dati.isEmpty() ? null : dati;
        }
    }
    
    /**
     * Estrae una lista di oggetti da una risposta JSON.
     * 
     * @param risposta La risposta dal server
     * @param chiaveLista La chiave della lista nei dati
     * @return La lista di oggetti, o una lista vuota se non ci sono oggetti
     */
    public static List<Map<String, Object>> estraiLista(String risposta, String chiaveLista) {
        List<Map<String, Object>> lista = new ArrayList<>();
        
        Map<String, Object> dati = estraiDati(risposta, FORMAT_JSON);
        if (dati != null && dati.containsKey(chiaveLista)) {
            List<Map<String, Object>> listaObj = (List<Map<String, Object>>) dati.get(chiaveLista);
            if (listaObj != null) {
                lista.addAll(listaObj);
            }
        }
        
        return lista;
    }
    
    /**
     * Estrae un valore da una stringa di testo.
     * 
     * @param testo Il testo da cui estrarre il valore
     * @param prefisso Il prefisso che precede il valore
     * @return Il valore estratto, o una stringa vuota se il valore non è trovato
     */
    public static String estraiValore(String testo, String prefisso) {
        if (testo == null || prefisso == null) {
            return "";
        }
        
        int indice = testo.indexOf(prefisso);
        if (indice < 0) {
            return "";
        }
        
        int inizio = indice + prefisso.length();
        int fine = testo.indexOf('\n', inizio);
        
        if (fine < 0) {
            return testo.substring(inizio);
        } else {
            return testo.substring(inizio, fine);
        }
    }
    
    /**
     * Estrae un ID da un messaggio di risposta.
     * 
     * @param messaggio Il messaggio da cui estrarre l'ID
     * @param prefisso Il prefisso che precede l'ID (es. "ID: ")
     * @return L'ID estratto, o -1 se l'ID non è trovato o non è valido
     */
    public static int estraiID(String messaggio, String prefisso) {
        if (messaggio == null || prefisso == null) {
            return -1;
        }
        
        int indice = messaggio.indexOf(prefisso);
        if (indice < 0) {
            return -1;
        }
        
        int inizio = indice + prefisso.length();
        int fine = messaggio.indexOf(')', inizio);
        
        if (fine < 0) {
            fine = messaggio.indexOf('\n', inizio);
            if (fine < 0) {
                fine = messaggio.length();
            }
        }
        
        try {
            return Integer.parseInt(messaggio.substring(inizio, fine).trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}