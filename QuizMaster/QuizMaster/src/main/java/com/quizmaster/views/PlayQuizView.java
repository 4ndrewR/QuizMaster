package com.quizmaster.views;

import com.quizmaster.models.Question;
import com.quizmaster.models.ScoreRecord;
import com.quizmaster.models.Student;
import com.quizmaster.services.AuthService;
import com.quizmaster.services.DataStore;
import com.quizmaster.services.SoundService;
import com.quizmaster.utils.SceneManager;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * Quiz gameplay screen.
 *
 * Phases:
 *  0. Gate    - a student must be enrolled in at least one subject to play.
 *  1. Setup   - pick an enrolled subject (or "All Enrolled"), difficulty, count.
 *  2. Play    - one question at a time with live timers, a progress bar, a
 *               streak counter, and a two-stage Submit -> Next flow that reveals
 *               the answer with colour + a sound effect.
 *  3. Results - score, GRADE out of 100 with letter + PASS/FAIL, timing summary,
 *               best streak, personal-best flag, and a review of wrong answers.
 */
public class PlayQuizView implements SceneManager.View {

    private final BorderPane root;

    // ---- game state ----
    private List<Question> questions = new ArrayList<>();
    private int currentIndex = 0;
    private int correctCount = 0;
    private int totalPoints = 0;
    private int currentStreak = 0;
    private int bestStreak = 0;
    private String chosenSubject;
    private String chosenDifficulty;

    // ---- timing ----
    private long quizStartMillis;
    private long questionStartMillis;
    private final List<Long> perQuestionMillis = new ArrayList<>();
    private Timeline timer;
    private boolean answerRevealed = false;

    // ---- live labels ----
    private Label overallTimerLabel;
    private Label questionTimerLabel;

    // ---- review data ----
    private final List<AnsweredQuestion> answered = new ArrayList<>();

    private static class AnsweredQuestion {
        final Question question;
        final int pickedIndex;
        final boolean correct;
        AnsweredQuestion(Question question, int pickedIndex, boolean correct) {
            this.question = question;
            this.pickedIndex = pickedIndex;
            this.correct = correct;
        }
    }

    public PlayQuizView() {
        root = new BorderPane();
        root.getStyleClass().add("dashboard-root");
        root.setTop(buildHeader());
        showSetup();
    }

    private HBox buildHeader() {
        HBox header = new HBox(12);
        header.getStyleClass().add("header");
        header.setPadding(new Insets(16, 30, 16, 30));
        header.setAlignment(Pos.CENTER_LEFT);

        Button back = new Button("\u2190 Back");
        back.getStyleClass().add("logout-btn");
        back.setOnAction(e -> {
            stopTimer();
            SceneManager.switchTo(new StudentDashboardView());
        });

        Label title = new Label("Play Quiz");
        title.getStyleClass().add("header-title");

        header.getChildren().addAll(back, title);
        return header;
    }

    // ================= GATE + SETUP PHASE =================

