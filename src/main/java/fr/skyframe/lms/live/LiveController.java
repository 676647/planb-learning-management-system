package fr.skyframe.lms.live;

import fr.skyframe.lms.live.dto.*;
import fr.skyframe.lms.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Live quiz commands. Every answer here is also pushed to the WebSocket (see LiveGameMessenger),
 * so the screens of everyone else in the game update at once.
 *
 * Who may do what: the course's teachers host; course members join with the PIN; only the host
 * moves the game on. Checks that depend on the game are in LiveGameService.
 */
@RestController
public class LiveController {

    private final LiveGameService liveGames;
    private final CurrentUser currentUser;

    public LiveController(LiveGameService liveGames, CurrentUser currentUser) {
        this.liveGames = liveGames;
        this.currentUser = currentUser;
    }

    /** Opens a lobby for one of the course's quizzes, with a PIN for the players. */
    @PostMapping("/api/contents/{quizId}/live")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@access.isTeacherOfContent(#quizId)")
    public LiveGameView host(@PathVariable Long quizId,
                             @Valid @RequestBody(required = false) CreateLiveGameRequest request) {
        return liveGames.create(quizId, currentUser.id(), request == null ? null : request.secondsPerQuestion());
    }

    /** Earlier live games of the quiz with their rankings (not grades). */
    @GetMapping("/api/contents/{quizId}/live-sessions")
    @PreAuthorize("@access.isTeacherOfContent(#quizId)")
    public List<LiveSessionResponse> results(@PathVariable Long quizId) {
        return liveGames.results(quizId);
    }

    /** Is a live quiz running in this course? (Shows the Join button.) */
    @GetMapping("/api/courses/{courseId}/live")
    @PreAuthorize("@access.isMemberOfCourse(#courseId)")
    public CourseLiveNotice courseLive(@PathVariable Long courseId) {
        return liveGames.courseNotice(courseId);
    }

    @PostMapping("/api/live/join")
    public LiveGameView join(@Valid @RequestBody JoinLiveGameRequest request) {
        return liveGames.join(request.pin(), currentUser.id());
    }

    @GetMapping("/api/live/{gameId}")
    public LiveGameView view(@PathVariable String gameId) {
        return liveGames.view(gameId, currentUser.id());
    }

    @PostMapping("/api/live/{gameId}/answers")
    public LiveGameView answer(@PathVariable String gameId, @Valid @RequestBody LiveAnswerRequest request) {
        return liveGames.answer(gameId, currentUser.id(), request.questionNumber(), request.answer());
    }

    /** Host: start the game, or go to the next question (ends the game after the last one). */
    @PostMapping("/api/live/{gameId}/next")
    public LiveGameView next(@PathVariable String gameId) {
        return liveGames.next(gameId, currentUser.id());
    }

    /** Host: close the question now and show the answer. */
    @PostMapping("/api/live/{gameId}/reveal")
    public LiveGameView reveal(@PathVariable String gameId) {
        return liveGames.reveal(gameId, currentUser.id());
    }

    @PostMapping("/api/live/{gameId}/end")
    public LiveGameView end(@PathVariable String gameId) {
        return liveGames.end(gameId, currentUser.id());
    }
}
