package me.laba.bookrecommender;

import java.util.ArrayList;

public class ConsigliLibri {

    private int suggerimentoId;
    private int userId;
    private ArrayList<String> libriId;

    public ConsigliLibri(int suggerimentoId, int userId, ArrayList<String> libriId) {
        this.suggerimentoId = suggerimentoId;
        this.userId = userId;
        this.libriId = libriId;
    }

}