    private void showSetup() {
        Student student = (Student) AuthService.getCurrentUser();
        List<String> enrolled = new ArrayList<>(student.getEnrolledSubjects());
        if (enrolled.isEmpty()) {
            showEnrollmentPrompt();
            return;
        }

        VBox card = new VBox(14);
        card.getStyleClass().add("card");
        card.setMaxWidth(420);
        card.setPadding(new Insets(30));
        card.setAlignment(Pos.CENTER);

        Text heading = new Text("Configure your quiz");
        heading.getStyleClass().add("title");

        List<String> subjectOptions = new ArrayList<>();
        subjectOptions.add("All Enrolled");
        subjectOptions.addAll(enrolled);
        ComboBox<String> subjectCombo = new ComboBox<>(FXCollections.observableArrayList(subjectOptions));
        subjectCombo.setValue("All Enrolled");
        subjectCombo.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> diffCombo = new ComboBox<>(FXCollections.observableArrayList(
                "Any", "EASY", "MEDIUM", "HARD"));
        diffCombo.setValue("Any");
        diffCombo.setMaxWidth(Double.MAX_VALUE);

        Spinner<Integer> countSpinner = new Spinner<>(1, 50, 10);
        countSpinner.setEditable(true);
        countSpinner.setMaxWidth(Double.MAX_VALUE);

        CheckBox soundCheck = new CheckBox("Sound effects");
        soundCheck.setSelected(SoundService.isEnabled());
        soundCheck.setOnAction(e -> SoundService.setEnabled(soundCheck.isSelected()));

        Label passNote = new Label("Pass mark: " + ScoreRecord.PASSING_GRADE + "/100");
        passNote.getStyleClass().add("hint-label");

        Button startBtn = new Button("Start Quiz");
        startBtn.getStyleClass().add("primary-btn");
        startBtn.setMaxWidth(Double.MAX_VALUE);
        startBtn.setOnAction(e -> {
            chosenSubject = subjectCombo.getValue();
            chosenDifficulty = diffCombo.getValue();
            Question.Difficulty diffFilter = "Any".equals(chosenDifficulty)
                    ? null : Question.Difficulty.valueOf(chosenDifficulty);
            List<String> subjectsForQuiz = "All Enrolled".equals(chosenSubject)
                    ? enrolled : List.of(chosenSubject);
            questions = DataStore.getInstance().getRandomQuestionsForSubjects(
                    subjectsForQuiz, diffFilter, countSpinner.getValue());
            if (questions.isEmpty()) {
                SceneManager.showAlert(Alert.AlertType.WARNING, "No questions",
                        "No questions match those filters. Try widening your selection.");
                return;
            }
            startQuiz();
        });

        card.getChildren().addAll(heading,
                new Label("Subject"), subjectCombo,
                new Label("Difficulty"), diffCombo,
                new Label("Number of questions"), countSpinner,
                soundCheck, passNote,
                startBtn);

        StackPane center = new StackPane(card);
        center.setPadding(new Insets(30));
        root.setCenter(center);
    }

    private void showEnrollmentPrompt() {
        VBox card = new VBox(16);
        card.getStyleClass().add("card");
        card.setMaxWidth(440);
        card.setPadding(new Insets(32));
        card.setAlignment(Pos.CENTER);

        Text heading = new Text("No subjects yet");
        heading.getStyleClass().add("title");

        Label msg = new Label("You need to enroll in at least one subject before "
                + "you can take a quiz. Head to \"My Subjects\" to enroll.");
        msg.getStyleClass().add("subtitle");
        msg.setWrapText(true);
        msg.setMaxWidth(360);

        Button goEnroll = new Button("Go to My Subjects");
        goEnroll.getStyleClass().add("primary-btn");
        goEnroll.setMaxWidth(Double.MAX_VALUE);
        goEnroll.setOnAction(e -> SceneManager.switchTo(new SubjectsView()));

        Button back = new Button("Back to Dashboard");
        back.getStyleClass().add("link-btn");
        back.setOnAction(e -> SceneManager.switchTo(new StudentDashboardView()));

        card.getChildren().addAll(heading, msg, goEnroll, back);

        StackPane center = new StackPane(card);
        center.setPadding(new Insets(30));
        root.setCenter(center);
    }

    private void startQuiz() {
        currentIndex = 0;
        correctCount = 0;
        totalPoints = 0;
        currentStreak = 0;
        bestStreak = 0;
        perQuestionMillis.clear();
        answered.clear();

        quizStartMillis = System.currentTimeMillis();
        timer = new Timeline(new KeyFrame(Duration.millis(200), e -> updateTimerLabels()));
        timer.setCycleCount(Animation.INDEFINITE);
        timer.play();

        showQuestion();
    }

    // ================= QUESTION PHASE =================

