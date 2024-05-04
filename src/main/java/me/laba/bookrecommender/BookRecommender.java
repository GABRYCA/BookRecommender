package me.laba.bookrecommender;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvMalformedLineException;
import com.opencsv.exceptions.CsvValidationException;
import me.laba.bookrecommender.enumeratori.TipoRicercaLibro;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

/**
 * Classe Main del progetto.
 *
 * LE SEGUENTI LINEE DEVONO ESSERE INSERITE IN OGNI FILE .java!
 * @author Caretti Gabriele 756564 VA
 * @author Como Riccardo 758697 VA
 * @author Manicone Giorgia  758716 VA
 */
public class BookRecommender {

    public static File sorgenteLibri = new File("data\\Libri.dati.csv");
    public static File sorgenteUtenti = new File("data\\UtentiRegistrati.dati.csv");
    public static File sorgenteConsigli = new File("data\\ConsigliLibri.dati.csv");
    public static File sorgenteValutazioni = new File("data\\ValutazioneLibri.dati.csv");
    public static File sorgenteLibrerie = new File("data\\Librerie.dati.csv");
    public static ArrayList<Libro> libri = new ArrayList<>();
    public static ArrayList<Utente> utenti = new ArrayList<>();
    public static ArrayList<ConsigliLibri> consigli = new ArrayList<>();
    public static ArrayList<Valutazione> valutazioni = new ArrayList<>();
    public static ArrayList<Libreria> librerie = new ArrayList<>();

    public static void main(String[] args) {
        System.out.println("Programma avviato!");

        /*File sorgente = new File("data\\BooksDataset.csv");
        File destinazione = new File("data\\Libri.dati.csv");
        estrattoreLibri(sorgente, destinazione);*/

        System.out.println("Caricamento libri in corso...");
        libri = caricaLibri();
        System.out.println("Libri caricati con successo!");

        System.out.println("Caricamento utenti in corso...");
        utenti = caricaUtenti();
        System.out.println("Utenti caricati con successo!");

        System.out.println("Caricamento consigli in corso...");
        consigli = caricaConsigli();
        System.out.println("Consigli caricati con successo!");

        System.out.println("Caricamento valutazioni in corso...");
        valutazioni = caricaValutazioni();
        System.out.println("Valutazioni caricati con successo!");

        System.out.println("Caricamento librerie in corso...");
        librerie = caricaLibrerie();
        System.out.println("Librerie caricate con successo!");

        // CODICE QUI.



        // Salvataggio alla chiusura libri
        System.out.println("Salvataggio libri in corso...");
        if (salvataggioLibri()){
            System.out.println("Libri salvati con successo!");
        } else {
            System.out.println("Errore durante il salvataggio dei libri!");
        }

        // Salvataggio alla chiusura utenti
        System.out.println("Salvataggio utenti in corso...");
        if (salvataggioUtenti()){
            System.out.println("Utenti salvati con successo!");
        } else {
            System.out.println("Errore durante il salvataggio degli utenti!");
        }

        // Salvataggio alla chiusura consigli
        System.out.println("Salvataggio consigli in corso...");
        if (salvataggioConsigli()){
            System.out.println("Consigli salvati con successo!");
        } else {
            System.out.println("Errore durante il salvataggio dei consigli!");
        }

        // Salvataggio alla chiusura valutazioni
        System.out.println("Salvataggio valutazioni in corso...");
        if (salvataggioValutazioni()){
            System.out.println("Valutazioni salvate con successo!");
        } else {
            System.out.println("Errore durante il salvataggio delle valutazioni!");
        }

        // Salvataggio alla chiusura librerie
        System.out.println("Salvataggio librerie in corso...");
        if (salvataggioLibrerie()){
            System.out.println("Librerie salvate con successo!");
        } else {
            System.out.println("Errore durante il salvataggio delle librerie!");
        }

        System.out.println("Programma terminato!");
    }

