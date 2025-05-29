package me.labb.bookrecommender.server.csv;

import me.labb.bookrecommender.server.oggetti.Libro;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Legge un file CSV di libri dalla cartella resources utilizzando Apache Commons CSV,
 * gestendo correttamente le virgole nei campi quotati e altre complessità del formato CSV.
 *
 * @author Caretti Gabriele 756564 VA
 * @author Como Riccardo 758697 VA
 * @author Manicone Giorgia 758716 VA
 */
public class CsvReader {

    private static final String CSV_FILE_PATH = "/BooksDatasetClean.csv";
    private static final String[] HEADERS = {
            "Title", "Authors", "Description", "Category", "Publisher",
            "Price Starting With ($)", "Publish Date (Month)", "Publish Date (Year)"
    };

    /**
     * Usiamo Apache Commons CSV.
     * Le righe che causano errori durante il parsing vengono saltate e loggate.
     *
     * @return Lista di oggetti Libro letti dal file.
     */
    public List<Libro> readLibri() {
        List<Libro> libri = new ArrayList<>();


        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(HEADERS)
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .get();

        try (InputStream is = CsvReader.class.getResourceAsStream(CSV_FILE_PATH);
             InputStreamReader isr = (is != null) ? new InputStreamReader(is, StandardCharsets.UTF_8) : null;
             CSVParser parser = (isr != null) ? csvFormat.parse(isr) : null) {

            if (parser == null) {
                System.err.println("File non trovato o impossibile da leggere nella cartella resources: " + CSV_FILE_PATH);
                return libri;
            }

            int recordNumber = 1;
            for (CSVRecord record : parser) {
                recordNumber++;
                try {
                    if (!record.isConsistent()) {
                        System.out.println("Record CSV inconsistente saltato alla linea ~" + recordNumber + ". Colonne attese: " + HEADERS.length + ", trovate: " + record.size() + ". Contenuto: " + record);
                        continue;
                    }

                    Libro libro = new Libro(
                            Integer.parseInt(record.get("LibroID")),
                            record.get("Title"),
                            record.get("Authors"),
                            record.get("Description"),
                            record.get("Category"),
                            record.get("Publisher"),
                            Float.parseFloat(record.get("Price Starting With ($)")),
                            record.get("Publish Date (Month)"),
                            Integer.parseInt(record.get("Publish Date (Year)"))
                    );
                    libri.add(libro);

                } catch (IllegalArgumentException e) {
                    System.out.println("Errore nel parsing del record CSV alla linea ~" + recordNumber + ". Record: " + record);
                    System.out.println("Eccezione: " + e.getMessage());
                } catch (ArrayIndexOutOfBoundsException e) {
                    System.out.println("Errore di indice del record CSV alla linea ~" + recordNumber + ". Record: " + record);
                    System.out.println("Eccezione: " + e.getMessage());
                } catch (Exception e) {
                    System.out.println("Errore imprevisto processando il record CSV alla linea ~" + recordNumber + ". Record: " + record);
                    System.out.println("Eccezione: " + e.getMessage());
                }
            }

        } catch (IOException e) {
            System.err.println("Errore di I/O durante la lettura o il parsing del file CSV: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("Errore di formato del file CSV: " + e.getMessage());
        } catch (NullPointerException e) {
            System.err.println("Errore critico: Impossibile localizzare il file CSV nel classpath: " + CSV_FILE_PATH);
        }
        System.out.println("Parsing CSV completato. Numero di record letti con successo: " + libri.size() + " da " + CSV_FILE_PATH);

        if (libri.isEmpty()) {
            System.out.println("Nessun libro letto dal file CSV.");
        }
        return libri;
    }
}