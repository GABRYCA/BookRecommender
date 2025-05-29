package me.labb.bookrecommender.client;

import me.labb.bookrecommender.client.comunicazione.ClientComunicazione;
import me.labb.bookrecommender.client.comunicazione.ClientOperazioni;
import me.labb.bookrecommender.client.oggetti.Libro;
import me.labb.bookrecommender.client.oggetti.Utente;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

/**
 * Client di test per dimostrare la funzionalità della comunicazione client-server.
 * Usiamo un'interfaccia a riga di comando per interagire con il server.
 *
 * @author Caretti Gabriele 756564 VA
 * @author Como Riccardo 758697 VA
 * @author Manicone Giorgia 758716 VA
 */
public class TestClient {
    
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 8080;
    
    private static final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    private static ClientOperazioni client;
    
    public static void main(String[] args) {
        client = new ClientOperazioni(SERVER_ADDRESS, SERVER_PORT);
        
        System.out.println("=== Client BookRecommender ===");
        
        try {
            boolean esci = false;
            
            while (!esci) {
                if (!client.isConnesso()) {
                    System.out.println("\nNon sei connesso al server.");
                    System.out.println("1. Connetti al server");
                    System.out.println("0. Esci");
                    
                    int scelta = leggiIntero("Scelta: ");
                    
                    switch (scelta) {
                        case 1:
                            connetti();
                            break;
                        case 0:
                            esci = true;
                            break;
                        default:
                            System.out.println("Scelta non valida.");
                    }
                } else if (!client.isAutenticato()) {
                    System.out.println("\nSei connesso al server ma non sei autenticato.");
                    System.out.println("1. Login");
                    System.out.println("2. Registrati");
                    System.out.println("3. Cerca libri");
                    System.out.println("4. Consiglia libri per categoria");
                    System.out.println("5. Disconnetti");
                    System.out.println("0. Esci");
                    
                    int scelta = leggiIntero("Scelta: ");
                    
                    switch (scelta) {
                        case 1:
                            login();
                            break;
                        case 2:
                            registra();
                            break;
                        case 3:
                            cercaLibri();
                            break;
                        case 4:
                            consigliaLibri();
                            break;
                        case 5:
                            disconnetti();
                            break;
                        case 0:
                            esci = true;
                            break;
                        default:
                            System.out.println("Scelta non valida.");
                    }
                } else {
                    System.out.println("\nSei connesso e autenticato come " + client.getUtenteAutenticato().username() + ".");
                    System.out.println("1. Visualizza profilo");
                    System.out.println("2. Cerca libri");
                    System.out.println("3. Consiglia libri per categoria");
                    System.out.println("4. Logout");
                    System.out.println("5. Disconnetti");
                    System.out.println("0. Esci");
                    
                    int scelta = leggiIntero("Scelta: ");
                    
                    switch (scelta) {
                        case 1:
                            visualizzaProfilo();
                            break;
                        case 2:
                            cercaLibri();
                            break;
                        case 3:
                            consigliaLibri();
                            break;
                        case 4:
                            logout();
                            break;
                        case 5:
                            disconnetti();
                            break;
                        case 0:
                            esci = true;
                            break;
                        default:
                            System.out.println("Scelta non valida.");
                    }
                }
            }
            
            System.out.println("Arrivederci!");
            
        } catch (Exception e) {
            System.err.println("Errore: " + e.getMessage());
            e.printStackTrace();
        } finally {
            client.chiudi();
        }
    }
    
    /**
     * Connette il client al server.
     */
    private static void connetti() {
        System.out.println("Connessione al server...");
        
        try {
            if (client.connetti()) {
                System.out.println("Connessione stabilita con successo.");
            } else {
                System.out.println("Impossibile connettersi al server.");
            }
        } catch (IOException e) {
            System.err.println("Errore durante la connessione: " + e.getMessage());
        }
    }
    
    /**
     * Disconnette il client dal server.
     */
    private static void disconnetti() {
        System.out.println("Disconnessione dal server...");
        client.chiudi();
        System.out.println("Disconnessione completata.");
    }
    
    /**
     * Effettua il login di un utente.
     */
    private static void login() {
        try {
            System.out.println("\n=== Login ===");
            String username = leggiStringa("Username: ");
            String password = leggiStringa("Password: ");
            
            if (client.login(username, password)) {
                System.out.println("Login effettuato con successo.");
            } else {
                System.out.println("Login fallito. Credenziali non valide.");
            }
        } catch (IOException e) {
            System.err.println("Errore durante il login: " + e.getMessage());
        }
    }
    
    /**
     * Effettua il logout dell'utente corrente.
     */
    private static void logout() {
        try {
            if (client.logout()) {
                System.out.println("Logout effettuato con successo.");
            } else {
                System.out.println("Logout fallito.");
            }
        } catch (IOException e) {
            System.err.println("Errore durante il logout: " + e.getMessage());
        }
    }
    
