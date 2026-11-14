# Live quiz (novel feature)

A Kahoot-style quiz played together in class. The teacher hosts one of the course's quizzes on the
projector; students join on their phones or laptops with a 6-digit PIN and answer against the clock.
After every question everyone sees the right answer, how many picked each option and the
leaderboard; at the end there is a podium. Results are saved for the teacher but **don't count
toward grades**.

## How to use it

1. **Teacher:** open a quiz that has questions, choose the time per question under *⚡ Live quiz*
   and click **Host live**. The lobby shows the PIN.
2. **Students:** either click **Join** in the banner that appears on the course page, or enter the
   PIN on the dashboard. Only members of the course can join.
3. **Teacher:** click **Start** when everyone is in. A question closes when the time is up, when
   everyone has answered, or when the teacher clicks **Show answer**. **Next question** moves on;
   after the last question the podium is shown. **End quiz** stops the game at any time.
4. Afterwards, the quiz page lists *Earlier live games* with the winners.

All four question types work. Multiple choice gets Kahoot's colored tiles (one tap answers if
there is one correct option); ordering, matching and fill-in-the-blanks use the normal quiz
controls with a **Submit answer** button. Default times: 20 s multiple choice, 30 s ordering,
40 s blanks, 45 s matching, or the same time for every question.

**Scoring:** a correct answer gives 1000 points if given at once, falling to 500 at the last
moment. Partly correct answers (ordering, matching, blanks) give the same share of that, e.g.
2 of 4 items in place = half. Wrong or no answer = 0.

> Students see the questions and the correct answers during the game. Use a separate quiz for live
> games (it can be hidden from students), not the graded one.

## New technology: WebSockets with STOMP

A normal REST API only answers when the browser asks. A live quiz needs the opposite: when one
student answers, the teacher's screen must update at once, and when the teacher moves on, every
phone must show the next question. For this we use **WebSockets** (one long-lived, two-way
connection per browser) with **STOMP**, a simple publish/subscribe protocol on top of them, via
Spring's `spring-boot-starter-websocket` and the `@stomp/stompjs` client.

```
 teacher's browser             Spring Boot                            students' browsers
 ──────────────────            ───────────                            ──────────────────
 POST /api/live/{id}/next ──►  LiveController ─► LiveGameService
                                                      │ changes the game in memory
                                                      ▼
                               LiveGameMessenger ── SimpMessagingTemplate
                                                      │ in-memory STOMP broker
                 ◄── MESSAGE /topic/live/{id} ────────┼──── MESSAGE /topic/live/{id} ──►
                                                      └──── MESSAGE /user/queue/live ──► (each player: own score)
```

- **Commands use REST, updates use the WebSocket.** Joining, answering and the host's buttons are
  ordinary REST calls, with the same security and validation as the rest of the API, and they are
  easy to test. The WebSocket only carries messages *from* the server.
- **Destinations:**
  - `/topic/live/{gameId}`: the game for everyone in it (phase, question, countdown, answer
    counts, leaderboard). Never contains the correct answer while a question is open.
  - `/user/queue/live`: one player's own score, rank and how their last answer went. Spring sends
    it only to that user's connections.
  - `/topic/courses/{courseId}/live`: a live quiz started or ended (for the Join banner).
- **Security** (`StompAuthInterceptor`): browsers can't send headers when opening a WebSocket, so
  the handshake on `/api/ws` is open and the JWT goes in the STOMP `CONNECT` frame instead
  (`Authorization: Bearer …`, the same token as for REST). An invalid token is refused. A
  `SUBSCRIBE` is only allowed to the user's own queue, to games in the user's courses and to their
  courses' notices. Sending messages to the server is refused, since commands go through REST.
- **Timing:** the server says how many milliseconds are left (not a timestamp), so the countdown
  doesn't depend on the clocks of the browsers. The server enforces the deadline (plus one second
  for network delay) and closes the question with a timer.
- **Reconnecting:** if a connection drops, the client reconnects by itself, subscribes again and
  loads the game over REST, so nothing is lost. Reloading the page works the same way.
- **Proxies:** nginx (`frontend/nginx.conf`) and the Vite dev server (`vite.config.js`, `ws: true`)
  forward the WebSocket upgrade on `/api/ws`. Heartbeats every 10 seconds keep idle
  connections (e.g. in the lobby) open.

## Where the code is

| Part | Files |
|---|---|
| Game engine (in memory) | `live/LiveGameService.java`, `live/LiveGame.java`, `live/LivePhase.java`, `live/LiveScoring.java` |
| WebSocket setup and security | `live/WebSocketConfig.java`, `live/StompAuthInterceptor.java`, `live/LiveGameMessenger.java` |
| REST API | `live/LiveController.java`, `live/dto/*` |
| Saved results | `domain/LiveSession.java`, `domain/LiveSessionPlayer.java`, `repository/LiveSessionRepository.java` |
| Tests | `LiveQuizApiTests` (REST flow, rules, scoring), `LiveWebSocketTests` (real STOMP connections), `live/LiveScoringTests` |
| Frontend | `live/liveSocket.js` (shared STOMP connection), `live/useLiveGame.js`, `live/LiveEntry.jsx` (Host / Join / banner), `pages/LiveGame.jsx` + `.css` (the game screens) |

## REST endpoints

| Method and path | Who | What |
|---|---|---|
| `POST /api/contents/{quizId}/live` | teacher of the course | open a lobby; body `{"secondsPerQuestion": 20}` (optional) |
| `GET /api/courses/{courseId}/live` | course member | is a live quiz running? |
| `POST /api/live/join` | course member | join with `{"pin": "123456"}` |
| `GET /api/live/{gameId}` | host or player | the game now, including my own score |
| `POST /api/live/{gameId}/answers` | player | `{"questionNumber": 1, "answer": ["2"]}` (same answer format as quiz attempts) |
| `POST /api/live/{gameId}/next` | host | start / next question / show results |
| `POST /api/live/{gameId}/reveal` | host | close the question now |
| `POST /api/live/{gameId}/end` | host | end the game |
| `GET /api/contents/{quizId}/live-sessions` | teacher | earlier games with rankings |

## Limits

- Games are kept in the memory of the backend (a game lasts minutes and changes several times per
  second, which suits memory better than the database). The final ranking is saved in PostgreSQL.
  This means one backend instance, as in `compose.yaml`; a restart ends running games. Several
  instances would need a shared game store and a broker relay (e.g. RabbitMQ's STOMP plugin)
  instead of Spring's in-memory broker.
- One live quiz per course at a time. Abandoned games are removed after 3 hours.
