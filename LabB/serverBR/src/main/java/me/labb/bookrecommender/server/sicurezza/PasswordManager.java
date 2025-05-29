package me.labb.bookrecommender.server.sicurezza;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Classe per gestione delle password (hashing in particolare).
 * Usiamo SHA-256 con salt per l'hashing delle password.
 *
 * @author Caretti Gabriele 756564 VA
 * @author Como Riccardo 758697 VA
 * @author Manicone Giorgia 758716 VA
 */
public class PasswordManager {
    private static final int SALT_LENGTH = 16;
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final String DELIMITER = ":";

    /**
     * Genera l'hash di una password.
     *
     * @param password Password in chiaro da farne hash
     * @return Stringa contenente salt e hash, separati da delimitatore
     */
    public static String hashPassword(String password) {
        try {
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[SALT_LENGTH];
            random.nextBytes(salt);

            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
            md.update(salt);
            byte[] hashedPassword = md.digest(password.getBytes());

            String saltBase64 = Base64.getEncoder().encodeToString(salt);
            String hashBase64 = Base64.getEncoder().encodeToString(hashedPassword);

            return saltBase64 + DELIMITER + hashBase64;

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Errore durante l'hashing della password", e);
        }
    }

    /**
     * Verifica se la password è corretta (se corrisponde a un hash).
     *
     * @param password Password in chiaro da verificare
     * @param storedHash Hash archiviato con formato salt:hash
     * @return true se la password corrisponde, false altrimenti
     */
    public static boolean verificaPassword(String password, String storedHash) {
        try {
            String[] parts = storedHash.split(DELIMITER);
            if (parts.length != 2) {
                return false;
            }

            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] hash = Base64.getDecoder().decode(parts[1]);

            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
            md.update(salt);
            byte[] calculatedHash = md.digest(password.getBytes());

            return MessageDigest.isEqual(hash, calculatedHash);

        } catch (NoSuchAlgorithmException | IllegalArgumentException e) {
            return false;
        }
    }
}