    private void showQuestion() {
        if (currentIndex >= questions.size()) {
            showResults();
            return;
        }
        Question q = questions.get(currentIndex);
        answerRevealed = false;
        questionStartMillis = System.currentTimeMillis();

        VBox card = new VBox(16);
        card.getStyleClass().add("card");
        card.setMaxWidth(680);
        card.setPadding(new Insets(28));

        Label progressLabel = new Label("Question " + (currentIndex + 1) + " of " + questions.size());
        progressLabel.getStyleClass().add("subtitle");

        Label streakLabel = new Label("Streak: " + currentStreak);
        streakLabel.getStyleClass().add("streak-label");
        if (currentStreak >= 3) streakLabel.getStyleClass().add("streak-hot");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        questionTimerLabel = new Label("0.0s");
        questionTimerLabel.getStyleClass().add("timer-chip");

        overallTimerLabel = new Label("0:00");
        overallTimerLabel.getStyleClass().add("timer-chip-total");

        ToggleButton soundToggle = new ToggleButton(
                SoundService.isEnabled() ? "Sound: On" : "Sound: Off");
        soundToggle.setSelected(SoundService.isEnabled());
        soundToggle.getStyleClass().add("sound-toggle");
        soundToggle.setOnAction(e -> {
            SoundService.setEnabled(soundToggle.isSelected());
            soundToggle.setText(soundToggle.isSelected() ? "Sound: On" : "Sound: Off");
        });

        HBox statusRow = new HBox(10, progressLabel, streakLabel, spacer,
                questionTimerLabel, overallTimerLabel, soundToggle);
        statusRow.setAlignment(Pos.CENTER_LEFT);

        ProgressBar progressBar = new ProgressBar((currentIndex + 1.0) / questions.size());
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.getStyleClass().add("quiz-progress");

        Label meta = new Label(q.getSubject() + "  \u2022  " + q.getDifficulty().name()
                + "  \u2022  " + q.getPoints() + " pts");
        meta.getStyleClass().add("question-meta");

        Text questionText = new Text(q.getText());
        questionText.getStyleClass().add("question-text");
        questionText.setWrappingWidth(600);

        VBox optionsBox = new VBox(10);
        ToggleGroup group = new ToggleGroup();
        List<RadioButton> radios = new ArrayList<>();
        for (int i = 0; i < q.getOptions().size(); i++) {
            RadioButton rb = new RadioButton(q.getOptions().get(i));
            rb.setToggleGroup(group);
            rb.setUserData(i);
            rb.getStyleClass().add("option-radio");
            radios.add(rb);
            optionsBox.getChildren().add(rb);
        }

        Label feedback = new Label();
        feedback.getStyleClass().add("feedback-label");
        feedback.setVisible(false);

        boolean isLast = currentIndex == questions.size() - 1;
        Button actionBtn = new Button("Submit");
        actionBtn.getStyleClass().add("primary-btn");
        actionBtn.setOnAction(e -> {
            if (!answerRevealed) {
                Toggle selected = group.getSelectedToggle();
                if (selected == null) {
                    SceneManager.showAlert(Alert.AlertType.WARNING, "Pick an answer",
                            "Please select one of the options before submitting.");
                    return;
                }
                revealAnswer(q, (int) selected.getUserData(), radios, optionsBox,
                        feedback, streakLabel, actionBtn, isLast);
            } else {
                currentIndex++;
                showQuestion();
            }
        });

        card.getChildren().addAll(statusRow, progressBar, meta, questionText,
                optionsBox, feedback, actionBtn);

        StackPane center = new StackPane(card);
        center.setPadding(new Insets(24));
        root.setCenter(center);
    }