    /**
     * Ricerca dei libri in base al tipo di ricerca.
     *
     * @param tipo TipoRicercaLibro
     * @param valore String
     * @param anno Optional<Integer>
     *
     * @return ArrayList<Libro> risultato
     * */
    public static ArrayList<Libro> ricercaPerTipo(TipoRicercaLibro tipo, String valore, Optional<Integer> anno){
        ArrayList<Libro> risultato = new ArrayList<>();
        switch (tipo){
            case TITOLO:
                // Ricerca per titolo
                for (Libro libro : libri){
                    if (libro.getTitolo().contains(valore)){
                        risultato.add(libro);
                    }
                }
                break;
            case AUTORE:
                // Ricerca per autore
                for (Libro libro : libri){
                    for (String autore : libro.getAutori()){
                        if (autore.contains(valore)){
                            risultato.add(libro);
                            break;
                        }
                    }
                }
                break;
            case AUTORE_ANNO:
                if (anno.isEmpty()){
                    return risultato;
                }
                for (Libro libro : libri){
                    for (String autore : libro.getAutori()){
                        if (autore.contains(valore) && libro.getAnnoPubblicazione() == anno.get()){
                            risultato.add(libro);
                            break;
                        }
                    }
                }
                break;
        }
        return risultato;
    }

    /**
     * Carica tutti i libri presenti nel file CSV.
     *
     * @return ArrayList<Libro> libri
     * */
    public static ArrayList<Libro> caricaLibri(){
        ArrayList<Libro> libri = new ArrayList<>();
        try {
            CSVReader reader = new CSVReader(new FileReader(sorgenteLibri));
            String[] nextLine;

            // Salto intestazione
            reader.readNext();

            nextLine = reader.readNext();
            while ((nextLine) != null) {
                ArrayList<String> autori = new ArrayList<>(Arrays.asList(nextLine[2].split(",")));
                ArrayList<String> categoria = new ArrayList<>(Arrays.asList(nextLine[5].split(",")));
                Libro libro = new Libro(categoria, nextLine[4], Integer.parseInt(nextLine[3]), autori, nextLine[1], Integer.parseInt(nextLine[0]));
                libri.add(libro);
                try {
                    nextLine = reader.readNext();
                } catch (CsvMalformedLineException e) {
                    // Skippa linea malformata
                    nextLine = reader.readNext();
                }
            }

            reader.close();

        } catch (IOException | CsvValidationException e) {
            e.printStackTrace();
        }

        return libri;
    }

    /**
     * Carica tutti gli utenti presenti nel file CSV.
     *
     * @return ArrayList<Utente> utenti
     * */
    public static ArrayList<Utente> caricaUtenti(){
        ArrayList<Utente> utenti = new ArrayList<>();

        try {
            CSVReader reader = new CSVReader(new FileReader(sorgenteUtenti));
            String[] nextLine;

            // Salto intestazione
            reader.readNext();

            nextLine = reader.readNext();
            while ((nextLine) != null) {
                Utente utente = new Utente(Integer.parseInt(nextLine[0]), nextLine[1], nextLine[2], nextLine[3], nextLine[4], nextLine[5]);
                utenti.add(utente);
                try {
                    nextLine = reader.readNext();
                } catch (CsvMalformedLineException e) {
                    // Skippa linea malformata
                    nextLine = reader.readNext();
                }
            }

            reader.close();

        } catch (IOException | CsvValidationException e) {
            e.printStackTrace();
        }

        return utenti;
    }

    /**
     * Carica i consigli presenti nel file CSV.
     *
     * @return ArrayList<ConsigliLibri> consigli
     * */
    public static ArrayList<ConsigliLibri> caricaConsigli() {
        ArrayList<ConsigliLibri> consigli = new ArrayList<>();

        try {
            CSVReader reader = new CSVReader(new FileReader(sorgenteConsigli));
            String[] nextLine;

            // Salto intestazione
            reader.readNext();

            nextLine = reader.readNext();
            while ((nextLine) != null) {
                ArrayList<String> libriId = new ArrayList<>(Arrays.asList(nextLine[2].split(",")));
                ConsigliLibri consiglio = new ConsigliLibri(Integer.parseInt(nextLine[0]), Integer.parseInt(nextLine[1]), libriId);
                consigli.add(consiglio);
                try {
                    nextLine = reader.readNext();
                } catch (CsvMalformedLineException e) {
                    // Skippa linea malformata
                    nextLine = reader.readNext();
                }
            }

            reader.close();

        } catch (IOException | CsvValidationException e) {
            e.printStackTrace();
        }

        return consigli;
    }

