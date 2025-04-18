package me.labb.bookrecommender.client.comunicazione;

import me.labb.bookrecommender.client.oggetti.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Classe che fornisce le operazioni di alto livello per il client.
 * Utilizza la classe ClientComunicazione per la comunicazione con il server.
 * Implementa le operazioni principali supportate dal server.
 */
public class ClientOperazioni {
    
    // Client di comunicazione
    private final ClientComunicazione client;
    
    // ObjectMapper per la gestione del JSON
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Utente attualmente autenticato
    private Utente utenteAutenticato = null;
    
    /**
     * Costruttore con indirizzo e porta del server.
     * 
     * @param serverAddress Indirizzo del server
     * @param serverPort Porta del server
     */
    public ClientOperazioni(String serverAddress, int serverPort) {
        this.client = new ClientComunicazione(serverAddress, serverPort);
    }
    
    /**
     * Costruttore con client di comunicazione già configurato.
     * 
     * @param client Client di comunicazione
     */
    public ClientOperazioni(ClientComunicazione client) {
        this.client = client;
    }
    
    /**
     * Stabilisce una connessione con il server.
     * 
     * @return true se la connessione è stata stabilita con successo, false altrimenti
     * @throws IOException se si verifica un errore durante la connessione
     */
    public boolean connetti() throws IOException {
        return client.connetti();
    }
    
    /**
     * Chiude la connessione con il server.
     */
    public void chiudi() {
        client.chiudi();
    }
    
    /**
     * Verifica se il client è connesso al server.
     * 
     * @return true se il client è connesso, false altrimenti
     */
    public boolean isConnesso() {
        return client.isConnesso();
    }
    
    /**
     * Verifica se l'utente è autenticato.
     * 
     * @return true se l'utente è autenticato, false altrimenti
     */
    public boolean isAutenticato() {
        return utenteAutenticato != null;
    }
    
    /**
     * Ottiene l'utente attualmente autenticato.
     * 
     * @return L'utente autenticato, o null se non c'è un utente autenticato
     */
    public Utente getUtenteAutenticato() {
        return utenteAutenticato;
    }
    