    private void revealAnswer(Question q, int pickedIndex, List<RadioButton> radios,
                              VBox optionsBox, Label feedback, Label streakLabel,
                              Button actionBtn, boolean isLast) {
        answerRevealed = true;

        long elapsed = System.currentTimeMillis() - questionStartMillis;
        perQuestionMillis.add(elapsed);

        boolean correct = q.isCorrect(pickedIndex);
        answered.add(new AnsweredQuestion(q, pickedIndex, correct));

        if (correct) {
            correctCount++;
            totalPoints += q.getPoints();
            currentStreak++;
            bestStreak = Math.max(bestStreak, currentStreak);
            SoundService.playCorrect();
            feedback.setText("Correct!  +" + q.getPoints() + " pts");
            feedback.getStyleClass().removeAll("feedback-wrong");
            if (!feedback.getStyleClass().contains("feedback-correct"))
                feedback.getStyleClass().add("feedback-correct");
        } else {
            currentStreak = 0;
            SoundService.playWrong();
            feedback.setText("Wrong! Correct answer: " + q.getOptions().get(q.getCorrectIndex()));
            feedback.getStyleClass().removeAll("feedback-correct");
            if (!feedback.getStyleClass().contains("feedback-wrong"))
                feedback.getStyleClass().add("feedback-wrong");
        }
        feedback.setVisible(true);

        radios.get(q.getCorrectIndex()).getStyleClass().add("option-correct");
        if (!correct) {
            radios.get(pickedIndex).getStyleClass().add("option-wrong");
        }
        optionsBox.setMouseTransparent(true);

        streakLabel.setText("Streak: " + currentStreak);
        streakLabel.getStyleClass().remove("streak-hot");
        if (currentStreak >= 3) streakLabel.getStyleClass().add("streak-hot");

        actionBtn.setText(isLast ? "See Results" : "Next");
    }

    private void updateTimerLabels() {
        long now = System.currentTimeMillis();
        if (overallTimerLabel != null) {
            overallTimerLabel.setText(formatClock(now - quizStartMillis));
        }
        if (questionTimerLabel != null) {
            long qMillis = answerRevealed && !perQuestionMillis.isEmpty()
                    ? perQuestionMillis.get(perQuestionMillis.size() - 1)
                    : now - questionStartMillis;
            questionTimerLabel.setText(String.format("%.1fs", qMillis / 1000.0));
        }
    }

    // ================= RESULTS PHASE =================

