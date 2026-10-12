package com.quizmaster.services;

import com.quizmaster.models.ScoreRecord;
import com.quizmaster.models.Student;
import com.quizmaster.models.User;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * PDF generation service (satisfies requirement #8: "PDF file containing the
 * user's information"). Generates a personalized report with the user's profile
 * and — for students — their enrolled subjects, grade summary, and full quiz
 * history (with per-attempt grade and PASS/FAIL). Uses Apache PDFBox 2.x.
 */
public class PdfService {

    public static File generateUserInfoPdf(User user) throws IOException {
        Path desktop = Paths.get(System.getProperty("user.home"), "Desktop");
        Path outputDir = desktop.toFile().exists() ? desktop : Paths.get(System.getProperty("user.home"));
        File outputFile = outputDir.resolve(user.getUsername() + "_QuizMaster_Report.pdf").toFile();

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = 760;

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 22);
                cs.newLineAtOffset(60, y);
                cs.showText("QuizMaster - User Report");
                cs.endText();
                y -= 20;

                cs.addRect(60, y, 475, 1);
                cs.fill();
                y -= 40;

                y = writeLine(cs, "Profile Information", 60, y, PDType1Font.HELVETICA_BOLD, 16);
                y -= 10;
                y = writeLine(cs, "Username: " + user.getUsername(), 60, y, PDType1Font.HELVETICA, 12);
                y = writeLine(cs, "Full Name: " + user.getFullName(), 60, y, PDType1Font.HELVETICA, 12);
                y = writeLine(cs, "Role: " + user.getRoleDisplay(), 60, y, PDType1Font.HELVETICA, 12);
                y = writeLine(cs, "Date of Birth: " + safe(user.getDob()), 60, y, PDType1Font.HELVETICA, 12);
                y = writeLine(cs, "Member Since: " + safe(user.getCreatedAt()), 60, y, PDType1Font.HELVETICA, 12);
                y -= 20;

                // Student-only section: enrollment, grades, and score history.
                if (user instanceof Student student) {
                    // Enrolled subjects
                    y = writeLine(cs, "Enrolled Subjects", 60, y, PDType1Font.HELVETICA_BOLD, 16);
                    y -= 10;
                    List<String> subjects = student.getEnrolledSubjects();
                    if (subjects.isEmpty()) {
                        y = writeLine(cs, "Not enrolled in any subject yet.", 60, y,
                                PDType1Font.HELVETICA_OBLIQUE, 11);
                    } else {
                        y = writeLine(cs, String.join(", ", subjects), 60, y, PDType1Font.HELVETICA, 12);
                    }
                    y -= 16;

                    // Quiz statistics + grade summary
                    List<ScoreRecord> scores = DataStore.getInstance().getScoresForUser(student.getUsername());
                    int bestGrade = DataStore.getInstance().getBestGrade(student.getUsername());
                    long passes = scores.stream().filter(ScoreRecord::isPassing).count();

                    y = writeLine(cs, "Quiz Statistics", 60, y, PDType1Font.HELVETICA_BOLD, 16);
                    y -= 10;
                    y = writeLine(cs, "Total Points: " + student.getTotalPoints(), 60, y, PDType1Font.HELVETICA, 12);
                    y = writeLine(cs, "Games Played: " + student.getGamesPlayed(), 60, y, PDType1Font.HELVETICA, 12);
                    y = writeLine(cs, "Best Grade: " + (bestGrade < 0 ? "N/A" : bestGrade + "/100"),
                            60, y, PDType1Font.HELVETICA, 12);
                    y = writeLine(cs, "Quizzes Passed: " + passes + " of " + scores.size(),
                            60, y, PDType1Font.HELVETICA, 12);
                    y -= 20;

                    y = writeLine(cs, "Score History (" + scores.size() + " games)",
                            60, y, PDType1Font.HELVETICA_BOLD, 16);
                    y -= 10;

                    if (scores.isEmpty()) {
                        y = writeLine(cs, "No games played yet.", 60, y, PDType1Font.HELVETICA_OBLIQUE, 11);
                    } else {
                        for (ScoreRecord s : scores) {
                            if (y < 80) break; // avoid running off the page
                            String line = String.format("- %s | %s | %d/100 (%s) %s | %d pts | %s",
                                    s.getPlayedAt().substring(0, Math.min(19, s.getPlayedAt().length())),
                                    s.getSubject(),
                                    s.getGrade(), s.getLetterGrade(), s.getResult(),
                                    s.getScore(),
                                    s.getFormattedTime());
                            y = writeLine(cs, line, 60, y, PDType1Font.HELVETICA, 10);
                        }
                    }
                }

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_OBLIQUE, 9);
                cs.newLineAtOffset(60, 40);
                cs.showText("Generated by QuizMaster - " + java.time.LocalDateTime.now());
                cs.endText();
            }

            doc.save(outputFile);
        }
        return outputFile;
    }

    private static float writeLine(PDPageContentStream cs, String text, float x, float y,
                                   PDType1Font font, float size) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(text == null ? "" : text);
        cs.endText();
        return y - (size + 6);
    }

    private static String safe(String s) {
        return s == null ? "-" : s;
    }
}
