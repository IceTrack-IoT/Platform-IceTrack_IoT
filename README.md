# IceTrack Platform API

> Spring Boot REST API for ice tracking — Java 26 · Spring Boot 4.1.1 · PostgreSQL 18 · Docker-ready.

[![Java](https://img.shields.io/badge/Java-26-orange?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1.1-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18-336791?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://docs.docker.com/compose/)
[![Swagger](https://img.shields.io/badge/Swagger-OpenAPI-85EA2D?style=for-the-badge&logo=swagger&logoColor=black)](https://swagger.io/)

Exposes versioned resources under `/api/v1/*` (e.g. `/api/v1/authentication`, `/api/v1/users`, `/api/v1/profiles/owners`, `/api/v1/profiles/technicians`) with OpenAPI/Swagger docs out of the box.

---

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Services (Compose)](#services-compose)
- [Configuration](#configuration)
- [Run locally with Docker](#run-locally-with-docker)
- [Access](#access)
- [Useful commands](#useful-commands)
- [Troubleshooting](#troubleshooting)

---

## Features

- **Auth built-in** — local sign-up/sign-in + Google OIDC deferred registration (`/google/verify` → `/google/complete-registration/{owner|technician}`), role explicit local sign-up (`/sign-up/{owner|technician}`), JWT bearer auth with a `role` claim (JJWT 0.13.0), rotating refresh tokens (`/refresh-token`), `/logout` and `/me`
- **Immutable roles** — every account is `OWNER_ROLE` or `TECHNICIAN_ROLE`, chosen once at registration, and gets its `OwnerProfile` / `TechnicianProfile` in the same transaction
- **IAM + Profiles** bounded contexts (`/api/v1/authentication`, `/api/v1/users`, `/api/v1/roles`, `/api/v1/profiles/owners`, `/api/v1/profiles/technicians`)
- **OpenAPI first** — springdoc-openapi 3.1.1 with Swagger UI
- **Postgres persistence** — Spring Data JPA/Hibernate with custom snake-case + pluralized naming strategy
- **Docker-first dev loop** — multi-stage build, Compose healthchecks, dev/prod overlays

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 26 |
| Framework | Spring Boot 4.1.1 (Web, Data JPA, Validation, Security, OAuth2 Resource Server) |
| Database | `postgres:18-alpine` (database `icetrack`) |
| Build | Maven 3.9 (`mvnw` wrapper included) |
| Runtime | Multi-stage `Dockerfile` (Maven 3.9 + Temurin 26 build → Temurin 26 JRE) |
| Docs | springdoc-openapi 3.1.1 |

## Prerequisites

- [ ] Docker Desktop (or Docker Engine + Compose v2)
- [ ] Ports `8080` (API) and `5432` (Postgres) free
- [ ] Files in project root: `Dockerfile`, `docker-compose.yml`, `docker-compose.dev.yml`, `.env`

## Services (Compose)

Base file `docker-compose.yml` defines:

- `db` — Postgres 18, `POSTGRES_USER=postgres`, `POSTGRES_PASSWORD=postgres`, `POSTGRES_DB=icetrack`, with `pg_isready` healthcheck. Named volume `pgdata`.
- `app` — built from `.` (`Dockerfile`), waits for `db` healthy (`service_healthy`).

Dev overlay `docker-compose.dev.yml` adds:

- `db` — `5432:5432`, volume `pgdata:/var/lib/postgresql`
- `app` — `8080:8080`, `env_file: .env`, `SPRING_PROFILES_ACTIVE=dev`

> Production uses `docker-compose.prod.yml` instead (`SPRING_PROFILES_ACTIVE=prod`, host-mounted volume, `restart: unless-stopped`).

## Configuration

Defaults come from `src/main/resources/application.properties`:

| Variable | Default | Description |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/icetrack` | JDBC URL (override in `.env` for Docker networking, e.g. `jdbc:postgresql://db:5432/icetrack`) |
| `SPRING_DATASOURCE_USERNAME` | `postgres` | DB user |
| `SPRING_DATASOURCE_PASSWORD` | `postgres` | DB password |
| `SPRING_PROFILES_ACTIVE` | `dev` | Set to `dev` by `docker-compose.dev.yml` |
| `JWT_SECRET` | dev-only default | Internal bearer-token signing key |
| `JWT_EXPIRATION_MINUTES` | `30` | Access token TTL |
| `REFRESH_TOKEN_EXPIRATION_DAYS` | `7` | Refresh token TTL (each refresh rotates the token) |
| `REFRESH_TOKEN_REUSE_GRACE_SECONDS` | `10` | Window in which re-presenting a just-rotated refresh token is a concurrent refresh (401 only) instead of a replay (revokes all sessions); `0` disables it |
| `REFRESH_TOKEN_PURGE_CRON` | `0 0 3 * * *` | Schedule of the purge of expired refresh tokens (Spring cron) |
| `GOOGLE_CLIENT_ID` | `google-client-id` | Google OIDC client ID for `id_token` validation |

`server.port` is `8080`. Hibernate `ddl-auto` is `update`.

## Run locally with Docker

From the project root:

```bash
docker compose -f docker-compose.yml -f docker-compose.dev.yml up
```

> First run builds the JAR inside Docker (`mvn clean package -DskipTests`), so it takes several minutes.

Run detached:

```bash
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d
```

Rebuild after code changes:

```bash
docker compose -f docker-compose.yml -f docker-compose.dev.yml up --build
```

## Access

| Service | URL / Address |
|---|---|
| API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html (OpenAPI JSON at `/v3/api-docs`) |
| Postgres | `localhost:5432`, db `icetrack`, user/password `postgres`/`postgres` |

## Useful commands

```bash
# Logs (all services)
docker compose -f docker-compose.yml -f docker-compose.dev.yml logs -f

# Logs (app only)
docker compose -f docker-compose.yml -f docker-compose.dev.yml logs -f app

# Stop (keep data)
docker compose -f docker-compose.yml -f docker-compose.dev.yml down

# Stop and delete DB data (pgdata volume)
docker compose -f docker-compose.yml -f docker-compose.dev.yml down -v
```

## Troubleshooting

| Symptom | Fix |
|---|---|
| App can't reach DB (`Connection to localhost:5432 refused` inside container) | Set `SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/icetrack` in `.env`. Inside Compose, the DB host is `db`, not `localhost`. |
| Ports already in use | Stop local Postgres/backend on `5432`/`8080`, or edit the `ports` mappings in `docker-compose.dev.yml`. |
| Stale build | Use `up --build` to force a Maven rebuild. |

---