    /**
     * Registra un nuovo utente.
     */
    private static void registra() {
        try {
            System.out.println("\n=== Registrazione ===");
            String nomeCompleto = leggiStringa("Nome completo: ");
            String email = leggiStringa("Email: ");
            String username = leggiStringa("Username: ");
            String password = leggiStringa("Password: ");
            String codiceFiscale = leggiStringa("Codice fiscale (opzionale, premi invio per saltare): ");
            
            int userID;
            if (codiceFiscale.isEmpty()) {
                userID = client.registra(nomeCompleto, email, username, password);
            } else {
                userID = client.registra(nomeCompleto, email, username, password, codiceFiscale);
            }
            
            if (userID > 0) {
                System.out.println("Registrazione completata con successo. UserID: " + userID);
            } else {
                System.out.println("Registrazione fallita.");
            }
        } catch (IOException e) {
            System.err.println("Errore durante la registrazione: " + e.getMessage());
        }
    }
    
    /**
     * Visualizza le informazioni del profilo dell'utente autenticato.
     */
    private static void visualizzaProfilo() {
        try {
            Utente utente = client.visualizzaProfilo();
            
            if (utente != null) {
                System.out.println("\n=== Profilo ===");
                System.out.println("UserID: " + utente.userID());
                System.out.println("Nome: " + utente.nomeCompleto());
                System.out.println("Username: " + utente.username());
                System.out.println("Email: " + utente.email());
                
                if (utente.codiceFiscale() != null && !utente.codiceFiscale().isEmpty()) {
                    System.out.println("Codice fiscale: " + utente.codiceFiscale());
                }
                
                System.out.println("Data registrazione: " + utente.dataRegistrazione());
            } else {
                System.out.println("Impossibile recuperare le informazioni del profilo.");
            }
        } catch (IOException e) {
            System.err.println("Errore durante il recupero del profilo: " + e.getMessage());
        }
    }
    
    /**
     * Cerca libri nel database in base a un termine di ricerca.
     */
    private static void cercaLibri() {
        try {
            System.out.println("\n=== Cerca libri ===");
            String termine = leggiStringa("Termine di ricerca: ");
            
            List<Libro> libri = client.cercaLibri(termine);
            
            if (libri.isEmpty()) {
                System.out.println("Nessun libro trovato per il termine: " + termine);
            } else {
                System.out.println("Trovati " + libri.size() + " libri:");
                
                for (Libro libro : libri) {
                    System.out.println("\nTitolo: " + libro.titolo());
                    System.out.println("Autori: " + libro.autori());
                    System.out.println("Categoria: " + libro.categoria());
                    System.out.println("Prezzo: " + libro.prezzo());
                }
            }
        } catch (IOException e) {
            System.err.println("Errore durante la ricerca dei libri: " + e.getMessage());
        }
    }
    
    /**
     * Ottiene consigli di libri in base a una categoria.
     */
    private static void consigliaLibri() {
        try {
            System.out.println("\n=== Consiglia libri ===");
            String categoria = leggiStringa("Categoria: ");
            
            List<Libro> libri = client.consigliaLibri(categoria);
            
            if (libri.isEmpty()) {
                System.out.println("Nessun libro consigliato per la categoria: " + categoria);
            } else {
                System.out.println("Libri consigliati per la categoria '" + categoria + "':");
                
                for (Libro libro : libri) {
                    System.out.println("\nTitolo: " + libro.titolo());
                    System.out.println("Autori: " + libro.autori());
                    System.out.println("Categoria: " + libro.categoria());
                    System.out.println("Prezzo: " + libro.prezzo());
                }
            }
        } catch (IOException e) {
            System.err.println("Errore durante la ricerca dei consigli: " + e.getMessage());
        }
    }
    
    /**
     * Legge una stringa dall'input dell'utente.
     * 
     * @param prompt Il messaggio da mostrare all'utente
     * @return La stringa letta
     */
    private static String leggiStringa(String prompt) {
        System.out.print(prompt);
        try {
            return reader.readLine();
        } catch (IOException e) {
            System.err.println("Errore durante la lettura dell'input: " + e.getMessage());
            return "";
        }
    }
    
    /**
     * Legge un intero dall'input dell'utente.
     * 
     * @param prompt Il messaggio da mostrare all'utente
     * @return L'intero letto, o 0 se l'input non è valido
     */
    private static int leggiIntero(String prompt) {
        System.out.print(prompt);
        try {
            String input = reader.readLine();
            return Integer.parseInt(input);
        } catch (IOException e) {
            System.err.println("Errore durante la lettura dell'input: " + e.getMessage());
            return 0;
        } catch (NumberFormatException e) {
            System.err.println("Input non valido. Inserisci un numero intero.");
            return 0;
        }
    }
}