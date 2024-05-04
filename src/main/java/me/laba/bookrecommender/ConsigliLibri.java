package me.laba.bookrecommender;

import java.util.ArrayList;

/**
 * @author Caretti Gabriele 756564 VA
 * @author Como Riccardo 758697 VA
 * @author Manicone Giorgia  758716 VA
 */
public class ConsigliLibri {

    private int suggerimentoId;
    private int userId;
    private ArrayList<String> libriId;

    public ConsigliLibri(int suggerimentoId, int userId, ArrayList<String> libriId) {
        this.suggerimentoId = suggerimentoId;
        this.userId = userId;
        this.libriId = libriId;
    }

    public int getSuggerimentoId() {
        return suggerimentoId;
    }

    public void setSuggerimentoId(int suggerimentoId) {
        this.suggerimentoId = suggerimentoId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public ArrayList<String> getLibriId() {
        return libriId;
    }

    public void setLibriId(ArrayList<String> libriId) {
        this.libriId = libriId;
    }

    @Override
    public String toString() {
        return "ConsigliLibri{" +
                "suggerimentoId=" + suggerimentoId +
                ", userId=" + userId +
                ", libriId=" + libriId +
                '}';
    }
}
