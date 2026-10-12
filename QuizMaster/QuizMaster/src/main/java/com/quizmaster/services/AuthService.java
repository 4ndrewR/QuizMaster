package com.quizmaster.services;

import com.quizmaster.models.User;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Optional;

/**
 * Authentication service. Verifies credentials against DataStore and tracks
 * the currently-logged-in user for the session.
 *
 * Passwords are stored as salted BCrypt hashes (cost 12). Records saved by
 * older versions in plaintext are transparently upgraded to a hash on the
 * user's next successful login.
 */
public class AuthService {

    private static User currentUser;

    private static final int BCRYPT_COST = 12;

    /** Hash a plaintext password for storage. */
    public static String hash(String plain) {
        return BCrypt.hashpw(plain, BCrypt.gensalt(BCRYPT_COST));
    }

    private static boolean isHash(String stored) {
        return stored != null && stored.matches("^\\$2[aby]\\$\\d{2}\\$.{53}$");
    }

    /** Attempt to log in. Returns the User on success, empty on failure. */
    public static Optional<User> login(String username, String password) {
        Optional<User> found = DataStore.getInstance().findUser(username);
        if (found.isEmpty()) return Optional.empty();

        User user = found.get();
        String stored = user.getPassword();
        boolean ok;

        if (isHash(stored)) {
            ok = BCrypt.checkpw(password, stored);
        } else {
            // Legacy plaintext record from an older save file: verify, then upgrade.
            ok = stored != null && stored.equals(password);
            if (ok) {
                user.setPassword(hash(password));
                DataStore.getInstance().updateUser(user);
            }
        }

        if (!ok) return Optional.empty();
        currentUser = user;
        return Optional.of(user);
    }

    public static void logout() {
        currentUser = null;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }
}
