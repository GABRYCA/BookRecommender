package me.labb.bookrecommender.server.oggetti;

import java.time.ZonedDateTime;

/**
 * Record per rappresentare una libreria personale di un utente.
 * Ogni utente può creare multiple librerie per organizzare i propri libri.
 * 
 * @param libreriaID Identificativo univoco della libreria
 * @param userID Identificativo dell'utente proprietario della libreria
 * @param nomeLibreria Nome assegnato alla libreria dall'utente
 * @param dataCreazione Data e ora di creazione della libreria
 * 
 * @author Caretti Gabriele 756564 VA
 * @author Como Riccardo 758697 VA
 * @author Manicone Giorgia 758716 VA
 */
public record Libreria (
        int libreriaID,
        int userID,
        String nomeLibreria,
        ZonedDateTime dataCreazione
) {}
