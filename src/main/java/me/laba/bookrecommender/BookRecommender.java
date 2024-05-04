package me.laba.bookrecommender;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import com.opencsv.exceptions.CsvMalformedLineException;
import com.opencsv.exceptions.CsvValidationException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Classe Main del progetto.
 *
 * LE SEGUENTI LINEE DEVONO ESSERE INSERITE IN OGNI FILE .java!
 * @author Caretti Gabriele 756564 VA
 * @author Como Riccardo 758697 VA
 * @author Manicone Giorgia  758716 VA
 */
public class BookRecommender {
    public static void main(String[] args) {
        System.out.println("Programma avviato!");

        /*File sorgente = new File("data\\BooksDataset.csv");
        File destinazione = new File("data\\Libri.dati.csv");

        estrattoreLibri(sorgente, destinazione);*/

        System.out.println("Programma terminato!");

    }

    public static void estrattoreLibri(File sorgente, File destinazione) {
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
    }
}