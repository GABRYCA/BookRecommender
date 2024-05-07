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
import java.util.Scanner;

/**
 * Classe Main del progetto.
 * <p>
 * LE SEGUENTI LINEE DEVONO ESSERE INSERITE IN OGNI FILE .java!
 *
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

        inizializzaDati(true);

        // CODICE QUI.
        int scelta;
        boolean loggato = false;
        Utente utenteLoggato = null;
        do {
            Scanner scanner = new Scanner(System.in);
            System.out.println("\nMenu:");
            System.out.println("0. Esci");

            // Opzioni disponibili per tutti
            System.out.println("1. Ricerca libri");
            System.out.println("2. Visualizza libro per id");

            // Login e registrazione
            if (!loggato) {
                System.out.println("3. Login");
                System.out.println("4. Registrazione");
            } else {
                System.out.println("3. Logout");
                System.out.println("4. Gestisci librerie");
                System.out.println("5. Valuta libro");
                System.out.println("6. Suggerisci libro"); // usa funzione inserisciSuggerimentoLibro() successivamente
            }
            System.out.println("Scelta: ");

            scelta = scanner.nextInt();
            switch (scelta) {
                case 1:
                    System.out.println("Inserisci tipo di ricerca (1. Titolo, 2. Autore, 3. Autore e Anno): ");
                    int tipoRicerca = scanner.nextInt();
                    TipoRicercaLibro tipo = TipoRicercaLibro.TITOLO;
                    switch (tipoRicerca) {
                        case 1:
                            tipo = TipoRicercaLibro.TITOLO;
                            break;
                        case 2:
                            tipo = TipoRicercaLibro.AUTORE;
                            break;
                        case 3:
                            tipo = TipoRicercaLibro.AUTORE_ANNO;
                            break;
                        default:
                            System.out.println("Scelta non valida!");
                            break;
                    }
                    System.out.println("Inserisci il " + tipo.toString().toLowerCase() + " da cercare: ");
                    String valore = scanner.next();
                    Optional<Integer> anno = Optional.empty();
                    if (tipo == TipoRicercaLibro.AUTORE_ANNO) {
                        System.out.println("Inserisci anno di pubblicazione: ");
                        anno = Optional.of(scanner.nextInt());
                    }
                    ArrayList<Libro> risultato = cercaLibro(tipo, valore, anno);
                    for (Libro libro : risultato) {
                        System.out.println(libro);
                    }
                    continua(scanner);
                    break;
                case 2:
                    System.out.println("Inserisci id del libro: ");
                    int libroId = scanner.nextInt();
                    for (Libro libro : libri) {
                        if (libro.getLibroId() == libroId) {
                            // Stampa informazioni del libro in modo dettagliato ed elegante + valutazioni
                            delimitatore();
                            System.out.println("Informazioni libro con id " + libroId + ":");
                            System.out.println("Titolo: " + libro.getTitolo());
                            System.out.println("Autori: " + String.join(", ", libro.getAutori()));
                            System.out.println("Anno di pubblicazione: " + libro.getAnnoPubblicazione());
                            System.out.println("Editore: " + libro.getEditore());
                            System.out.println("Categoria: " + String.join(", ", libro.getCategoria()));
                            delimitatore();
                            System.out.println("Valutazioni:");
                            for (Valutazione valutazione : valutazioni) {
                                if (valutazione.getLibroId() == libroId) {
                                    // Ottieni nome utente e stampalo
                                    for (Utente u : utenti) {
                                        if (u.getUserId() == valutazione.getUserId()) {
                                            System.out.println("Utente: " + u.getNome() + " " + u.getCognome());
                                            break;
                                        }
                                    }
                                    System.out.println("Stile: " + valutazione.getStile());
                                    System.out.println("Contenuto: " + valutazione.getContenuto());
                                    System.out.println("Gradevolezza: " + valutazione.getGradevolezza());
                                    System.out.println("Originalità: " + valutazione.getOriginalita());
                                    System.out.println("Edizione: " + valutazione.getEdizione());
                                    System.out.println("Voto finale: " + valutazione.getVotoFinale());
                                    System.out.println("Commento: " + valutazione.getCommento());
                                    System.out.println();
                                }
                            }
                            delimitatore();
                            break;
                        }
                    }
                    break;
                case 3:
                    if (!loggato) {
                        System.out.println("Inserisci email: ");
                        String email = scanner.next();
                        System.out.println("Inserisci password: ");
                        String password = scanner.next();
                        for (Utente utente : utenti) {
                            if (utente.getEmail().equals(email) && utente.getPassword().equals(password)) {
                                loggato = true;
                                utenteLoggato = utente;
                                System.out.println("Login effettuato con successo!");
                                break;
                            }
                        }
                    } else {
                        loggato = false;
                        utenteLoggato = null;
                        System.out.println("Logout effettuato con successo!");
                    }
                    break;
                case 4:
                    if (loggato) { // Gestisci librerie
                        System.out.println("1. Crea libreria");
                        System.out.println("2. Aggiungi libro alla libreria");
                        System.out.println("3. Visualizza librerie");
                        System.out.println("4. Rimuovi libreria");

                        int sceltaLibrerie = scanner.nextInt();
                        switch (sceltaLibrerie) {
                            case 1:
                                System.out.println("Inserisci nome libreria: ");
                                String nomeLibreria = scanner.next();
                                Libreria libreria = new Libreria(librerie.size() + 1, utenteLoggato.getUserId(), nomeLibreria, new ArrayList<>());
                                librerie.add(libreria);
                                System.out.println("Libreria creata con successo!");
                                break;
                            case 2:
                                System.out.println("Inserisci id della libreria: ");
                                int libreriaId = scanner.nextInt();
                                for (Libreria l : librerie) {
                                    if (l.getLibreriaId() == libreriaId) {
                                        if (l.getUserId() != utenteLoggato.getUserId()) { // Verifica se l'utente è il proprietario della libreria
                                            System.out.println("Non sei il proprietario di questa libreria!");
                                            break;
                                        }
                                        System.out.println("Inserisci id del libro da aggiungere: ");
                                        int libroIdLibreria = scanner.nextInt();
                                        boolean trovato = false;
                                        boolean giaPresente = false;
                                        for (Libro libro : libri) {
                                            if (libro.getLibroId() == libroIdLibreria) {
                                                if (l.getLibriId().contains(String.valueOf(libroIdLibreria))) { // Verifica che il libro non sia già presente
                                                    System.out.println("Libro già presente nella libreria!");
                                                    giaPresente = true;
                                                    break;
                                                }
                                                l.getLibriId().add(String.valueOf(libroIdLibreria));
                                                trovato = true;
                                                break;
                                            }
                                        }
                                        if (trovato) {
                                            System.out.println("Libro aggiunto con successo!");
                                        } else if (giaPresente) {
                                            System.out.println("Libro già presente nella libreria!");
                                        } else {
                                            System.out.println("Libro non trovato!");
                                        }
                                        continua(scanner);
                                        break;
                                    }
                                }
                                break;
                            case 3:
                                int trovato = 0;
                                for (Libreria l : librerie) {
                                    if (l.getUserId() == utenteLoggato.getUserId()) {
                                        trovato++;
                                        delimitatore();
                                        System.out.println("Le tua libreria:");
                                        System.out.println("Nome: " + l.getNomeLibreria());
                                        System.out.println("Id libreria: " + l.getLibreriaId());
                                        System.out.println("Libri:");
                                        int conta = 1;
                                        for (String libroIdLibreria : l.getLibriId()) {
                                            for (Libro libro : libri) {
                                                if (libro.getLibroId() == Integer.parseInt(libroIdLibreria)) {
                                                    System.out.println("Libro " + conta + ": " + libro.getTitolo());
                                                    System.out.println("Id: " + libro.getLibroId());
                                                    conta++;
                                                    break;
                                                }
                                            }
                                        }
                                        delimitatore();
                                    }
                                }
                                if (trovato == 0) {
                                    System.out.println("Nessuna libreria trovata!");
                                }
                                continua(scanner);
                                break;
                            case 4:
                                System.out.println("Inserisci l'id della libreria da eliminare: ");
                                int libreriaIdElim = scanner.nextInt();
                                int trovatoo = 0;

                                for (Libreria l : librerie) {
                                    if (l.getLibreriaId() == libreriaIdElim) {
                                        if (l.getUserId() != utenteLoggato.getUserId()) { // Verifica se l'utente è il proprietario della libreria
                                            System.out.println("Non sei il proprietario di questa libreria!");
                                            trovatoo++;
                                            break;
                                        }
                                        else {
                                            trovatoo++;
                                            librerie.remove(libreriaIdElim-1);
                                            System.out.println("Libreria eliminata con successo !");
                                            break;


                                        }
                                    }
                                }
                                if (trovatoo == 0) {
                                    System.out.println("Nessuna libreria trovata!");
                                }
                                continua(scanner);

                                break;
                            default:
                                System.out.println("Scelta non valida!");
                                break;
                        }
                    } else { // Registrazione
                        System.out.println("Inserisci nome: ");
                        String nome = scanner.next();
                        System.out.println("Inserisci cognome: ");
                        String cognome = scanner.next();
                        System.out.println("Inserisci codice fiscale: ");
                        String codiceFiscale = scanner.next();
                        System.out.println("Inserisci email: ");
                        String email = scanner.next();
                        System.out.println("Inserisci userId: ");
                        int userId = scanner.nextInt();
                        System.out.println("Inserisci password: ");
                        String password = scanner.next();
                        Utente utente = new Utente(userId, nome, cognome, codiceFiscale, email, password);
                        // Controlla non ci sia già un utente con lo stesso userId
                        boolean trovato = false;
                        for (Utente u : utenti) {
                            if (u.getUserId() == userId) {
                                trovato = true;
                                break;
                            }
                        }
                        if (trovato) {
                            System.out.println("Utente già registrato con questo userId!");
                            break;
                        }
                        utenti.add(utente);
                        System.out.println("Registrazione effettuata con successo! Ora puoi loggarti...");
                    }
                    break;
                case 5:
                    System.out.println("Inserisci l'id del libro: ");
                    libroId = scanner.nextInt();
                    if (libroId >= 0 && libroId <= libri.size()) {
                        if (!Libro.esisteLibro(libri, libroId)) {
                            System.out.println("Libro non trovato!");
                            break;
                        }
                        System.out.println("Inserisci la valutazione (da  1 a 5) \n");
                        System.out.println("Stile : ");
                        int stile = scanner.nextInt();
                        System.out.println("Contenuto : ");
                        int contenuto = scanner.nextInt();
                        System.out.println("Gradevolezza : ");
                        int gradevolezza = scanner.nextInt();
                        System.out.println("Originalita': ");
                        int originalita = scanner.nextInt();
                        System.out.println("Edizione : ");
                        int edizione = scanner.nextInt();
                        System.out.println("Voto finale : ");
                        int Vfinale = scanner.nextInt();
                        scanner.nextLine();
                        System.out.println("Commento : ");
                        String commento = scanner.nextLine();

                        valutazioni.add(new Valutazione(valutazioni.size() + 1, utenteLoggato.getUserId(), libroId, stile, contenuto, gradevolezza, originalita, edizione, Vfinale, commento));
                        System.out.println("\nValutazione creata con successo!");
                    }else {
                        System.out.println("\nLibro inesistente");
                    }

                    continua(scanner);
                    break;
                default:
                    System.out.println("Scelta non valida!");
                    break;
            }
        } while (scelta != 0);


        // Salvataggio alla chiusura libri
        salvataggioDati(true);

        System.out.println("Programma terminato!");
    }

    /**
     * Ricerca dei libri in base al tipo di ricerca.
     *
     * @param tipo   TipoRicercaLibro
     * @param valore String
     * @param anno   Optional<Integer>
     * @return ArrayList<Libro> risultato
     */
    public static ArrayList<Libro> cercaLibro(TipoRicercaLibro tipo, String valore, Optional<Integer> anno) {
        ArrayList<Libro> risultato = new ArrayList<>();
        switch (tipo) {
            case TITOLO:
                // Ricerca per titolo
                for (Libro libro : libri) {
                    if (libro.getTitolo().contains(valore)) {
                        risultato.add(libro);
                    }
                }
                break;
            case AUTORE:
                // Ricerca per autore
                for (Libro libro : libri) {
                    for (String autore : libro.getAutori()) {
                        if (autore.contains(valore)) {
                            risultato.add(libro);
                            break;
                        }
                    }
                }
                break;
            case AUTORE_ANNO:
                if (anno.isEmpty()) {
                    return risultato;
                }
                for (Libro libro : libri) {
                    for (String autore : libro.getAutori()) {
                        if (autore.contains(valore) && libro.getAnnoPubblicazione() == anno.get()) {
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
     * Premi un tasto per continuare
     *
     * @param scanner Scanner
     * @return void
     */
    public static void continua(Scanner scanner) {
        System.out.println("Premi un tasto per continuare...");
        scanner.nextLine();
        scanner.nextLine();
    }

    /**
     * Delimitatore terminale per stile
     *
     * @return void
     * */
    public static void delimitatore() {
        System.out.println("**************************************");
    }

    /**
     * Carica tutti i libri presenti nel file CSV.
     *
     * @return ArrayList<Libro> libri
     */
    public static ArrayList<Libro> caricaLibri() {
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
     */
    public static ArrayList<Utente> caricaUtenti() {
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
     */
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
     */
    public static ArrayList<Valutazione> caricaValutazioni() {
        ArrayList<Valutazione> valutazioni = new ArrayList<>();

        try {
            CSVReader reader = new CSVReader(new FileReader(sorgenteValutazioni));
            String[] nextLine;

            // Salto intestazione
            reader.readNext();

            nextLine = reader.readNext();
            while ((nextLine) != null) {
                Valutazione valutazione = new Valutazione(Integer.parseInt(nextLine[0]), Integer.parseInt(nextLine[1]), Integer.parseInt(nextLine[2]), Integer.parseInt(nextLine[3]), Integer.parseInt(nextLine[4]), Integer.parseInt(nextLine[5]), Integer.parseInt(nextLine[6]), Integer.parseInt(nextLine[7]), Integer.parseInt(nextLine[8]), nextLine[9]);
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
     */
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
     */
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
     */
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
     */
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
     */
    public static boolean salvataggioValutazioni() {
        try {
            CSVWriter writer = new CSVWriter(new FileWriter(sorgenteValutazioni));

            // Scrivo intestazione
            writer.writeNext(new String[]{"ValutazioneId", "UserId", "LibroId", "Stile", "Contenuto", "Gradevolezza", "Originalità", "Edizione", "VotoFinale", "Commento"});

            for (Valutazione valutazione : valutazioni) {
                String[] entries = {String.valueOf(valutazione.getValutazioneId()), String.valueOf(valutazione.getUserId()), String.valueOf(valutazione.getLibroId()), String.valueOf(valutazione.getStile()), String.valueOf(valutazione.getContenuto()), String.valueOf(valutazione.getGradevolezza()), String.valueOf(valutazione.getOriginalita()), String.valueOf(valutazione.getEdizione()), String.valueOf(valutazione.getVotoFinale()), String.valueOf(valutazione.getCommento())};
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
     */
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

    /**
     * Inizializza e carica tutti i dati dai file CSV.
     *
     * @param mostraMessaggi boolean
     * */
    private static void inizializzaDati(boolean mostraMessaggi) {
        if (mostraMessaggi) {
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
        } else {
            libri = caricaLibri();
            utenti = caricaUtenti();
            consigli = caricaConsigli();
            valutazioni = caricaValutazioni();
            librerie = caricaLibrerie();
        }
    }

    /**
     * Salva tutti i dati nei file CSV.
     *
     * @param mostraMessaggi boolean
     * */
    private static void salvataggioDati(boolean mostraMessaggi) {
        if (mostraMessaggi) {
            System.out.println("Salvataggio libri in corso...");
            if (salvataggioLibri()) {
                System.out.println("Libri salvati con successo!");
            } else {
                System.out.println("Errore durante il salvataggio dei libri!");
            }

            // Salvataggio alla chiusura utenti
            System.out.println("Salvataggio utenti in corso...");
            if (salvataggioUtenti()) {
                System.out.println("Utenti salvati con successo!");
            } else {
                System.out.println("Errore durante il salvataggio degli utenti!");
            }

            // Salvataggio alla chiusura consigli
            System.out.println("Salvataggio consigli in corso...");
            if (salvataggioConsigli()) {
                System.out.println("Consigli salvati con successo!");
            } else {
                System.out.println("Errore durante il salvataggio dei consigli!");
            }

            // Salvataggio alla chiusura valutazioni
            System.out.println("Salvataggio valutazioni in corso...");
            if (salvataggioValutazioni()) {
                System.out.println("Valutazioni salvate con successo!");
            } else {
                System.out.println("Errore durante il salvataggio delle valutazioni!");
            }

            // Salvataggio alla chiusura librerie
            System.out.println("Salvataggio librerie in corso...");
            if (salvataggioLibrerie()) {
                System.out.println("Librerie salvate con successo!");
            } else {
                System.out.println("Errore durante il salvataggio delle librerie!");
            }
        } else {
            salvataggioLibri();
            salvataggioUtenti();
            salvataggioConsigli();
            salvataggioValutazioni();
            salvataggioLibrerie();
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

