package me.laba.bookrecommender;

import java.util.ArrayList;

/**
 * @author Caretti Gabriele 756564 VA
 * @author Como Riccardo 758697 VA
 * @author Manicone Giorgia 758716 VA
 */
public class Utente {

    private int userId;
    private String nome;
    private String cognome;
    private String codiceFiscale;
    private String email;
    private String password;

    /**
     * Costruttore classe Utente
     *
     * @param userId int
     * @param nome String
     * @param cognome String
     * @param codiceFiscale String
     * @param email String
     * @param password String
     */
    public Utente(int userId, String nome, String cognome, String codiceFiscale, String email, String password) {
        this.userId = userId;
        this.nome = nome;
        this.cognome = cognome;
        this.codiceFiscale = codiceFiscale;
        this.email = email;
        this.password = password;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCognome() {
        return cognome;
    }

    public void setCognome(String cognome) {
        this.cognome = cognome;
    }

    public String getCodiceFiscale() {
        return codiceFiscale;
    }

    public void setCodiceFiscale(String codiceFiscale) {
        this.codiceFiscale = codiceFiscale;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Verifica se l'utente con ID dato esiste già
     *
     * @param utenti ArrayList<Utente>
     * @param userId int
     * @return boolean
     */
    public static boolean esisteUtente(ArrayList<Utente> utenti, int userId) {
        return utenti.stream().anyMatch(utente -> utente.getUserId() == userId);
    }

    /**
     * Verifica se l'utente con email dato esiste già
     *
     * @param utenti ArrayList<Utente>
     * @param email String
     * @return boolean
     */
    public static boolean esisteUtente(ArrayList<Utente> utenti, String email) {
        return utenti.stream().anyMatch(utente -> utente.getEmail().equals(email));
    }

    /**
     * Verifica se l'utente con il codice fiscale dato esiste già
     *
     * @param utenti ArrayList<Utente>
     * @param codiceFiscale String
     * @return boolean
     */
    public static boolean esisteUtenteC(ArrayList<Utente> utenti, String codiceFiscale) {
        return utenti.stream().anyMatch(utente -> utente.getCodiceFiscale().equals(codiceFiscale));
    }

    @Override
    public String toString() {
        return "Utente{" +
                "userId=" + userId +
                ", nome='" + nome + '\'' +
                ", cognome='" + cognome + '\'' +
                ", codiceFiscale='" + codiceFiscale + '\'' +
                ", email='" + email + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}
