package me.laba.bookrecommender;

import java.util.ArrayList;

/**
 * @author Caretti Gabriele 756564 VA
 * @author Como Riccardo 758697 VA
 * @author Manicone Giorgia 758716 VA
 */
public class Libro {

    private int libroId;
    private String titolo;
    private ArrayList<String> autori;
    private int annoPubblicazione;
    private String editore;
    private ArrayList<String> categoria;

    /**
     * Costruttore classe Libro
     *
     * @param categoria ArrayList<String>
     * @param editore String
     * @param annoPubblicazione int
     * @param autori ArrayList<String>
     * @param titolo String
     * @param libroId int
     */
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

    /**
     * Stampa lista completa dei libri
     *
     * @return void
     */
    public static void stampaLibri(ArrayList<Libro> libri) {
        for (Libro libro : libri) {
            System.out.println(libro);
        }
    }

    /**
     * Ritorna se esiste il libro con l'id passato
     *
     * @param libri ArrayList<Libro>
     * @param libroId int
     *
     * @return boolean
     * */
    public static boolean esisteLibro(ArrayList<Libro> libri, int libroId) {
        for (Libro libro : libri) {
            if (libro.getLibroId() == libroId) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        String categorieString;
        String autoriString;
        if (categoria.toString().equals("[]")) {
            categorieString = "\033[1;31mNessuna categoria";
        } else {
            categorieString = categoria.toString().substring(1, categoria.toString().length() - 1);
        }
        if (autori.toString().equals("[]")) {
            autoriString = "\033[1;31mNessun autore";
        } else {
            autoriString = autori.toString().substring(1, autori.toString().length() - 1);
        }
        return "\033[1;36mLibro\033[0m" +
                "\n\033[1;33m🔹 libroId\033[0m=" + libroId +
                ",\n\033[1;33m🔹 titolo\033[0m='" + titolo + '\'' +
                ",\n\033[1;33m🔹 autori\033[0m=" + autoriString +
                ",\n\033[1;33m🔹 annoPubblicazione\033[0m=" + annoPubblicazione +
                ",\n\033[1;33m🔹 editore\033[0m='" + editore + '\'' +
                ",\n\033[1;33m🔹 categoria\033[0m=" + categorieString +
                "\n";
    }
}
