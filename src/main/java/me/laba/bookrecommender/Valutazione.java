package me.laba.bookrecommender;

/**
 * @author Caretti Gabriele 756564 VA
 * @author Como Riccardo 758697 VA
 * @author Manicone Giorgia  758716 VA
 */
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

    public int getValutazioneId() {
        return valutazioneId;
    }

    public void setValutazioneId(int valutazioneId) {
        this.valutazioneId = valutazioneId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getLibroId() {
        return libroId;
    }

    public void setLibroId(int libroId) {
        this.libroId = libroId;
    }

    public int getStile() {
        return stile;
    }

    public void setStile(int stile) {
        this.stile = stile;
    }

    public int getContenuto() {
        return contenuto;
    }

    public void setContenuto(int contenuto) {
        this.contenuto = contenuto;
    }

    public int getGradevolezza() {
        return gradevolezza;
    }

    public void setGradevolezza(int gradevolezza) {
        this.gradevolezza = gradevolezza;
    }

    public int getOriginalita() {
        return originalita;
    }

    public void setOriginalita(int originalita) {
        this.originalita = originalita;
    }

    public int getEdizione() {
        return edizione;
    }

    public void setEdizione(int edizione) {
        this.edizione = edizione;
    }

    public int getVotoFinale() {
        return votoFinale;
    }

    public void setVotoFinale(int votoFinale) {
        this.votoFinale = votoFinale;
    }

    @Override
    public String toString() {
        return "Valutazione{" +
                "valutazioneId=" + valutazioneId +
                ", userId=" + userId +
                ", libroId=" + libroId +
                ", stile=" + stile +
                ", contenuto=" + contenuto +
                ", gradevolezza=" + gradevolezza +
                ", originalita=" + originalita +
                ", edizione=" + edizione +
                ", votoFinale=" + votoFinale +
                '}';
    }
}