    /**
     * Carica valutazioni dei libri
     *
     * @return ArrayList<Valutazione> valutazioni
     * */
    public static ArrayList<Valutazione> caricaValutazioni() {
        ArrayList<Valutazione> valutazioni = new ArrayList<>();

        // Esempio struttura
        /*
        ValutazioneId,UserId,LibroId,Stile,Contenuto,Gradevolezza,Originalità,Edizione,VotoFinale
        1,1,1,4,3,4,3,4,3
         */

        try {
            CSVReader reader = new CSVReader(new FileReader(sorgenteValutazioni));
            String[] nextLine;

            // Salto intestazione
            reader.readNext();

            nextLine = reader.readNext();
            while ((nextLine) != null) {
                Valutazione valutazione = new Valutazione(Integer.parseInt(nextLine[0]), Integer.parseInt(nextLine[1]), Integer.parseInt(nextLine[2]), Integer.parseInt(nextLine[3]), Integer.parseInt(nextLine[4]), Integer.parseInt(nextLine[5]), Integer.parseInt(nextLine[6]), Integer.parseInt(nextLine[7]), Integer.parseInt(nextLine[8]));
                valutazioni.add(valutazione);
                try {
                    nextLine = reader.readNext();
                } catch (CsvMalformedLineException e) {
                    // Skippa linea malformata
                    nextLine = reader.readNext();
                }
            }

            reader.close();
        } catch (IOException | CsvValidationException e) {
            e.printStackTrace();
        }

        return valutazioni;
    }

    /**
     * Carica librerie dal file CSV
     *
     * @return ArrayList<Libreria> librerie
     * */
    public static ArrayList<Libreria> caricaLibrerie() {
        ArrayList<Libreria> librerie = new ArrayList<>();

        try {
            CSVReader reader = new CSVReader(new FileReader(sorgenteLibrerie));
            String[] nextLine;

            // Salto intestazione
            reader.readNext();

            nextLine = reader.readNext();
            while ((nextLine) != null) {
                ArrayList<String> libriId = new ArrayList<>(Arrays.asList(nextLine[3].split(",")));
                Libreria libreria = new Libreria(Integer.parseInt(nextLine[0]), Integer.parseInt(nextLine[1]), nextLine[2], libriId);
                librerie.add(libreria);
                try {
                    nextLine = reader.readNext();
                } catch (CsvMalformedLineException e) {
                    // Skippa linea malformata
                    nextLine = reader.readNext();
                }
            }

            reader.close();

        } catch (IOException | CsvValidationException e) {
            e.printStackTrace();
        }

        return librerie;
    }

