package com.quizmaster.models;

import com.google.gson.annotations.SerializedName;

import java.time.LocalDateTime;

/**
 * Record of one completed quiz attempt by a student.
 * Used for the leaderboard, personal score history, grades, and teacher reports.
 *
 * GRADING (v1.2): a grade out of 100 is derived from correct/total. A grade of
 * {@value #PASSING_GRADE} or higher is a pass. The grade is COMPUTED on demand,
 * not stored, so it stays consistent if the rule ever changes.
 *
 * The stored JSON key is "subject"; older files using "category" still load.
 */
public class ScoreRecord {

    /** Minimum grade (out of 100) required to pass. */
    public static final int PASSING_GRADE = 60;

    private String username;
    private int score;
    private int correctAnswers;
    private int totalQuestions;

    @SerializedName(value = "subject", alternate = {"category"})
    private String subject;

    private String difficulty;
    private String playedAt;

    // --- timing ---
    private int totalTimeSeconds;
    private double avgTimePerQuestion;

    public ScoreRecord() {}

    public ScoreRecord(String username, int score, int correctAnswers,
                       int totalQuestions, String subject, String difficulty,
                       int totalTimeSeconds, double avgTimePerQuestion) {
        this.username = username;
        this.score = score;
        this.correctAnswers = correctAnswers;
        this.totalQuestions = totalQuestions;
        this.subject = subject;
        this.difficulty = difficulty;
        this.totalTimeSeconds = totalTimeSeconds;
        this.avgTimePerQuestion = avgTimePerQuestion;
        this.playedAt = LocalDateTime.now().toString();
    }

    public String getUsername() { return username; }
    public int getScore() { return score; }
    public int getCorrectAnswers() { return correctAnswers; }
    public int getTotalQuestions() { return totalQuestions; }
    public String getSubject() { return subject; }
    public String getDifficulty() { return difficulty; }
    public String getPlayedAt() { return playedAt; }
    public int getTotalTimeSeconds() { return totalTimeSeconds; }
    public double getAvgTimePerQuestion() { return avgTimePerQuestion; }

    public double getPercentage() {
        return totalQuestions == 0 ? 0.0
                : (double) correctAnswers / totalQuestions * 100.0;
    }

    // ----- Grading -----

    /** Grade out of 100, rounded from the percentage of correct answers. */
    public int getGrade() {
        return (int) Math.round(getPercentage());
    }

    public boolean isPassing() {
        return getGrade() >= PASSING_GRADE;
    }

    /** "PASS" or "FAIL". */
    public String getResult() {
        return isPassing() ? "PASS" : "FAIL";
    }

    /** Letter grade: A>=90, B>=80, C>=70, D>=60, otherwise F. */
    public String getLetterGrade() {
        int g = getGrade();
        if (g >= 90) return "A";
        if (g >= 80) return "B";
        if (g >= 70) return "C";
        if (g >= 60) return "D";
        return "F";
    }

    /** Formats total time as M:SS (e.g. "1:23"). */
    public String getFormattedTime() {
        int m = totalTimeSeconds / 60;
        int s = totalTimeSeconds % 60;
        return String.format("%d:%02d", m, s);
    }
}
