package me.laba.bookrecommender;

import java.util.ArrayList;

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


}
