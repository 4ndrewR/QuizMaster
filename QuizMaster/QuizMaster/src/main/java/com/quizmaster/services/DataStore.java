package com.quizmaster.services;

import com.google.gson.*;
import com.quizmaster.models.*;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Singleton data-access layer. Persists users, questions, and scores as JSON
 * files in a "quizmaster-data" folder under the user's home directory.
 *
 * On first run, seeds:
 *   - default teacher: ProAnds / JavaProject123
 *   - 120 questions: 30 in each of the 4 subjects (Mathematics, Science,
 *     Programming, General Knowledge)
 *
 * The User type uses a custom JsonDeserializer because Gson alone cannot pick
 * the correct subclass (Teacher vs Student) when reading polymorphic JSON.
 */
public class DataStore {

    private static final DataStore INSTANCE = new DataStore();

    /** Canonical list of subjects. Each is seeded with exactly 30 questions. */
    public static final List<String> SUBJECTS = List.of(
            "Mathematics", "Science", "Programming", "General Knowledge");

    /** Bump this when the seeded question bank changes; forces a one-time reseed. */
    private static final int CURRENT_BANK_VERSION = 2;

    private final Path dataDir = Paths.get(System.getProperty("user.home"), "quizmaster-data");
    private final Path usersFile = dataDir.resolve("users.json");
    private final Path questionsFile = dataDir.resolve("questions.json");
    private final Path scoresFile = dataDir.resolve("scores.json");
    private final Path bankVersionFile = dataDir.resolve(".bankversion");

    private final Gson gson;
    private final List<User> users = new ArrayList<>();
    private final List<Question> questions = new ArrayList<>();
    private final List<ScoreRecord> scores = new ArrayList<>();

    private DataStore() {
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(User.class, new UserDeserializer())
                .create();
    }

    public static DataStore getInstance() {
        return INSTANCE;
    }

    /** Called once at app startup. Loads existing data or seeds defaults. */
    public void initialize() {
        try {
            Files.createDirectories(dataDir);
            loadUsers();
            loadQuestions();
            loadScores();

            if (users.isEmpty()) {
                seedDefaultTeacher();
            }
            // Seed (or re-seed on a bank-version bump) the question bank.
            if (questions.isEmpty() || readBankVersion() < CURRENT_BANK_VERSION) {
                questions.clear();
                seedDefaultQuestions();
                writeBankVersion(CURRENT_BANK_VERSION);
            }
        } catch (IOException e) {
            System.err.println("DataStore init failed: " + e.getMessage());
        }
    }

    // ---------------- USERS ----------------

    public List<User> getAllUsers() {
        return new ArrayList<>(users);
    }

    public List<Student> getAllStudents() {
        return users.stream()
                .filter(u -> u instanceof Student)
                .map(u -> (Student) u)
                .collect(Collectors.toList());
    }

    public int countTeachers() {
        return (int) users.stream().filter(u -> u instanceof Teacher).count();
    }

    public Optional<User> findUser(String username) {
        return users.stream()
                .filter(u -> u.getUsername().equalsIgnoreCase(username))
                .findFirst();
    }

    public boolean addUser(User user) {
        if (findUser(user.getUsername()).isPresent()) {
            return false; // duplicate username
        }
        users.add(user);
        saveUsers();
        return true;
    }

    public void updateUser(User user) {
        saveUsers(); // user object is mutated in place; just persist.
    }

    public boolean deleteUser(String username) {
        boolean removed = users.removeIf(u -> u.getUsername().equalsIgnoreCase(username));
        if (removed) saveUsers();
        return removed;
    }

    /**
     * Promote a Student to a Teacher. Implemented as an OBJECT SWAP using
     * Teacher's copy-constructor: the new Teacher keeps the same identity and
     * join date, replacing the Student in-place. Demonstrates polymorphism +
     * how state can be migrated between sibling subclasses.
     *
     * @return true if a Student was found and promoted.
     */
    public boolean promoteToTeacher(String username) {
        for (int i = 0; i < users.size(); i++) {
            User u = users.get(i);
            if (u.getUsername().equalsIgnoreCase(username) && u instanceof Student) {
                users.set(i, new Teacher(u));
                saveUsers();
                return true;
            }
        }
        return false;
    }

