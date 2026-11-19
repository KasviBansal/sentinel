package com.sentinel.service;

import com.sentinel.dao.UserDAO;
import com.sentinel.database.Database;
import java.security.*;
import java.util.Arrays;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class AuthService {
    public static final String DEMO_EMAIL = "admin@sentinel.demo";

    private final Database database;
    private final UserDAO users;

    public AuthService(Database database, UserDAO users) {
        this.database = database;
        this.users = users;
    }

    public boolean login(String email, String password) {
        if (email.isBlank() || password.isBlank()) return false;
        var credentials = database.read(connection -> users.findByEmail(connection, email.strip()));
        return credentials != null && MessageDigest.isEqual(credentials.hash(), hash(password, credentials.salt()));
    }

    public static UserDAO.Credentials createCredentials(String password) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return new UserDAO.Credentials(hash(password, salt), salt);
    }

    private static byte[] hash(String password, byte[] salt) {
        char[] characters = password.toCharArray();
        PBEKeySpec spec = new PBEKeySpec(characters, salt, 210_000, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException error) {
            throw new IllegalStateException("Could not calculate password hash", error);
        } finally {
            spec.clearPassword();
            Arrays.fill(characters, '\0');
        }
    }
}