    private void showResults() {
        stopTimer();

        long totalMillis = System.currentTimeMillis() - quizStartMillis;
        int totalSeconds = (int) (totalMillis / 1000);
        double avgPerQuestion = perQuestionMillis.isEmpty() ? 0.0
                : perQuestionMillis.stream().mapToLong(Long::longValue).average().orElse(0) / 1000.0;

        String username = AuthService.getCurrentUser().getUsername();
        List<ScoreRecord> priorScores = DataStore.getInstance().getScoresForUser(username);
        int priorBest = priorScores.stream().mapToInt(ScoreRecord::getScore).max().orElse(-1);
        boolean newPersonalBest = !priorScores.isEmpty() && totalPoints > priorBest;

        // Build the record first so grade/letter/result come from one place (the model).
        ScoreRecord rec = new ScoreRecord(username, totalPoints, correctCount, questions.size(),
                chosenSubject, chosenDifficulty, totalSeconds, avgPerQuestion);
        DataStore.getInstance().addScore(rec);

        if (AuthService.getCurrentUser() instanceof Student student) {
            student.addPoints(totalPoints);
            student.incrementGamesPlayed();
            DataStore.getInstance().updateUser(student);
        }

        // Result sound based on pass/fail.
        if (rec.isPassing()) SoundService.playPass();
        else SoundService.playFail();

        // ---- summary card ----
        VBox summary = new VBox(8);
        summary.setAlignment(Pos.CENTER);

        Text done = new Text("Quiz Complete!");
        done.getStyleClass().add("title");

        Label score = new Label(correctCount + " / " + questions.size() + " correct");
        score.getStyleClass().add("big-number");

        Label gradeLabel = new Label("Grade: " + rec.getGrade() + "/100   (" + rec.getLetterGrade() + ")"
                + "   \u2022   " + totalPoints + " pts");
        gradeLabel.getStyleClass().add("subtitle");

        Label resultBadge = new Label(rec.getResult());
        resultBadge.getStyleClass().add(rec.isPassing() ? "result-pass" : "result-fail");

        summary.getChildren().addAll(done, score, gradeLabel, resultBadge);

        if (newPersonalBest) {
            Label pb = new Label("New personal best!");
            pb.getStyleClass().add("personal-best");
            summary.getChildren().add(pb);
        }

        // ---- timing + streak stat cards ----
        HBox stats = new HBox(16,
                statCard("Total Time", formatClock(totalMillis)),
                statCard("Avg / Question", String.format("%.1fs", avgPerQuestion)),
                statCard("Best Streak", String.valueOf(bestStreak)));
        stats.setAlignment(Pos.CENTER);

        // ---- review of wrong answers ----
        VBox reviewBox = new VBox(10);
        reviewBox.setPadding(new Insets(6, 0, 6, 0));

        List<AnsweredQuestion> wrong = answered.stream().filter(a -> !a.correct).toList();
        if (wrong.isEmpty()) {
            Label perfect = new Label("Perfect run \u2014 no wrong answers!");
            perfect.getStyleClass().add("feedback-correct");
            reviewBox.getChildren().add(perfect);
        } else {
            Label reviewHeading = new Label("Review \u2014 " + wrong.size()
                    + (wrong.size() == 1 ? " question to revisit" : " questions to revisit"));
            reviewHeading.getStyleClass().add("section-heading");
            reviewBox.getChildren().add(reviewHeading);

            for (AnsweredQuestion a : wrong) {
                VBox item = new VBox(4);
                item.getStyleClass().add("review-item");
                item.setPadding(new Insets(12));

                Label qText = new Label(a.question.getText());
                qText.getStyleClass().add("review-question");
                qText.setWrapText(true);

                Label yourAns = new Label("Your answer: " + a.question.getOptions().get(a.pickedIndex));
                yourAns.getStyleClass().add("review-your-answer");
                yourAns.setWrapText(true);

                Label correctAns = new Label("Correct answer: "
                        + a.question.getOptions().get(a.question.getCorrectIndex()));
                correctAns.getStyleClass().add("review-correct-answer");
                correctAns.setWrapText(true);

                item.getChildren().addAll(qText, yourAns, correctAns);
                reviewBox.getChildren().add(item);
            }
        }

        ScrollPane scroll = new ScrollPane(reviewBox);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("review-scroll");
        scroll.setPrefViewportHeight(200);

        Button playAgain = new Button("Play Again");
        playAgain.getStyleClass().add("primary-btn");
        playAgain.setOnAction(e -> showSetup());

        Button home = new Button("Back to Dashboard");
        home.getStyleClass().add("link-btn");
        home.setOnAction(e -> SceneManager.switchTo(new StudentDashboardView()));

        HBox actions = new HBox(12, playAgain, home);
        actions.setAlignment(Pos.CENTER);

        VBox card = new VBox(18, summary, stats, scroll, actions);
        card.getStyleClass().add("card");
        card.setMaxWidth(680);
        card.setPadding(new Insets(28));

        StackPane center = new StackPane(card);
        center.setPadding(new Insets(24));
        root.setCenter(center);
    }

    // ================= helpers =================

    private VBox statCard(String label, String value) {
        Label v = new Label(value);
        v.getStyleClass().add("stat-value");
        Label l = new Label(label);
        l.getStyleClass().add("subtitle");
        VBox card = new VBox(4, v, l);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("stat-card");
        card.setPadding(new Insets(14, 22, 14, 22));
        return card;
    }

    private String formatClock(long millis) {
        long totalSec = millis / 1000;
        return String.format("%d:%02d", totalSec / 60, totalSec % 60);
    }

    private void stopTimer() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
    }

    @Override
    public Parent getRoot() {
        return root;
    }
}
