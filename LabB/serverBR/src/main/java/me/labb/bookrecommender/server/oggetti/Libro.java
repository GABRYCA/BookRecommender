package me.labb.bookrecommender.server.oggetti;

/**
 * Record per rappresentare un libro letto dal file CSV.
 */
public class Libro {
    private String titolo;
    private String Autore;
    private String descrizione;
    private String categoria;
    private String editore;
    private int annoPubblicazione;

    public Libro(String titolo, String Autore, String descrizione, String categoria, String editore, int annoPubblicazione) {
        this.titolo = titolo;
        this.Autore = Autore;
        this.descrizione = descrizione;
        this.categoria = categoria;
        this.editore = editore;
        this.annoPubblicazione = annoPubblicazione;
    }

    public String getTitolo() {
        return titolo;
    }

    public void setTitolo(String titolo) {
        this.titolo = titolo;
    }

    public String getAutore() {
        return Autore;
    }

    public void setAutore(String Autore) {
        this.Autore = Autore;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getEditore() {
        return editore;
    }

    public void setEditore(String editore) {
        this.editore = editore;
    }

    public int getAnnoPubblicazione() {
        return annoPubblicazione;
    }

    public void setAnnoPubblicazione(int annoPubblicazione) {
        this.annoPubblicazione = annoPubblicazione;
    }
}