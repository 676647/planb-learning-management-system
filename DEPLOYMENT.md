# Running the whole LMS in Docker

One command builds and starts everything: the React frontend, the Spring Boot backend,
PostgreSQL, MongoDB and RabbitMQ.

```bash
docker compose up -d --build
```

Then open **http://localhost:8080**. The first build takes a few minutes, because it downloads
the Maven and npm dependencies and the base images. Later builds reuse the cached layers.

Stop with `docker compose down`. Your data stays in Docker volumes; `docker compose down -v`
deletes it as well.

> Port 8080 is used by the `web` container. Stop a backend you started from IntelliJ or
> `./mvnw spring-boot:run` first, or change `"8080:80"` in `compose.yaml` to e.g. `"8000:80"`.

## The containers

```
           browser
              │ :8080
         ┌────▼────┐   /api/*   ┌─────────┐
         │   web   ├───────────►│ backend │──► postgres (users, courses, submissions, grades, …)
         │ (nginx) │            │ (Spring)│──► mongo    (uploaded files, GridFS)
         └─────────┘            └─────────┘──► rabbitmq (domain events)
```

| Service | Image | Purpose |
|---|---|---|
| `web` | built from `frontend/Dockerfile` | nginx serving the built React app; forwards `/api/*` to `backend` |
| `backend` | built from `Dockerfile` | Spring Boot REST API |
| `postgres` | `postgres:17` | relational database |
| `mongo` | `mongo:8.0` | NoSQL store for uploaded files (GridFS) |
| `rabbitmq` | `rabbitmq:4-management` | message broker for domain events |

How it fits together:

- **One origin.** The browser only talks to `web`. nginx serves the app and passes every `/api/...`
  request on to the backend, the same way the Vite dev server does during development. So there
  are no CORS issues, and the backend doesn't need to be reachable from outside. Third-party API
  clients use the same address: `http://localhost:8080/api/...`.
- **Multi-stage builds.** Both images are built in two steps: first with the full build tools
  (Maven or Node), then only the result is copied into a small runtime image (a Java runtime, or
  nginx). The final images contain no source code and no build tools, and the backend doesn't run
  as root.
- **Start order.** PostgreSQL, MongoDB and RabbitMQ have health checks. The backend only starts
  once all three report healthy, so it never starts against a database that isn't ready yet.
- **Configuration from outside.** The backend gets its connection details as environment variables
  (`DB_URL`, `MONGODB_URI`, `RABBITMQ_HOST`, …). It's the same image everywhere; only the settings
  differ.

## Useful commands

```bash
docker compose ps                    # what is running, and is it healthy?
docker compose logs -f backend       # follow the backend's log
docker compose up -d --build backend # rebuild and restart only the backend after a code change
docker compose exec postgres psql -U lms lms   # look into the database
```

RabbitMQ's management UI is at http://localhost:15672 (login `lms` / `lms`).

## Before a real deployment

The passwords in `compose.yaml` are for development only. On a server:

- Put a long random `JWT_SECRET` in a `.env` file next to `compose.yaml` (`compose.yaml` already
  reads it). Generate one with `openssl rand -base64 48`.
- Change the PostgreSQL and RabbitMQ passwords, in their own service and in the `backend` settings.
- Serve the site over HTTPS, for example with a reverse proxy such as Caddy or Traefik in front of
  `web`.

## Development without building images

During development it's faster to run only the services in Docker and start the backend and
frontend yourself, with live reloading:

```bash
docker compose up -d postgres mongo rabbitmq
./mvnw spring-boot:run              # Windows: .\mvnw.cmd spring-boot:run
cd frontend && npm run dev          # http://localhost:5173
```
