package com.quizmaster.models;

/**
 * Teacher user (formerly "Admin"). Has access to:
 *   - User management (view, delete, promote/demote)
 *   - Question management (add, delete)
 *   - Subject overview, reports & statistics
 *
 * The default teacher (ProAnds / JavaProject123) is seeded by DataStore on first run.
 */
public class Teacher extends User {

    public Teacher() {
        // For Gson deserialization.
        this.type = "TEACHER";
    }

    public Teacher(String username, String password, String firstName,
                   String lastName, String dob) {
        super(username, password, firstName, lastName, dob);
        this.type = "TEACHER";
    }

    /**
     * Copy-constructor used when PROMOTING a Student to a Teacher.
     * Preserves the user's identity and original join date so the promotion
     * is a true role change rather than a brand-new account.
     */
    public Teacher(User source) {
        this.username = source.getUsername();
        this.password = source.getPassword();
        this.firstName = source.getFirstName();
        this.lastName = source.getLastName();
        this.dob = source.getDob();
        this.createdAt = source.getCreatedAt();
        this.type = "TEACHER";
    }

    @Override
    public String getRole() {
        return "TEACHER";
    }

    @Override
    public String getRoleDisplay() {
        return "Teacher";
    }

    @Override
    public String getDashboardTitle() {
        return "Teacher Control Panel";
    }
}
