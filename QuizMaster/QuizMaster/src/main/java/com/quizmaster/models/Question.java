package com.quizmaster.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.UUID;

/**
 * One multiple-choice quiz question.
 *
 * Each question has 4 options, one of which is correct (correctIndex).
 * Difficulty determines points awarded: EASY=10, MEDIUM=20, HARD=30.
 *
 * The stored JSON key is "subject". @SerializedName keeps older save files that
 * used "category" loadable without data loss.
 */
public class Question {

    public enum Difficulty {
        EASY(10), MEDIUM(20), HARD(30);

        private final int points;
        Difficulty(int points) { this.points = points; }
        public int getPoints() { return points; }
    }

    private String id;
    private String text;
    private List<String> options;
    private int correctIndex;

    @SerializedName(value = "subject", alternate = {"category"})
    private String subject;

    private Difficulty difficulty;

    public Question() {}

    public Question(String text, List<String> options, int correctIndex,
                    String subject, Difficulty difficulty) {
        this.id = UUID.randomUUID().toString();
        this.text = text;
        this.options = options;
        this.correctIndex = correctIndex;
        this.subject = subject;
        this.difficulty = difficulty;
    }

    public String getId() { return id; }
    public String getText() { return text; }
    public List<String> getOptions() { return options; }
    public int getCorrectIndex() { return correctIndex; }
    public String getSubject() { return subject; }
    public Difficulty getDifficulty() { return difficulty; }

    public boolean isCorrect(int selectedIndex) {
        return selectedIndex == correctIndex;
    }

    public int getPoints() {
        return difficulty.getPoints();
    }
}
