package me.labb.bookrecommender.server.sicurezza;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Classe per gestire la sicurezza delle password.
 * Usa SHA-256 con salt per l'hashing delle password.
 */
public class PasswordManager {
    private static final int SALT_LENGTH = 16;
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final String DELIMITER = ":";
    
    /**
     * Genera un hash sicuro per una password.
     * 
     * @param password Password in chiaro da hashare
     * @return Stringa contenente salt e hash, separati da delimitatore
     */
    public static String hashPassword(String password) {
        try {
            // Genera salt casuale
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[SALT_LENGTH];
            random.nextBytes(salt);
            
            // Calcola hash
            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
            md.update(salt);
            byte[] hashedPassword = md.digest(password.getBytes());
            
            // Codifica in Base64 per archiviazione
            String saltBase64 = Base64.getEncoder().encodeToString(salt);
            String hashBase64 = Base64.getEncoder().encodeToString(hashedPassword);
            
            // Restituisci salt:hash
            return saltBase64 + DELIMITER + hashBase64;
            
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Errore durante l'hashing della password", e);
        }
    }
    
    /**
     * Verifica se una password corrisponde all'hash archiviato.
     * 
     * @param password Password in chiaro da verificare
     * @param storedHash Hash archiviato con formato salt:hash
     * @return true se la password corrisponde, false altrimenti
     */
    public static boolean verificaPassword(String password, String storedHash) {
        try {
            // Estrai salt e hash
            String[] parts = storedHash.split(DELIMITER);
            if (parts.length != 2) {
                return false;
            }
            
            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] hash = Base64.getDecoder().decode(parts[1]);
            
            // Calcola hash con lo stesso salt
            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
            md.update(salt);
            byte[] calculatedHash = md.digest(password.getBytes());
            
            // Confronta gli hash
            return MessageDigest.isEqual(hash, calculatedHash);
            
        } catch (NoSuchAlgorithmException | IllegalArgumentException e) {
            return false;
        }
    }
}
