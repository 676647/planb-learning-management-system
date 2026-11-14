package fr.skyframe.lms.live;

import fr.skyframe.lms.domain.*;
import fr.skyframe.lms.live.LiveGame.LiveQuestion;
import fr.skyframe.lms.live.LiveGame.Player;
import fr.skyframe.lms.live.LiveGame.PlayerAnswer;
import fr.skyframe.lms.live.dto.*;
import fr.skyframe.lms.repository.ContentRepository;
import fr.skyframe.lms.repository.EnrollmentRepository;
import fr.skyframe.lms.repository.LiveSessionRepository;
import fr.skyframe.lms.repository.UserRepository;
import fr.skyframe.lms.service.*;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Runs Kahoot-style live quizzes.
 *
 * The teacher hosts one of the course's quizzes; course members join with a 6-digit PIN.
 * Commands (join, answer, next question, …) come in as REST calls; every change is pushed
 * to all screens over WebSocket/STOMP (LiveGameMessenger). Games live in memory; when a game
 * ends, the final ranking is saved as a LiveSession. Results don't count toward grades.
 *
 * Note: because games are in memory, this works with one backend instance (as in compose.yaml).
 * Several instances would need a shared store and a broker relay (e.g. RabbitMQ's STOMP plugin).
 */
@Service
public class LiveGameService {

    private static final Logger log = LoggerFactory.getLogger(LiveGameService.class);

    /** Answers arriving up to this long after the deadline still count (network delay). */
    private static final Duration GRACE = Duration.ofSeconds(1);
    private static final int LEADERBOARD_SIZE = 5;
    /** Finished games stay viewable this long; abandoned games are removed after ABANDONED. */
    private static final Duration KEEP_FINISHED = Duration.ofMinutes(30);
    private static final Duration ABANDONED = Duration.ofHours(3);

    private final Map<String, LiveGame> games = new ConcurrentHashMap<>();
    private final QuizService quizService;
    private final UserService userService;
    private final UserRepository users;
    private final ContentRepository contents;
    private final EnrollmentRepository enrollments;
    private final LiveSessionRepository sessions;
    private final LiveGameMessenger messenger;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();
    /** Closes questions when their time is up, and removes old games. */
    private final ScheduledExecutorService timers = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "live-quiz-timer");
        thread.setDaemon(true);
        return thread;
    });

    public LiveGameService(QuizService quizService, UserService userService, UserRepository users,
                           ContentRepository contents, EnrollmentRepository enrollments,
                           LiveSessionRepository sessions, LiveGameMessenger messenger, Clock clock) {
        this.quizService = quizService;
        this.userService = userService;
        this.users = users;
        this.contents = contents;
        this.enrollments = enrollments;
        this.sessions = sessions;
        this.messenger = messenger;
        this.clock = clock;
        timers.scheduleAtFixedRate(this::removeOldGames, 10, 10, TimeUnit.MINUTES);
    }

    @PreDestroy
    void shutdown() {
        timers.shutdownNow();
    }

    // --- the host ---

    /** Opens a lobby for the quiz. Only one live quiz can run per course at a time. */
    @Transactional(readOnly = true) // the questions' lists are loaded here, before they leave the transaction
    public LiveGameView create(Long quizId, Long hostId, Integer secondsPerQuestion) {
        Quiz quiz = quizService.getQuiz(quizId);
        List<Question> questions = quizService.questionsOf(quizId);
        if (questions.isEmpty()) {
            throw new ConflictException("Add questions to the quiz before hosting it live");
        }
        User host = userService.get(hostId);
        Long courseId = quiz.getModule().getCourse().getId();
        List<LiveQuestion> played = questions.stream()
                .map(q -> new LiveQuestion(q, quizService.toStudentView(q), q.correctAnswer(),
                        secondsPerQuestion != null ? secondsPerQuestion : LiveScoring.defaultSeconds(q.getType())))
                .toList();

        LiveGame game;
        synchronized (games) {
            if (activeGameInCourse(courseId).isPresent()) {
                throw new ConflictException("A live quiz is already running in this course. End it first.");
            }
            game = new LiveGame(UUID.randomUUID().toString(), newPin(), quiz.getId(), quiz.getTitle(), courseId,
                    host.getId(), host.getName(), played, Instant.now(clock));
            games.put(game.id, game);
        }
        log.info("Live quiz '{}' opened by {} with PIN {}", game.quizTitle, host.getEmail(), game.pin);
        synchronized (game) {
            publish(game);
            messenger.courseChanged(courseId, noticeOf(game));
            return viewFor(game, hostId);
        }
    }

    /**
     * Moves the game on: from the lobby to the first question, or from an answer to the next
     * question. After the last question it ends the game.
     */
    @Transactional
    public LiveGameView next(String gameId, Long hostId) {
        LiveGame game = hostedGame(gameId, hostId);
        synchronized (game) {
            switch (game.phase) {
                case LOBBY -> {
                    if (game.players.isEmpty()) {
                        throw new ConflictException("Wait until at least one player has joined");
                    }
                    startQuestion(game, 0);
                }
                case REVEAL -> {
                    if (game.index + 1 < game.questions.size()) {
                        startQuestion(game, game.index + 1);
                    } else {
                        finish(game);
                    }
                }
                case QUESTION -> throw new ConflictException("Show the answer before going to the next question");
                case FINISHED -> throw new ConflictException("The game is over");
            }
            return viewFor(game, hostId);
        }
    }

    /** Closes the open question before the time is up. */
    public LiveGameView reveal(String gameId, Long hostId) {
        LiveGame game = hostedGame(gameId, hostId);
        synchronized (game) {
            if (game.phase != LivePhase.QUESTION) {
                throw new ConflictException("No question is open");
            }
            closeQuestion(game);
            return viewFor(game, hostId);
        }
    }

    /** Ends the game at once (also from the lobby or in the middle of a question). */
    @Transactional
    public LiveGameView end(String gameId, Long hostId) {
        LiveGame game = hostedGame(gameId, hostId);
        synchronized (game) {
            if (game.isActive()) {
                finish(game);
            }
            return viewFor(game, hostId);
        }
    }

    // --- the players ---

    public LiveGameView join(String pin, Long userId) {
        String cleanPin = pin.replaceAll("\\s", "");
        LiveGame game = games.values().stream()
                .filter(g -> g.isActive() && g.pin.equals(cleanPin))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("No live quiz with PIN " + cleanPin + " is running"));
        if (game.hostId.equals(userId)) {
            throw new ConflictException("You are hosting this quiz");
        }
        if (!enrollments.existsByUserIdAndCourseId(userId, game.courseId)) {
            throw new ForbiddenException("Only members of the course can join this quiz");
        }
        User user = userService.get(userId);
        synchronized (game) {
            if (!game.isActive()) {
                throw new ConflictException("This quiz has ended");
            }
            if (!game.players.containsKey(userId)) { // joining again (e.g. after a reload) keeps the score
                game.players.put(userId, new Player(userId, user.getEmail(), user.getName()));
                touch(game);
                publish(game);
            }
            return viewFor(game, userId);
        }
    }

    public LiveGameView answer(String gameId, Long userId, int questionNumber, List<String> answer) {
        LiveGame game = game(gameId);
        synchronized (game) {
            Player player = game.players.get(userId);
            if (player == null) {
                throw new ForbiddenException("Join the quiz with its PIN first");
            }
            if (game.phase != LivePhase.QUESTION || questionNumber != game.index + 1) {
                throw new ConflictException("This question is closed");
            }
            if (player.answers.containsKey(game.index)) {
                throw new ConflictException("You have already answered this question");
            }
            Instant now = Instant.now(clock);
            if (now.isAfter(game.deadline.plus(GRACE))) {
                throw new ConflictException("Time is up");
            }
            LiveQuestion question = game.current();
            double fraction = scoreSafely(question.question(), answer);
            int points = LiveScoring.points(fraction, Duration.between(game.questionStartedAt, now),
                    Duration.ofSeconds(question.seconds()));
            player.answers.put(game.index, new PlayerAnswer(List.copyOf(answer), fraction, points));
            player.score += points;
            if (fraction >= 1.0) {
                player.correctAnswers++;
            }
            touch(game);
            if (game.answeredCount() == game.players.size()) {
                closeQuestion(game); // everyone has answered: no need to wait for the timer
            } else {
                publish(game); // so the host sees "3 of 10 answered"
            }
            return viewFor(game, userId);
        }
    }

    // --- everyone ---

    /** The game as the host or a player sees it, e.g. after reloading the page. */
    public LiveGameView view(String gameId, Long userId) {
        LiveGame game = game(gameId);
        synchronized (game) {
            if (!game.hostId.equals(userId) && !game.players.containsKey(userId)) {
                throw new ForbiddenException("Join the quiz with its PIN first");
            }
            return viewFor(game, userId);
        }
    }

    /** Whether a live quiz is running in the course right now. */
    public CourseLiveNotice courseNotice(Long courseId) {
        return activeGameInCourse(courseId).map(this::noticeOf).orElse(CourseLiveNotice.none());
    }

    /** May this user receive the game's updates over the WebSocket? (Checked on SUBSCRIBE.) */
    public boolean canWatch(String gameId, Long userId) {
        LiveGame game = games.get(gameId);
        return game != null && (game.hostId.equals(userId) || game.players.containsKey(userId)
                || enrollments.existsByUserIdAndCourseId(userId, game.courseId));
    }

    /** Finished live quizzes of a quiz, newest first. */
    @Transactional(readOnly = true)
    public List<LiveSessionResponse> results(Long quizId) {
        quizService.getQuiz(quizId);
        return sessions.findByQuizIdOrderByEndedAtDescIdDesc(quizId).stream()
                .map(s -> new LiveSessionResponse(s.getId(), s.getStartedAt(), s.getEndedAt(), s.getQuestionsPlayed(),
                        s.getHost().getName(),
                        s.getPlayers().stream().map(p -> new LeaderboardEntry(p.getRank(), p.getUser().getId(),
                                p.getUser().getName(), p.getScore(), p.getCorrectAnswers())).toList()))
                .toList();
    }

    // --- game flow (always called while holding the game's lock) ---

    private void startQuestion(LiveGame game, int index) {
        game.index = index;
        game.phase = LivePhase.QUESTION;
        game.questionStartedAt = Instant.now(clock);
        int seconds = game.current().seconds();
        game.deadline = game.questionStartedAt.plusSeconds(seconds);
        game.cancelTimer();
        game.timer = timers.schedule(() -> timeUp(game.id, index), seconds * 1000L + GRACE.toMillis(),
                TimeUnit.MILLISECONDS);
        touch(game);
        publish(game);
    }

    /** Runs on the timer thread when a question's time is up. */
    private void timeUp(String gameId, int index) {
        LiveGame game = games.get(gameId);
        if (game == null) {
            return;
        }
        synchronized (game) {
            // The host may have closed it already, or moved on
            if (game.phase == LivePhase.QUESTION && game.index == index) {
                closeQuestion(game);
            }
        }
    }

    private void closeQuestion(LiveGame game) {
        game.cancelTimer();
        game.phase = LivePhase.REVEAL;
        touch(game);
        publish(game);
        sendPersonalUpdates(game);
    }

    private void finish(LiveGame game) {
        boolean played = game.index >= 0;
        game.cancelTimer();
        game.phase = LivePhase.FINISHED;
        game.endedAt = Instant.now(clock);
        touch(game);
        publish(game);
        sendPersonalUpdates(game);
        messenger.courseChanged(game.courseId, CourseLiveNotice.none());
        if (played && !game.players.isEmpty()) {
            save(game);
        }
        log.info("Live quiz '{}' (PIN {}) ended with {} players", game.quizTitle, game.pin, game.players.size());
    }

    private void save(LiveGame game) {
        Optional<Content> quiz = contents.findById(game.quizId);
        if (quiz.isEmpty() || !(quiz.get() instanceof Quiz q)) {
            return; // the quiz was deleted during the game
        }
        LiveSession session = new LiveSession();
        session.setQuiz(q);
        session.setHost(users.getReferenceById(game.hostId));
        session.setStartedAt(game.createdAt);
        session.setEndedAt(game.endedAt);
        session.setQuestionsPlayed(game.index + 1);
        for (Player p : game.ranking()) {
            session.addPlayer(users.getReferenceById(p.userId), p.score, p.correctAnswers, game.rankOf(p));
        }
        sessions.save(session);
    }

    private void publish(LiveGame game) {
        messenger.gameChanged(viewFor(game, null));
    }

    private void sendPersonalUpdates(LiveGame game) {
        for (Player p : game.players.values()) {
            messenger.toPlayer(p.email, new LivePersonalUpdate(game.id, meOf(game, p)));
        }
    }

    // --- views ---

    private LiveGameView viewFor(LiveGame game, Long viewerId) {
        boolean open = game.phase == LivePhase.QUESTION;
        boolean revealed = game.phase == LivePhase.REVEAL;
        LiveQuestion current = game.current();
        StudentQuestion question = (open || revealed) && current != null ? current.view() : null;
        long remaining = open ? Math.max(0, Duration.between(Instant.now(clock), game.deadline).toMillis()) : 0;
        List<String> names = game.phase == LivePhase.LOBBY
                ? game.players.values().stream().map(p -> p.name).toList()
                : null;
        List<LeaderboardEntry> leaderboard = switch (game.phase) {
            case REVEAL -> leaderboard(game, LEADERBOARD_SIZE);
            case FINISHED -> leaderboard(game, Integer.MAX_VALUE);
            default -> null;
        };
        LiveMe me = null;
        if (viewerId != null) {
            Player player = game.players.get(viewerId);
            me = player != null ? meOf(game, player) : new LiveMe(game.hostId.equals(viewerId), false, 0, 0, false, null);
        }
        return new LiveGameView(game.id, game.pin, game.quizId, game.quizTitle, game.courseId, game.hostName,
                game.phase, game.index + 1, game.questions.size(), question,
                current != null ? current.seconds() : 0, remaining, game.players.size(), names,
                open || revealed ? game.answeredCount() : 0,
                revealed ? revealOf(game) : null, leaderboard, me);
    }

    private LiveMe meOf(LiveGame game, Player player) {
        LiveFeedback last = null;
        int lastClosed = game.phase == LivePhase.QUESTION ? game.index - 1 : game.index;
        if (lastClosed >= 0 && game.phase != LivePhase.LOBBY) {
            PlayerAnswer a = player.answers.get(lastClosed);
            last = a == null
                    ? new LiveFeedback(lastClosed + 1, false, false, 0, 0)
                    : new LiveFeedback(lastClosed + 1, true, a.fraction() >= 1.0, a.fraction(), a.points());
        }
        return new LiveMe(false, true, player.score, game.rankOf(player),
                game.phase == LivePhase.QUESTION && player.answers.containsKey(game.index), last);
    }

    private LiveReveal revealOf(LiveGame game) {
        LiveQuestion current = game.current();
        List<PlayerAnswer> answers = game.players.values().stream()
                .map(p -> p.answers.get(game.index)).filter(Objects::nonNull).toList();
        List<Integer> optionCounts = null;
        if (current.question() instanceof MultipleChoiceQuestion mc) {
            int[] counts = new int[mc.getOptions().size()];
            for (PlayerAnswer a : answers) {
                for (String chosen : new HashSet<>(a.answer())) {
                    try {
                        int i = Integer.parseInt(chosen);
                        if (i >= 0 && i < counts.length) {
                            counts[i]++;
                        }
                    } catch (NumberFormatException ignored) {
                        // not an option number: counts as no option
                    }
                }
            }
            optionCounts = Arrays.stream(counts).boxed().toList();
        }
        int fullyCorrect = (int) answers.stream().filter(a -> a.fraction() >= 1.0).count();
        return new LiveReveal(current.correctAnswer(), optionCounts, fullyCorrect, answers.size());
    }

    private List<LeaderboardEntry> leaderboard(LiveGame game, int size) {
        return game.ranking().stream().limit(size)
                .map(p -> new LeaderboardEntry(game.rankOf(p), p.userId, p.name, p.score, p.correctAnswers))
                .toList();
    }

    private CourseLiveNotice noticeOf(LiveGame game) {
        return new CourseLiveNotice(true, game.id, game.pin, game.quizTitle, game.hostId, game.hostName);
    }

    // --- helpers ---

    private LiveGame game(String gameId) {
        LiveGame game = games.get(gameId);
        if (game == null) {
            throw new NotFoundException("Live quiz " + gameId + " not found");
        }
        return game;
    }

    private LiveGame hostedGame(String gameId, Long hostId) {
        LiveGame game = game(gameId);
        if (!game.hostId.equals(hostId)) {
            throw new ForbiddenException("Only the host can control the quiz");
        }
        return game;
    }

    private Optional<LiveGame> activeGameInCourse(Long courseId) {
        return games.values().stream().filter(g -> g.isActive() && g.courseId.equals(courseId)).findFirst();
    }

    /** A 6-digit PIN that no running game uses. Called while holding the games lock. */
    private String newPin() {
        while (true) {
            String pin = String.valueOf(100_000 + random.nextInt(900_000));
            if (games.values().stream().noneMatch(g -> g.isActive() && g.pin.equals(pin))) {
                return pin;
            }
        }
    }

    /** A malformed answer (e.g. too few blanks) counts as wrong instead of failing. */
    private static double scoreSafely(Question question, List<String> answer) {
        try {
            return question.score(answer);
        } catch (RuntimeException e) {
            return 0;
        }
    }

    private void touch(LiveGame game) {
        game.lastActivity = Instant.now(clock);
    }

    private void removeOldGames() {
        Instant now = Instant.now(clock);
        games.values().removeIf(game -> {
            synchronized (game) {
                boolean old = game.isActive()
                        ? game.lastActivity.plus(ABANDONED).isBefore(now)
                        : game.endedAt.plus(KEEP_FINISHED).isBefore(now);
                if (old && game.isActive()) {
                    game.cancelTimer(); // abandoned: dropped without saving
                    messenger.courseChanged(game.courseId, CourseLiveNotice.none());
                }
                return old;
            }
        });
    }
}
