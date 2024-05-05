package me.laba.bookrecommender;

import java.util.ArrayList;

/**
 * @author Caretti Gabriele 756564 VA
 * @author Como Riccardo 758697 VA
 * @author Manicone Giorgia 758716 VA
 */
public class Libreria {

    private int libreriaId;
    private int userId;
    private String nomeLibreria;
    private ArrayList<String> libriId;

    public Libreria(int libreriaId, int userId, String nomeLibreria, ArrayList<String> libriId) {
        this.libreriaId = libreriaId;
        this.userId = userId;
        this.nomeLibreria = nomeLibreria;
        this.libriId = libriId;
    }

    public int getLibreriaId() {
        return libreriaId;
    }

    public void setLibreriaId(int libreriaId) {
        this.libreriaId = libreriaId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getNomeLibreria() {
        return nomeLibreria;
    }

    public void setNomeLibreria(String nomeLibreria) {
        this.nomeLibreria = nomeLibreria;
    }

    public ArrayList<String> getLibriId() {
        return libriId;
    }

    public void setLibriId(ArrayList<String> libriId) {
        this.libriId = libriId;
    }

    @Override
    public String toString() {
        return "Libreria{" +
                "libreriaId=" + libreriaId +
                ", userId=" + userId +
                ", nomeLibreria='" + nomeLibreria + '\'' +
                ", libriId=" + libriId +
                '}';
    }
}
