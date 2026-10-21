# Ideal LMS – frontend

React + Vite single-page app for the LMS. It talks to the Spring Boot backend through its REST API.

## Running it during development

You need two terminals:

```bash
# 1. Backend (from the project root) – http://localhost:8080
./mvnw spring-boot:run

# 2. Frontend (from this folder) – http://localhost:5173
npm install      # first time only
npm run dev
```

Open http://localhost:5173. The Vite dev server forwards every `/api/...` request to
Spring Boot on port 8080 (see `vite.config.js`), so the browser only talks to one origin.

The backend uses an in-memory H2 database, so all data is gone when Spring Boot restarts:
register again after a restart.

## Trying it out

1. Register a teacher account and create a course.
2. In a second browser window (or a private window), register a student account.
3. As the teacher: *Participants* → invite the student's e-mail. Add modules and items in *Course*.
4. As the student: open the course, open an assignment and hand in.
5. As the teacher: open the assignment, grade the submission, tick *Grades visible to students*,
   and look at *Grades* for the gradebook.

## Structure

```
src/
  api.js              all REST calls; keeps the login token (JWT) and sends it with each request
  useApiData.js       hook for loading data when a component appears (with reload())
  format.js           display helpers (dates, content types, status labels)
  Home.jsx            top level: Login → Dashboard → Course
  pages/
    Login.jsx         log in / create account
    Dashboard.jsx     your courses (teaching / studying), create a course
    Course.jsx        one course, with the tabs below
  components/
    CourseContent.jsx   modules and their items; teachers add/delete items and module requirements
    AddContentForm.jsx  teacher form for pages, quizzes, coding exercises and assignments
    ContentDetail.jsx   one item: students hand in; teachers grade and publish grades
    Participants.jsx    who is in the course; teachers invite by e-mail
    Grades.jsx          gradebook (teachers) or "my grades" (students)
    Groups.jsx          groups for group work; students can join one themselves
```

What a user sees depends on their **role in that course** (roles are per course), which comes
from their enrollment. The backend enforces the same rules, so hiding a button is only for
convenience, never the security.

## Login token

After login the backend returns a JWT. `api.js` keeps it in `sessionStorage` (survives a page
reload, cleared when the tab closes) and sends it as `Authorization: Bearer <token>`.
If the backend answers 401 (e.g. the token expired), the app returns to the login page.

Known trade-off: a token in `sessionStorage` can be read by JavaScript on the page, so an XSS bug
could leak it. An `HttpOnly` cookie set by the backend would be safer.

## Other commands

```bash
npm run lint     # ESLint
npm run build    # production build into dist/
```
