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
     * Metodo che verifica se un utente esiste
     *
     * @param utenti ArrayList<Utente>
     * @param userId int
     * @return boolean
     */
    public static boolean esisteUtente(ArrayList<Utente> utenti, int userId){
        for (Utente u : utenti) {
            if (u.getUserId() == userId) {
                return true;
            }
        }
        return false;
    }

    /**
     * Metodo che verifica se un utente esiste per email
     *
     * @param utenti ArrayList<Utente>
     * @param email String
     * @return boolean
     */
    public static boolean esisteUtente(ArrayList<Utente> utenti, String email) {
        for (Utente u : utenti) {
            if (u.getEmail().equals(email)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Metodo che verifica se un utente esiste per email e password
     *
     * @param utenti ArrayList<Utente>
     * @param email String
     * @return boolean
     */
    public static boolean esisteUtente(ArrayList<Utente> utenti, String email, String password) {
        for (Utente u : utenti) {
            if (u.getEmail().equals(email) && u.getPassword().equals(password)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Login di un utente, ritorna l'utente se esiste, altrimenti null
     *
     * @param utenti ArrayList<Utente>
     * @param email String
     * @param password String
     * @return Utente
     */
    public static Utente loginUtente(ArrayList<Utente> utenti, String email, String password) {
        for (Utente u : utenti) {
            if (u.getEmail().equals(email) && u.getPassword().equals(password)) {
                return u;
            }
        }
        return null;
    }

    /**
     * Metodo per prendere un utente per id, se esiste
     *
     * @param utenti ArrayList<Utente>
     * @param userId int
     * @return Utente
     */
    public static Utente getUtenteById(ArrayList<Utente> utenti, int userId) {
        for (Utente u : utenti) {
            if (u.getUserId() == userId) {
                return u;
            }
        }
        return null;
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
