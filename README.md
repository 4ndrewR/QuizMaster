# QuizMaster

A desktop trivia-quiz application built with **JavaFX**. Two roles — **Teacher**
and **Student** — with separate interfaces, subject enrollment, a graded quiz
engine, JSON persistence, and per-user PDF reports.

---

## What's new in v1.2

- **Roles renamed:** `Admin` → **Teacher**, `Client` → **Student** (full class
  rename, not just labels).
- **Role management:** a teacher can **promote** a student to teacher or
  **demote** a teacher to student. Guards prevent removing the last teacher or
  demoting yourself. Implemented as an object swap that preserves the user's
  identity and join date.
- **Grading system:** every quiz yields a **grade out of 100** with a letter
  (A–F). **60/100 is a pass.** Pass/fail is shown on the results screen, in
  score history, on the leaderboard, in reports, and in the PDF.
- **Subjects + enrollment:** four subjects — **Mathematics** (new), Science,
  Programming, General Knowledge — each with **30 questions (120 total)**.
  Students enroll in subjects and can only be quizzed on enrolled subjects.
- **Teacher subject overview:** question counts and enrolled-student counts.
- **Extras:** pass/fail result jingles, best-grade tracking, and pass-rate /
  average-grade stats in reports.
- **Backward compatible:** existing save files using the old `ADMIN`/`CLIENT`
  types still load as Teacher/Student.

---

## Demo account

A demo teacher account is seeded on first run so you can try the app:

```
Teacher (demo):
  username: ProAnds
  password: JavaProject123
```

Students sign up themselves from the login screen, then enroll in subjects via
**My Subjects** before playing.

---

## How to run

Requires a JDK (17+) and Maven. JavaFX is pulled in by Maven automatically.

```bash
mvn clean javafx:run
```

On Windows (PowerShell), if `mvn` isn't on the system PATH, set it for the
current terminal session first, then run the command above:

```powershell
$env:Path = "C:\path\to\apache-maven\bin;" + $env:Path
mvn clean javafx:run
```

---

## Data & reset

Data is saved as JSON under your home directory:

```
<home>/quizmaster-data/
  users.json        # accounts (teachers + students)
  questions.json    # the 120-question bank
  scores.json       # every graded attempt
  .bankversion      # internal marker; triggers a one-time bank refresh on upgrade
```

PDF reports are written to your **Desktop** (or home folder) as
`<username>_QuizMaster_Report.pdf`.

To start completely fresh, delete the `quizmaster-data` folder. (Upgrading from
v1.1 reseeds the question bank automatically via the `.bankversion` marker; your
accounts and scores are kept.)

---

## Project structure

```
src/main/java/com/quizmaster/
  Main.java                     # entry point (extends Application)
  models/                       # data: User (abstract) -> Teacher / Student,
                                #       Question, ScoreRecord (grading lives here)
  services/                     # logic: DataStore (singleton + JSON + seeding),
                                #        AuthService, PdfService, SoundService
  utils/SceneManager.java       # scene switching + the View interface
  views/                        # one class per screen (login, dashboards,
                                # quiz, subjects, manage users/questions, etc.)
src/main/resources/styles.css   # dark theme
pom.xml                         # Maven build (JavaFX, Gson, PDFBox, jBCrypt)
```

---

## Libraries

- **JavaFX 21** — UI
- **Gson 2.10.1** — JSON persistence (with a custom polymorphic deserializer)
- **Apache PDFBox 2.0.30** — PDF generation
- **jBCrypt 0.4** — salted password hashing

---

## Security

Passwords are never stored in plain text. They are hashed with **BCrypt**
(cost factor 12, unique salt per user) and verified with `BCrypt.checkpw`.
Save files created by older versions, which stored plaintext passwords, are
migrated automatically: on a user's next successful login the password is
re-hashed and the record is rewritten.
