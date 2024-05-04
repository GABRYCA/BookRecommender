package me.laba.bookrecommender;

public class Valutazione {

    private int valutazioneId;
    private int userId;
    private int libroId;
    private int stile;
    private int contenuto;
    private int gradevolezza;
    private int originalita;
    private int edizione;
    private int votoFinale;

    public Valutazione(int valutazioneId, int userId, int libroId, int stile, int contenuto, int gradevolezza, int originalita, int edizione, int votoFinale) {
        this.valutazioneId = valutazioneId;
        this.userId = userId;
        this.libroId = libroId;
        this.stile = stile;
        this.contenuto = contenuto;
        this.gradevolezza = gradevolezza;
        this.originalita = originalita;
        this.edizione = edizione;
        this.votoFinale = votoFinale;
    }
}