    /**
     * Demote a Teacher to a Student. Guarded so the LAST remaining teacher can
     * never be demoted (the system must always have at least one teacher).
     *
     * @return true if a Teacher was found, the guard passed, and it was demoted.
     */
    public boolean demoteToStudent(String username) {
        if (countTeachers() <= 1) return false; // never remove the last teacher
        for (int i = 0; i < users.size(); i++) {
            User u = users.get(i);
            if (u.getUsername().equalsIgnoreCase(username) && u instanceof Teacher) {
                users.set(i, new Student(u));
                saveUsers();
                return true;
            }
        }
        return false;
    }

    private void seedDefaultTeacher() {
        Teacher teacher = new Teacher("ProAnds", AuthService.hash("JavaProject123"), "Pro", "Ands", "2000-01-01");
        users.add(teacher);
        saveUsers();
    }

    // ---------------- QUESTIONS ----------------

    public List<Question> getAllQuestions() {
        return new ArrayList<>(questions);
    }

    /** The fixed list of subjects (always all four, regardless of bank state). */
    public List<String> getSubjects() {
        return new ArrayList<>(SUBJECTS);
    }

    public long countQuestionsInSubject(String subject) {
        return questions.stream().filter(q -> subject.equals(q.getSubject())).count();
    }

    /** Pull up to `count` random questions from a single subject (or any if null/"All"). */
    public List<Question> getRandomQuestions(String subject, Question.Difficulty difficulty, int count) {
        List<String> subjects = (subject == null || subject.equals("All"))
                ? null : List.of(subject);
        return getRandomQuestionsForSubjects(subjects, difficulty, count);
    }

    /** Pull up to `count` random questions drawn from any of the given subjects. */
    public List<Question> getRandomQuestionsForSubjects(List<String> subjects,
                                                        Question.Difficulty difficulty, int count) {
        List<Question> pool = questions.stream()
                .filter(q -> subjects == null || subjects.contains(q.getSubject()))
                .filter(q -> difficulty == null || q.getDifficulty() == difficulty)
                .collect(Collectors.toList());
        Collections.shuffle(pool);
        return pool.stream().limit(count).collect(Collectors.toList());
    }

    public void addQuestion(Question q) {
        questions.add(q);
        saveQuestions();
    }

    public boolean deleteQuestion(String id) {
        boolean removed = questions.removeIf(q -> q.getId().equals(id));
        if (removed) saveQuestions();
        return removed;
    }

    // ---------------- SCORES ----------------

    public List<ScoreRecord> getAllScores() {
        return new ArrayList<>(scores);
    }

    public List<ScoreRecord> getScoresForUser(String username) {
        return scores.stream()
                .filter(s -> s.getUsername().equalsIgnoreCase(username))
                .collect(Collectors.toList());
    }

    public List<ScoreRecord> getTopScores(int n) {
        return scores.stream()
                .sorted(Comparator.comparingInt(ScoreRecord::getScore).reversed())
                .limit(n)
                .collect(Collectors.toList());
    }

    /** Best (highest) grade out of 100 a user has achieved, or -1 if no games. */
    public int getBestGrade(String username) {
        return getScoresForUser(username).stream()
                .mapToInt(ScoreRecord::getGrade)
                .max().orElse(-1);
    }

    public void addScore(ScoreRecord record) {
        scores.add(record);
        saveScores();
    }

    // ---------------- FILE IO ----------------

    private void loadUsers() throws IOException {
        if (!Files.exists(usersFile)) return;
        String json = Files.readString(usersFile);
        Type listType = new com.google.gson.reflect.TypeToken<List<User>>() {}.getType();
        List<User> loaded = gson.fromJson(json, listType);
        if (loaded != null) {
            users.clear();
            users.addAll(loaded);
        }
    }

