package com.quizmaster.models;

import java.time.LocalDateTime;

/**
 * Abstract base class for all users in the system.
 *
 * Demonstrates INHERITANCE: Teacher and Student both extend User.
 * Demonstrates POLYMORPHISM: getRole(), getRoleDisplay(), and getDashboardTitle()
 * are abstract and behave differently depending on the runtime subtype.
 *
 * The 'type' field is the discriminator Gson uses to know which subclass to
 * deserialize into (see DataStore's custom UserDeserializer).
 */
public abstract class User {

    protected String username;
    protected String password; // BCrypt hash (see AuthService)
    protected String firstName;
    protected String lastName;
    protected String dob; // ISO format: YYYY-MM-DD
    protected String createdAt;
    protected String type; // discriminator for JSON deserialization ("TEACHER" or "STUDENT")

    protected User() {
        // Default constructor needed for Gson deserialization.
    }

    protected User(String username, String password, String firstName,
                   String lastName, String dob) {
        this.username = username;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dob = dob;
        this.createdAt = LocalDateTime.now().toString();
    }

    // ----- Abstract methods - subclasses MUST implement these (polymorphism) -----

    /** @return the role identifier, e.g. "TEACHER" or "STUDENT". */
    public abstract String getRole();

    /** @return a human-friendly role label shown in the UI. */
    public abstract String getRoleDisplay();

    /** @return the title shown in the user's dashboard header. */
    public abstract String getDashboardTitle();

    // ----- Common getters/setters -----

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getDob() { return dob; }
    public void setDob(String dob) { this.dob = dob; }

    public String getCreatedAt() { return createdAt; }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    @Override
    public String toString() {
        return getFullName() + " (" + username + ")";
    }
}
