# Code review: learning-management-system

Nov 18, 2026 · @Ståle

The code is in good shape overall, but one leaked database password needs fixing today and three bugs let students get grades or attempts they shouldn't.

Scope: the backend (security, services, live quiz, storage, messaging), the deploy and CI config, and the frontend API layer. Strengths: every endpoint declares its permissions, the domain rules sit in services, cleanup across Postgres and Mongo is tied to transaction outcomes, and there are 20 test classes. ESLint passes on the frontend with no warnings. The Java tests were not run, because Maven isn't installed on the machine and the wrapper needs network access.

Findings are numbered in order of how much they matter.

## 1. A real database password is committed and pushed

`src/main/resources/application-local.properties` contains `spring.datasource.password=npg_Nb0y…`. That looks like a live Neon Postgres credential. It was added in commit `3bdbbd2` and is on `origin/main` and `origin/LSM_frontend_right`.

The `.gitignore` entry meant to exclude it has a typo: `.src/main/resources/...` has an extra leading dot, so git still tracks the file.

- **Rotate the Neon password now.** Anyone who can read the repo has it, and deleting the file won't remove it from git history.
- Fix the `.gitignore` line, then run `git rm --cached` on the file.

## 2. Students can get credit for other groups' work

`Submission.countsFor()` checks who is in the group today, not who was in it when the work was handed in. Any student can join any group in their course (the `isMyEmail` rule in `GroupController`) and leave it again (`isMe`). So:

- A student without a group can join a group whose assignment has already been graded. They then get that grade in `my-grades` and can download the group's file (`canSeeSubmission` / `canSeeGrade`).
- A student who submitted for a group and then leaves it loses credit for their own submission.

**Fix:** record the members on the submission when it's handed in, and check that list instead of the live group. You could also stop students from changing groups once the group has a submission.

## 3. The quiz attempt limit can be bypassed

`SubmissionService.createSubmission` counts the student's attempts and then inserts a new one, with nothing stopping two requests from interleaving. If a student sends several attempts at the same moment, they all pass the check and go over `maxAttempts`. Once a student's attempts run out, the results show the correct answers, so this also helps with cheating.

**Fix:** lock the row per student and quiz (`SELECT … FOR UPDATE` or a `@Lock` on the count query), or add a unique constraint on the attempt number.

## 4. Course membership gaps

- **A course can lose its last teacher.** `CourseService.unenroll` lets a teacher remove themselves even if they're the only one. After that, nobody can edit or delete the course. Block that case, the way `AccountService` already stops the last admin removing themselves.
- **Removed students stay in their group.** Unenrolling a student doesn't take them out of their group, so group submissions still count for them.

## 5. Deployment and configuration

- **Demo data is on by default** (`DEMO_DATA:true`). Any fresh deployment gets two TEACHER accounts with the public password `password123`. The server checklist in `DEPLOYMENT.md` doesn't tell people to set `DEMO_DATA=false`. Add that line, or make the default `false`.
- **`ddl-auto=update` is risky outside development.** Hibernate never drops or renames columns, and it can make surprising changes to the schema. Flyway or Liquibase would be safer.
- **`compose.yaml` contradicts itself on MongoDB.** The comment says the project uses 7.0 on purpose because of a kernel bug, but the image is `mongo:9.0`. One of them is wrong.
- **CI doesn't run the same tests as local builds.** `.gitlab-ci.yml` sets `SPRING_DATASOURCE_*` environment variables, which override the H2 settings in the test config. So CI runs the tests against Postgres, while locally they use H2. CI also never builds or lints the frontend.

## 6. Repository hygiene

The repo tracks files that should not be in git:

| Path | What it is |
| --- | --- |
| `frontend/node_modules` | 3,689 files, including native binaries built for macOS on ARM (`darwin-arm64`) |
| `out/production/...` | Compiled `.class` files from IntelliJ |
| `.idea/`, `data/*.mv.db` | IDE settings and a local H2 database file; some are already in `.gitignore` but were committed before that |

Run `git rm -r --cached` on these paths and add `node_modules/` and `out/` to `.gitignore`.

## 7. Smaller issues

- **Anyone logged in can look up anyone's email.** `GET /api/users/{id}` has no permission check, so a script could collect every user's email by stepping through the ids. Also, `register` returns 409 for an email that's already taken, which undoes the effort `AuthService` makes to hide which emails exist.
- **WebSocket logins don't expire.** `StompAuthInterceptor` checks the token only when the connection opens, so an open connection keeps receiving updates after the token expires. Acceptable for games that last minutes, but worth knowing.
- **Some timestamps ignore the injected clock.** `CourseService.enroll` and `NotificationService.notify` call `Instant.now()` instead of the injected `Clock`, so tests that control time won't affect them.
- **The gradebook will get slow.** `GradingService.gradebook` scans every submission in the course for each student and item, so the work grows as students × items × submissions. Fine at demo size; grouping submissions by item first would fix it.
- **Downloads may fail in some browsers.** In `api.js`, `download()` calls `revokeObjectURL` right after `click()`, which can cancel the download in Firefox. Delaying it with `setTimeout(…, 0)` is enough.
- **Leftover names and comments.** `spring.application.name=myapp`, the jar is `myapp.jar`, and the comments in `.gitlab-ci.yml` are in German while the rest of the project is in English.

## Next steps

- [ ] Rotate the Neon password, fix the `.gitignore` typo and untrack the file
- [ ] Record group members on each submission instead of checking the live group
- [ ] Lock the attempt count so the quiz limit holds under concurrent requests
- [ ] Stop the last teacher leaving a course, and remove unenrolled students from groups
- [ ] Untrack `node_modules`, `out/`, `.idea/` and `data/`
- [ ] Add tests for each of the fixes above