    private void saveUsers() {
        try {
            Files.writeString(usersFile, gson.toJson(users));
        } catch (IOException e) {
            System.err.println("Could not save users: " + e.getMessage());
        }
    }

    private void loadQuestions() throws IOException {
        if (!Files.exists(questionsFile)) return;
        String json = Files.readString(questionsFile);
        Type listType = new com.google.gson.reflect.TypeToken<List<Question>>() {}.getType();
        List<Question> loaded = gson.fromJson(json, listType);
        if (loaded != null) {
            questions.clear();
            questions.addAll(loaded);
        }
    }

    private void saveQuestions() {
        try {
            Files.writeString(questionsFile, gson.toJson(questions));
        } catch (IOException e) {
            System.err.println("Could not save questions: " + e.getMessage());
        }
    }

    private void loadScores() throws IOException {
        if (!Files.exists(scoresFile)) return;
        String json = Files.readString(scoresFile);
        Type listType = new com.google.gson.reflect.TypeToken<List<ScoreRecord>>() {}.getType();
        List<ScoreRecord> loaded = gson.fromJson(json, listType);
        if (loaded != null) {
            scores.clear();
            scores.addAll(loaded);
        }
    }

    private void saveScores() {
        try {
            Files.writeString(scoresFile, gson.toJson(scores));
        } catch (IOException e) {
            System.err.println("Could not save scores: " + e.getMessage());
        }
    }