    /**
     * Effettua il login di un utente.
     * 
     * @param username Username o email dell'utente
     * @param password Password dell'utente
     * @return true se il login è avvenuto con successo, false altrimenti
     * @throws IOException se si verifica un errore durante la comunicazione
     */
    public boolean login(String username, String password) throws IOException {
        Map<String, Object> parametri = new HashMap<>();
        parametri.put("username", username);
        parametri.put("password", password);
        
        String risposta = client.inviaComando("LOGIN", parametri);
        
        if (client.isSuccesso(risposta)) {
            Map<String, Object> dati = client.estraiDati(risposta);
            if (dati != null && dati.containsKey("utente")) {
                Map<String, Object> utenteMap = (Map<String, Object>) dati.get("utente");
                
                // Crea l'oggetto Utente
                utenteAutenticato = new Utente(
                    ((Number) utenteMap.get("userID")).intValue(),
                    (String) utenteMap.get("nomeCompleto"),
                    null, // codiceFiscale non restituito dal server nella risposta di login
                    (String) utenteMap.get("email"),
                    (String) utenteMap.get("username"),
                    null, // passwordHash non restituito dal server
                    ZonedDateTime.now() // dataRegistrazione non restituita dal server nella risposta di login
                );
                
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Effettua il logout dell'utente corrente.
     * 
     * @return true se il logout è avvenuto con successo, false altrimenti
     * @throws IOException se si verifica un errore durante la comunicazione
     * @throws IllegalStateException se l'utente non è autenticato
     */
    public boolean logout() throws IOException {
        if (!isAutenticato()) {
            throw new IllegalStateException("Nessun utente autenticato");
        }
        
        String risposta = client.inviaComando("LOGOUT", "");
        
        if (client.isSuccesso(risposta)) {
            utenteAutenticato = null;
            return true;
        }
        
        return false;
    }
    
    /**
     * Registra un nuovo utente.
     * 
     * @param nomeCompleto Nome completo dell'utente
     * @param email Email dell'utente
     * @param username Username dell'utente
     * @param password Password dell'utente
     * @param codiceFiscale Codice fiscale dell'utente (opzionale)
     * @return L'ID dell'utente registrato, o -1 se la registrazione fallisce
     * @throws IOException se si verifica un errore durante la comunicazione
     */
    public int registra(String nomeCompleto, String email, String username, String password, String codiceFiscale) throws IOException {
        Map<String, Object> parametri = new HashMap<>();
        parametri.put("nomeCompleto", nomeCompleto);
        parametri.put("email", email);
        parametri.put("username", username);
        parametri.put("password", password);
        if (codiceFiscale != null && !codiceFiscale.isEmpty()) {
            parametri.put("codiceFiscale", codiceFiscale);
        }
        
        String risposta = client.inviaComando("REGISTRA", parametri);
        
        if (client.isSuccesso(risposta)) {
            Map<String, Object> dati = client.estraiDati(risposta);
            if (dati != null && dati.containsKey("userID")) {
                return ((Number) dati.get("userID")).intValue();
            }
        }
        
        return -1;
    }
    
    /**
     * Registra un nuovo utente senza codice fiscale.
     * 
     * @param nomeCompleto Nome completo dell'utente
     * @param email Email dell'utente
     * @param username Username dell'utente
     * @param password Password dell'utente
     * @return L'ID dell'utente registrato, o -1 se la registrazione fallisce
     * @throws IOException se si verifica un errore durante la comunicazione
     */
    public int registra(String nomeCompleto, String email, String username, String password) throws IOException {
        return registra(nomeCompleto, email, username, password, null);
    }
    
    /**
     * Ottiene le informazioni del profilo dell'utente autenticato.
     * 
     * @return L'utente con le informazioni complete del profilo, o null se l'operazione fallisce
     * @throws IOException se si verifica un errore durante la comunicazione
     * @throws IllegalStateException se l'utente non è autenticato
     */
    public Utente visualizzaProfilo() throws IOException {
        if (!isAutenticato()) {
            throw new IllegalStateException("Nessun utente autenticato");
        }
        
        String risposta = client.inviaComando("PROFILO", "");
        
        if (client.isSuccesso(risposta)) {
            Map<String, Object> dati = client.estraiDati(risposta);
            if (dati != null && dati.containsKey("profilo")) {
                Map<String, Object> profiloMap = (Map<String, Object>) dati.get("profilo");
                
                // Crea l'oggetto Utente con le informazioni complete
                return new Utente(
                    ((Number) profiloMap.get("userID")).intValue(),
                    (String) profiloMap.get("nome"),
                    (String) profiloMap.getOrDefault("codiceFiscale", ""),
                    (String) profiloMap.get("email"),
                    (String) profiloMap.get("username"),
                    null, // passwordHash non restituito dal server
                    ZonedDateTime.parse((String) profiloMap.get("dataRegistrazione"))
                );
            }
        }
        
        return null;
    }
    
    /**
     * Cerca libri nel database in base a un termine di ricerca.
     * 
     * @param termine Termine di ricerca
     * @return Lista di libri trovati, o una lista vuota se nessun libro è stato trovato
     * @throws IOException se si verifica un errore durante la comunicazione
     */
    public List<Libro> cercaLibri(String termine) throws IOException {
        String risposta = client.inviaComando("CERCA", termine);
        
        List<Libro> libri = new ArrayList<>();
        
        if (client.isSuccesso(risposta)) {
            Map<String, Object> dati = client.estraiDati(risposta);
            if (dati != null && dati.containsKey("libri")) {
                List<Map<String, Object>> libriList = (List<Map<String, Object>>) dati.get("libri");
                
                for (Map<String, Object> libroMap : libriList) {
                    // Crea l'oggetto Libro
                    Libro libro = new Libro(
                        (String) libroMap.get("titolo"),
                        (String) libroMap.get("autori"),
                        "", // descrizione non restituita dal server nella risposta di ricerca
                        (String) libroMap.get("categoria"),
                        "", // editore non restituito dal server nella risposta di ricerca
                        ((Number) libroMap.get("prezzo")).floatValue(),
                        "", // mesePubblicazione non restituito dal server nella risposta di ricerca
                        0 // annoPubblicazione non restituito dal server nella risposta di ricerca
                    );
                    
                    libri.add(libro);
                }
            }
        }
        
        return libri;
    }
    
    /**
     * Ottiene consigli di libri in base a una categoria.
     * 
     * @param categoria Categoria per cui ottenere consigli
     * @return Lista di libri consigliati, o una lista vuota se nessun libro è stato trovato
     * @throws IOException se si verifica un errore durante la comunicazione
     */
    public List<Libro> consigliaLibri(String categoria) throws IOException {
        String risposta = client.inviaComando("CONSIGLIA", categoria);
        
        List<Libro> libri = new ArrayList<>();
        
        if (client.isSuccesso(risposta)) {
            Map<String, Object> dati = client.estraiDati(risposta);
            if (dati != null && dati.containsKey("libri")) {
                List<Map<String, Object>> libriList = (List<Map<String, Object>>) dati.get("libri");
                
                for (Map<String, Object> libroMap : libriList) {
                    // Crea l'oggetto Libro
                    Libro libro = new Libro(
                        (String) libroMap.get("titolo"),
                        (String) libroMap.get("autori"),
                        "", // descrizione non restituita dal server nella risposta di consiglio
                        (String) libroMap.get("categoria"),
                        "", // editore non restituito dal server nella risposta di consiglio
                        ((Number) libroMap.get("prezzo")).floatValue(),
                        "", // mesePubblicazione non restituito dal server nella risposta di consiglio
                        0 // annoPubblicazione non restituito dal server nella risposta di consiglio
                    );
                    
                    libri.add(libro);
                }
            }
        }
        
        return libri;
    }
    
    /**
     * Imposta il formato di default per le comunicazioni.
     * 
     * @param formato Il formato da utilizzare (FORMAT_TEXT o FORMAT_JSON)
     * @return true se il formato è stato impostato con successo, false altrimenti
     * @throws IOException se si verifica un errore durante la comunicazione
     */
    public boolean impostaFormato(String formato) throws IOException {
        String risposta = client.inviaComando("FORMAT", formato);
        
        if (client.isSuccesso(risposta)) {
            client.setFormatoDefault(formato);
            return true;
        }
        
        return false;
    }
}