    /**
     * Salva i libri nel file CSV
     *
     * @return boolean
     * */
    public static boolean salvataggioLibri() {
        try {
            CSVWriter writer = new CSVWriter(new FileWriter(sorgenteLibri));

            // Scrivo intestazione
            writer.writeNext(new String[]{"LibroId", "Titolo", "Autori", "Anno di pubblicazione", "Editore", "Categoria"});

            for (Libro libro : libri) {
                String[] entries = {String.valueOf(libro.getLibroId()), libro.getTitolo(), String.join(",", libro.getAutori()), String.valueOf(libro.getAnnoPubblicazione()), libro.getEditore(), String.join(",", libro.getCategoria())};
                writer.writeNext(entries);
            }

            writer.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Salva gli utenti nel file CSV
     *
     * @return boolean
     * */
    public static boolean salvataggioUtenti() {
        try {
            CSVWriter writer = new CSVWriter(new FileWriter(sorgenteUtenti));

            // Scrivo intestazione
            writer.writeNext(new String[]{"UserId", "Nome", "Cognome", "Codice Fiscale", "Email", "Password"});

            for (Utente utente : utenti) {
                String[] entries = {String.valueOf(utente.getUserId()), utente.getNome(), utente.getCognome(), utente.getCodiceFiscale(), utente.getEmail(), utente.getPassword()};
                writer.writeNext(entries);
            }

            writer.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Salva i consigli nel file CSV
     *
     * @return boolean
     * */
    public static boolean salvataggioConsigli() {
        try {
            CSVWriter writer = new CSVWriter(new FileWriter(sorgenteConsigli));

            // Scrivo intestazione
            writer.writeNext(new String[]{"SuggerimentoId", "UserId", "LibriId"});

            for (ConsigliLibri consiglio : consigli) {
                String[] entries = {String.valueOf(consiglio.getSuggerimentoId()), String.valueOf(consiglio.getUserId()), String.join(",", consiglio.getLibriId())};
                writer.writeNext(entries);
            }

            writer.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Salva le valutazioni nel file CSV
     *
     * @return boolean
     * */
    public static boolean salvataggioValutazioni() {
        try {
            CSVWriter writer = new CSVWriter(new FileWriter(sorgenteValutazioni));

            // Scrivo intestazione
            writer.writeNext(new String[]{"ValutazioneId", "UserId", "LibroId", "Stile", "Contenuto", "Gradevolezza", "Originalità", "Edizione", "VotoFinale"});

            for (Valutazione valutazione : valutazioni) {
                String[] entries = {String.valueOf(valutazione.getValutazioneId()), String.valueOf(valutazione.getUserId()), String.valueOf(valutazione.getLibroId()), String.valueOf(valutazione.getStile()), String.valueOf(valutazione.getContenuto()), String.valueOf(valutazione.getGradevolezza()), String.valueOf(valutazione.getOriginalita()), String.valueOf(valutazione.getEdizione()), String.valueOf(valutazione.getVotoFinale())};
                writer.writeNext(entries);
            }

            writer.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Salva le librerie nel file CSV
     *
     * @return boolean
     * */
    public static boolean salvataggioLibrerie() {
        try {
            CSVWriter writer = new CSVWriter(new FileWriter(sorgenteLibrerie));

            // Scrivo intestazione
            writer.writeNext(new String[]{"LibreriaId", "UserId", "NomeLibreria", "LibriId"});

            for (Libreria libreria : librerie) {
                String[] entries = {String.valueOf(libreria.getLibreriaId()), String.valueOf(libreria.getUserId()), libreria.getNomeLibreria(), String.join(",", libreria.getLibriId())};
                writer.writeNext(entries);
            }

            writer.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    /*public static void estrattoreLibri(File sorgente, File destinazione) {
        try {
            CSVReader reader = new CSVReader(new FileReader(sorgente));
            CSVWriter writer = new CSVWriter(new FileWriter(destinazione, true)); // Aggiunto 'true' per abilitare l'append al file

            String[] nextLine;
            int libroId = 1;

            reader.readNext();

            // Per evitare di sovrascrivere la prima riga, Ricordarsi!
            writer.writeNext(new String[]{});

            nextLine = reader.readNext();
            while ((nextLine) != null) {

                ArrayList<String> autori = new ArrayList<>(Arrays.asList(nextLine[1].split(",")));
                ArrayList<String> categoria = new ArrayList<>(Arrays.asList(nextLine[3].split(",")));
                String[] pezzi = nextLine[5].split(",");
                int anno = 0;
                if (pezzi.length == 3){
                    anno = Integer.parseInt(pezzi[2].trim());
                } else if (pezzi.length == 1) {
                    pezzi = pezzi[0].split(" ");
                    anno = Integer.parseInt(pezzi[1].trim());
                }

                Libro libro = new Libro(categoria, nextLine[4], anno, autori, nextLine[0], libroId++);

                String[] entries = {String.valueOf(libro.getLibroId()), libro.getTitolo(), String.join(",", libro.getAutori()), String.valueOf(libro.getAnnoPubblicazione()), libro.getEditore(), String.join(",", libro.getCategoria())};
                writer.writeNext(entries);

                try {
                    nextLine = reader.readNext();
                } catch (CsvMalformedLineException e) {
                    // Skippa linea malformata
                    nextLine = reader.readNext();
                }
            }

            reader.close();
            writer.close();

        } catch (IOException | CsvValidationException e) {
            e.printStackTrace();
        }
    }*/
}

