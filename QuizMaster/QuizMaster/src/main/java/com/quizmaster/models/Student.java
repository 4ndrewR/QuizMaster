package com.quizmaster.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Student user (formerly "Client"). Can:
 *   - Enroll in / drop subjects
 *   - Play quizzes (only on enrolled subjects)
 *   - View personal scores and grades
 *   - See the public leaderboard and download a PDF report
 *
 * Adds student-specific state (totalPoints, gamesPlayed, enrolledSubjects) that
 * the Teacher subclass does not have — showing how subclasses extend the base.
 */
public class Student extends User {

    private int totalPoints;
    private int gamesPlayed;
    private List<String> enrolledSubjects;

    public Student() {
        this.type = "STUDENT";
        this.enrolledSubjects = new ArrayList<>();
    }

    public Student(String username, String password, String firstName,
                   String lastName, String dob) {
        super(username, password, firstName, lastName, dob);
        this.type = "STUDENT";
        this.totalPoints = 0;
        this.gamesPlayed = 0;
        this.enrolledSubjects = new ArrayList<>();
    }

    /**
     * Copy-constructor used when DEMOTING a Teacher back to a Student.
     * Carries over identity + join date. If the source is already a Student,
     * its stats and enrollments are preserved too.
     */
    public Student(User source) {
        this.username = source.getUsername();
        this.password = source.getPassword();
        this.firstName = source.getFirstName();
        this.lastName = source.getLastName();
        this.dob = source.getDob();
        this.createdAt = source.getCreatedAt();
        this.type = "STUDENT";
        this.enrolledSubjects = new ArrayList<>();
        if (source instanceof Student s) {
            this.totalPoints = s.totalPoints;
            this.gamesPlayed = s.gamesPlayed;
            this.enrolledSubjects.addAll(s.getEnrolledSubjects());
        }
    }

    @Override
    public String getRole() {
        return "STUDENT";
    }

    @Override
    public String getRoleDisplay() {
        return "Student";
    }

    @Override
    public String getDashboardTitle() {
        return "Student Hub";
    }

    public int getTotalPoints() { return totalPoints; }
    public void setTotalPoints(int totalPoints) { this.totalPoints = totalPoints; }
    public void addPoints(int delta) { this.totalPoints += delta; }

    public int getGamesPlayed() { return gamesPlayed; }
    public void setGamesPlayed(int gamesPlayed) { this.gamesPlayed = gamesPlayed; }
    public void incrementGamesPlayed() { this.gamesPlayed++; }

    public double getAverageScore() {
        return gamesPlayed == 0 ? 0.0 : (double) totalPoints / gamesPlayed;
    }

    // ----- Subject enrollment -----

    public List<String> getEnrolledSubjects() {
        if (enrolledSubjects == null) enrolledSubjects = new ArrayList<>();
        return enrolledSubjects;
    }

    public boolean isEnrolledIn(String subject) {
        return getEnrolledSubjects().contains(subject);
    }

    /** @return true if newly enrolled, false if already enrolled. */
    public boolean enroll(String subject) {
        if (isEnrolledIn(subject)) return false;
        getEnrolledSubjects().add(subject);
        return true;
    }

    /** @return true if dropped, false if wasn't enrolled. */
    public boolean drop(String subject) {
        return getEnrolledSubjects().remove(subject);
    }
}
