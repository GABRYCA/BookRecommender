package me.laba.bookrecommender;

import java.util.ArrayList;

/**
 * @author Caretti Gabriele 756564 VA
 * @author Como Riccardo 758697 VA
 * @author Manicone Giorgia  758716 VA
 */
public class Libro {

    private int libroId;
    private String titolo;
    private ArrayList<String> autori;
    private int annoPubblicazione;
    private String editore;
    private ArrayList<String> categoria;

    public Libro(ArrayList<String> categoria, String editore, int annoPubblicazione, ArrayList<String> autori, String titolo, int libroId) {
        this.categoria = categoria;
        this.editore = editore;
        this.annoPubblicazione = annoPubblicazione;
        this.autori = autori;
        this.titolo = titolo;
        this.libroId = libroId;
    }

    public int getLibroId() {
        return libroId;
    }

    public void setLibroId(int libroId) {
        this.libroId = libroId;
    }

    public String getTitolo() {
        return titolo;
    }

    public void setTitolo(String titolo) {
        this.titolo = titolo;
    }

    public ArrayList<String> getAutori() {
        return autori;
    }

    public void setAutori(ArrayList<String> autori) {
        this.autori = autori;
    }

    public int getAnnoPubblicazione() {
        return annoPubblicazione;
    }

    public void setAnnoPubblicazione(int annoPubblicazione) {
        this.annoPubblicazione = annoPubblicazione;
    }

    public String getEditore() {
        return editore;
    }

    public void setEditore(String editore) {
        this.editore = editore;
    }

    public ArrayList<String> getCategoria() {
        return categoria;
    }

    public void setCategoria(ArrayList<String> categoria) {
        this.categoria = categoria;
    }

    @Override
    public String toString() {
        return "Libro{" +
                "libroId=" + libroId +
                ", titolo='" + titolo + '\'' +
                ", autori=" + autori +
                ", annoPubblicazione=" + annoPubblicazione +
                ", editore='" + editore + '\'' +
                ", categoria=" + categoria +
                '}';
    }
}