    private int readBankVersion() {
        try {
            if (!Files.exists(bankVersionFile)) return 0;
            return Integer.parseInt(Files.readString(bankVersionFile).trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private void writeBankVersion(int version) {
        try {
            Files.writeString(bankVersionFile, String.valueOf(version));
        } catch (IOException e) {
            System.err.println("Could not write bank version: " + e.getMessage());
        }
    }

    // ---------------- SEED QUESTIONS (120 total = 30 per subject) ----------------

    private void seedDefaultQuestions() {
        seedMathematics();
        seedScience();
        seedProgramming();
        seedGeneralKnowledge();
    }

    private void add(String text, List<String> options, int correct,
                     String subject, Question.Difficulty diff) {
        addQuestion(new Question(text, options, correct, subject, diff));
    }

    private void seedMathematics() {
        String S = "Mathematics";
        var E = Question.Difficulty.EASY;
        var M = Question.Difficulty.MEDIUM;
        var H = Question.Difficulty.HARD;

        add("What is 7 \u00d7 8?", List.of("54", "56", "63", "48"), 1, S, E);
        add("What is 15 + 27?", List.of("42", "32", "52", "41"), 0, S, E);
        add("What is 100 \u00f7 4?", List.of("20", "25", "40", "50"), 1, S, E);
        add("What is 9 squared (9\u00b2)?", List.of("18", "72", "81", "99"), 2, S, E);
        add("What is the value of \u03c0 (pi) to two decimal places?",
                List.of("3.41", "3.14", "3.12", "3.16"), 1, S, E);
        add("How many sides does a hexagon have?", List.of("5", "6", "7", "8"), 1, S, E);
        add("What is 1/2 + 1/4?", List.of("1/4", "2/6", "3/4", "1/3"), 2, S, E);
        add("What is 10% of 250?", List.of("25", "20", "30", "15"), 0, S, E);
        add("What is the perimeter of a square with side length 5?",
                List.of("10", "20", "25", "15"), 1, S, E);
        add("What is 3 cubed (3\u00b3)?", List.of("9", "27", "12", "81"), 1, S, E);

        add("What is the square root of 144?", List.of("11", "12", "14", "16"), 1, S, M);
        add("Solve for x: 2x + 6 = 14", List.of("3", "4", "5", "8"), 1, S, M);
        add("What is the area of a circle with radius 3? (use \u03c0 \u2248 3.14)",
                List.of("18.84", "28.26", "9.42", "28.6"), 1, S, M);
        add("What is 7! (7 factorial)?", List.of("49", "720", "5040", "40320"), 2, S, M);
        add("What is the sum of the interior angles of a triangle?",
                List.of("90\u00b0", "180\u00b0", "270\u00b0", "360\u00b0"), 1, S, M);
        add("What is the slope of the line y = 3x + 2?", List.of("2", "3", "1", "-3"), 1, S, M);
        add("Convert 0.75 to a fraction in simplest form.",
                List.of("7/5", "3/4", "75/10", "1/4"), 1, S, M);
        add("What is the next prime number after 13?", List.of("14", "15", "17", "19"), 2, S, M);
        add("What is the area of a triangle with base 10 and height 6?",
                List.of("60", "16", "30", "300"), 2, S, M);
        add("If a = 5 and b = 3, what is a\u00b2 \u2212 b\u00b2?",
                List.of("16", "2", "4", "34"), 0, S, M);
        add("What is log\u2081\u2080(1000)?", List.of("2", "3", "10", "100"), 1, S, M);
        add("What is the least common multiple (LCM) of 4 and 6?",
                List.of("2", "12", "24", "10"), 1, S, M);

        add("What is the derivative of x\u00b2 with respect to x?",
                List.of("x", "2x", "x\u00b2", "2"), 1, S, H);
        add("What is the integral of 2x dx?",
                List.of("2 + C", "x\u00b2 + C", "x\u00b2/2 + C", "2x\u00b2 + C"), 1, S, H);
        add("What is the value of sin(90\u00b0)?",
                List.of("0", "0.5", "1", "\u221a2/2"), 2, S, H);
        add("What is the discriminant of x\u00b2 \u2212 4x + 4?",
                List.of("0", "4", "8", "16"), 0, S, H);
        add("What is the sum of the first 10 positive integers (1 to 10)?",
                List.of("50", "55", "45", "100"), 1, S, H);
        add("In a right triangle with legs 3 and 4, what is the hypotenuse?",
                List.of("5", "6", "7", "25"), 0, S, H);
        add("What is the value of cos(0\u00b0)?", List.of("0", "1", "0.5", "-1"), 1, S, H);
        add("What is 2 to the power of 10 (2\u00b9\u2070)?",
                List.of("512", "1000", "1024", "2048"), 2, S, H);
    }

    private void seedScience() {
        String S = "Science";
        var E = Question.Difficulty.EASY;
        var M = Question.Difficulty.MEDIUM;
        var H = Question.Difficulty.HARD;

        add("What is the chemical formula for water?",
                List.of("CO2", "H2O", "O2", "NaCl"), 1, S, E);
        add("What gas do plants absorb from the atmosphere for photosynthesis?",
                List.of("Oxygen", "Nitrogen", "Carbon Dioxide", "Hydrogen"), 2, S, E);
        add("Which planet is closest to the Sun?",
                List.of("Venus", "Mercury", "Earth", "Mars"), 1, S, E);
        add("What is the hardest natural substance on Earth?",
                List.of("Gold", "Iron", "Diamond", "Quartz"), 2, S, E);
        add("How many legs does an insect have?", List.of("4", "6", "8", "10"), 1, S, E);
        add("Which organ pumps blood through the human body?",
                List.of("Liver", "Heart", "Lungs", "Kidney"), 1, S, E);
        add("What is the largest planet in our solar system?",
                List.of("Saturn", "Jupiter", "Neptune", "Earth"), 1, S, E);
        add("What is H2O more commonly known as?",
                List.of("Salt", "Water", "Hydrogen Peroxide", "Ammonia"), 1, S, E);
        add("What is the chemical symbol for gold?",
                List.of("Go", "Gd", "Au", "Ag"), 2, S, E);
        add("What force pulls objects toward the Earth?",
                List.of("Magnetism", "Gravity", "Friction", "Tension"), 1, S, E);

        add("What is the powerhouse of the cell?",
                List.of("Nucleus", "Mitochondria", "Ribosome", "Golgi Apparatus"), 1, S, M);
        add("How many bones are in the adult human body?",
                List.of("196", "206", "216", "226"), 1, S, M);
        add("What is the most abundant gas in Earth's atmosphere?",
                List.of("Oxygen", "Carbon Dioxide", "Nitrogen", "Hydrogen"), 2, S, M);
        add("What is the approximate speed of light in a vacuum?",
                List.of("300,000 km/s", "150,000 km/s", "1,000,000 km/s", "30,000 km/s"), 0, S, M);
        add("Which part of the cell contains its genetic material (DNA)?",
                List.of("Cytoplasm", "Nucleus", "Membrane", "Vacuole"), 1, S, M);
        add("Which blood cells primarily help fight infection?",
                List.of("Red blood cells", "White blood cells", "Platelets", "Plasma"), 1, S, M);
        add("What is the chemical symbol for sodium?",
                List.of("So", "Sd", "Na", "S"), 2, S, M);
        add("At what temperature does water boil at sea level?",
                List.of("90\u00b0C", "100\u00b0C", "80\u00b0C", "120\u00b0C"), 1, S, M);
        add("What is the process by which plants make food using sunlight?",
                List.of("Respiration", "Photosynthesis", "Digestion", "Fermentation"), 1, S, M);
        add("How many chambers does the human heart have?",
                List.of("2", "3", "4", "5"), 2, S, M);
        add("What type of energy is stored in food?",
                List.of("Kinetic", "Chemical", "Nuclear", "Thermal"), 1, S, M);
        add("What gas is released as a byproduct of photosynthesis?",
                List.of("Carbon Dioxide", "Oxygen", "Nitrogen", "Methane"), 1, S, M);

        add("What is the pH of a neutral solution?",
                List.of("0", "7", "14", "1"), 1, S, H);
        add("Which subatomic particle carries a negative charge?",
                List.of("Proton", "Neutron", "Electron", "Nucleus"), 2, S, H);
        add("What does Newton's second law of motion state?",
                List.of("F = m/a", "F = ma", "E = mc\u00b2", "F = mv"), 1, S, H);
        add("What is the chemical formula for common table salt?",
                List.of("NaCl", "KCl", "CaCl2", "NaHCO3"), 0, S, H);
        add("Which scientist proposed the theory of general relativity?",
                List.of("Isaac Newton", "Albert Einstein", "Galileo Galilei", "Niels Bohr"), 1, S, H);
        add("What is the smallest unit of an element that retains its properties?",
                List.of("Molecule", "Atom", "Cell", "Proton"), 1, S, H);
        add("What is the SI unit of electrical resistance?",
                List.of("Volt", "Ampere", "Ohm", "Watt"), 2, S, H);
        add("Which scientist is credited with the laws of planetary motion?",
                List.of("Johannes Kepler", "Charles Darwin", "Marie Curie", "Michael Faraday"), 0, S, H);
    }

    private void seedProgramming() {
        String S = "Programming";
        var E = Question.Difficulty.EASY;
        var M = Question.Difficulty.MEDIUM;
        var H = Question.Difficulty.HARD;

        add("Which keyword in Java is used to inherit from a class?",
                List.of("implements", "extends", "inherits", "super"), 1, S, E);
        add("What does JVM stand for?",
                List.of("Java Virtual Machine", "Java Verified Module",
                        "Just-in-time Virtual Memory", "Java Variable Manager"), 0, S, E);
        add("What does HTML stand for?",
                List.of("HyperText Markup Language", "HighText Machine Language",
                        "HyperText Markdown Language", "Hyperlink Text Marking Language"), 0, S, E);
        add("Which symbol begins a single-line comment in Java?",
                List.of("#", "//", "/*", "--"), 1, S, E);
        add("Which data type stores a true/false value in Java?",
                List.of("int", "String", "boolean", "char"), 2, S, E);
        add("Which company originally developed Java?",
                List.of("Microsoft", "Sun Microsystems", "Apple", "Google"), 1, S, E);
        add("What does CPU stand for?",
                List.of("Central Processing Unit", "Computer Personal Unit",
                        "Central Process Utility", "Central Programming Unit"), 0, S, E);
        add("Which of these is a programming language?",
                List.of("Python", "HTTP", "JSON", "CSS"), 0, S, E);
        add("Which symbol typically ends a statement in Java?",
                List.of(".", ";", ":", ","), 1, S, E);
        add("Which keyword creates a new object in Java?",
                List.of("create", "new", "object", "make"), 1, S, E);

        add("Which design pattern restricts a class to a single instance?",
                List.of("Factory", "Observer", "Singleton", "Decorator"), 2, S, M);
        add("Which data structure works on a FIFO (First In, First Out) basis?",
                List.of("Stack", "Queue", "Tree", "Graph"), 1, S, M);
        add("What is the time complexity of binary search?",
                List.of("O(n)", "O(log n)", "O(n\u00b2)", "O(1)"), 1, S, M);
        add("Which data structure works on a LIFO (Last In, First Out) basis?",
                List.of("Queue", "Stack", "Array", "Tree"), 1, S, M);
        add("In OOP, hiding an object's internal data is called what?",
                List.of("Inheritance", "Encapsulation", "Polymorphism", "Abstraction"), 1, S, M);
        add("Which keyword declares a constant value in Java?",
                List.of("const", "static", "final", "constant"), 2, S, M);
        add("What does SQL stand for?",
                List.of("Simple Query Language", "Structured Query Language",
                        "Standard Question Language", "System Query Language"), 1, S, M);
        add("Which of these is NOT a primitive type in Java?",
                List.of("int", "double", "String", "char"), 2, S, M);
        add("What is the result of 5 % 2 in most languages?",
                List.of("2", "1", "2.5", "0"), 1, S, M);
        add("Which Java collection does not allow duplicate elements?",
                List.of("List", "Set", "ArrayList", "Map"), 1, S, M);
        add("What does API stand for?",
                List.of("Application Programming Interface", "Applied Program Internet",
                        "Application Process Integration", "Automated Programming Interface"), 0, S, M);
        add("Which loop is guaranteed to execute its body at least once?",
                List.of("for", "while", "do-while", "foreach"), 2, S, M);

        add("In JavaFX, which class is the base for all UI controls?",
                List.of("Node", "Control", "Region", "Parent"), 1, S, H);
        add("What is the worst-case time complexity of bubble sort?",
                List.of("O(n)", "O(n log n)", "O(n\u00b2)", "O(log n)"), 2, S, H);
        add("Which OOP principle lets a subclass override a parent's method?",
                List.of("Encapsulation", "Polymorphism", "Abstraction", "Composition"), 1, S, H);
        add("For a method, what does the 'static' keyword mean in Java?",
                List.of("It cannot be changed", "It belongs to the class, not an instance",
                        "It is always private", "It runs before main"), 1, S, H);
        add("Which HTTP method is typically used to create a new resource?",
                List.of("GET", "POST", "DELETE", "HEAD"), 1, S, H);
        add("In Java, a reference that points to no object has what value?",
                List.of("void", "null", "empty", "zero"), 1, S, H);
        add("Which access modifier makes a member visible only within its own class?",
                List.of("public", "protected", "private", "default"), 2, S, H);
        add("What does OOP stand for?",
                List.of("Object-Oriented Programming", "Open Output Protocol",
                        "Optimized Operation Process", "Object Operation Programming"), 0, S, H);
    }

    private void seedGeneralKnowledge() {
        String S = "General Knowledge";
        var E = Question.Difficulty.EASY;
        var M = Question.Difficulty.MEDIUM;
        var H = Question.Difficulty.HARD;

        add("What is the capital of France?",
                List.of("Berlin", "Madrid", "Paris", "Rome"), 2, S, E);
        add("How many continents are there on Earth?",
                List.of("5", "6", "7", "8"), 2, S, E);
        add("What is the largest mammal in the world?",
                List.of("African Elephant", "Blue Whale", "Giraffe", "Polar Bear"), 1, S, E);
        add("How many colors are in a rainbow?",
                List.of("5", "6", "7", "8"), 2, S, E);
        add("What is the capital of Japan?",
                List.of("Beijing", "Seoul", "Tokyo", "Bangkok"), 2, S, E);
        add("Which planet do humans live on?",
                List.of("Mars", "Venus", "Earth", "Jupiter"), 2, S, E);
        add("How many days are in a leap year?",
                List.of("364", "365", "366", "367"), 2, S, E);
        add("Which is the largest country in the world by area?",
                List.of("Canada", "China", "United States", "Russia"), 3, S, E);
        add("How many players from one team are on a soccer field at once?",
                List.of("9", "10", "11", "12"), 2, S, E);
        add("What color results from mixing blue and yellow?",
                List.of("Purple", "Green", "Orange", "Brown"), 1, S, E);

        add("In what year did World War II end?",
                List.of("1943", "1944", "1945", "1946"), 2, S, M);
        add("Who painted the Mona Lisa?",
                List.of("Michelangelo", "Leonardo da Vinci", "Raphael", "Donatello"), 1, S, M);
        add("Who was the first President of the United States?",
                List.of("Thomas Jefferson", "John Adams", "George Washington", "Benjamin Franklin"), 2, S, M);
        add("In which year did the Berlin Wall fall?",
                List.of("1987", "1989", "1991", "1993"), 1, S, M);
        add("What is the smallest country in the world by area?",
                List.of("Monaco", "Vatican City", "San Marino", "Liechtenstein"), 1, S, M);
        add("How many strings does a standard guitar have?",
                List.of("4", "5", "6", "7"), 2, S, M);
        add("What is the largest ocean on Earth?",
                List.of("Atlantic", "Pacific", "Indian", "Arctic"), 1, S, M);
        add("In which sport would you perform a slam dunk?",
                List.of("Volleyball", "Basketball", "Tennis", "Cricket"), 1, S, M);
        add("How many planets are in our solar system?",
                List.of("7", "8", "9", "10"), 1, S, M);
        add("What is the currency of Japan?",
                List.of("Won", "Yen", "Yuan", "Dollar"), 1, S, M);
        add("Which language has the most native speakers worldwide?",
                List.of("English", "Mandarin Chinese", "Spanish", "Hindi"), 1, S, M);
        add("How many sides does an octagon have?",
                List.of("6", "7", "8", "10"), 2, S, M);

        add("In which year did the Western Roman Empire traditionally fall?",
                List.of("476 AD", "1066 AD", "1453 AD", "323 BC"), 0, S, H);
        add("How many rings are on the Olympic flag?",
                List.of("4", "5", "6", "7"), 1, S, H);
        add("What is the capital of Australia?",
                List.of("Sydney", "Melbourne", "Canberra", "Perth"), 2, S, H);
        add("Who wrote the play 'Romeo and Juliet'?",
                List.of("Charles Dickens", "William Shakespeare", "Jane Austen", "Mark Twain"), 1, S, H);
        add("What is the largest hot desert in the world?",
                List.of("Gobi", "Sahara", "Kalahari", "Arabian"), 1, S, H);
        add("On which continent is Egypt located?",
                List.of("Asia", "Africa", "Europe", "South America"), 1, S, H);
        add("Which civilization built the Pyramids of Giza?",
                List.of("Romans", "Greeks", "Ancient Egyptians", "Mayans"), 2, S, H);
        add("What is the currency of the United Kingdom?",
                List.of("Euro", "Pound Sterling", "Dollar", "Franc"), 1, S, H);
    }

    // ---------------- USER DESERIALIZER ----------------

    /**
     * Custom deserializer: reads the `type` field and instantiates the matching
     * subclass. Without this, Gson would try to create the abstract User and fail.
     *
     * Backward compatibility: older save files used "ADMIN"/"CLIENT"; those are
     * mapped onto the new Teacher/Student classes so existing data still loads.
     */
    private static class UserDeserializer implements JsonDeserializer<User> {
        @Override
        public User deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx)
                throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            String type = obj.has("type") ? obj.get("type").getAsString() : "STUDENT";
            return switch (type) {
                case "TEACHER", "ADMIN" -> ctx.deserialize(json, Teacher.class);
                default -> ctx.deserialize(json, Student.class); // STUDENT, CLIENT, or anything else
            };
        }
    }
}
