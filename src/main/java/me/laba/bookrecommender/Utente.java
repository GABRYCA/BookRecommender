package me.laba.bookrecommender;

public class Utente {

    private int userId;
    private String nome;
    private String cognome;
    private String codiceFiscale;
    private String email;
    private String password;

    public Utente(int userId, String nome, String cognome, String codiceFiscale, String email, String password) {
        this.userId = userId;
        this.nome = nome;
        this.cognome = cognome;
        this.codiceFiscale = codiceFiscale;
        this.email = email;
        this.password = password;
    }

}
