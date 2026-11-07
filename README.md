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
- [Edge devices](#edge-devices)
- [Run locally with Docker](#run-locally-with-docker)
- [Access](#access)
- [Useful commands](#useful-commands)
- [Troubleshooting](#troubleshooting)

---

## Features

- **Auth built-in** — local sign-up/sign-in + Google OIDC deferred registration (`/google/verify` → `/google/complete-registration/{owner|technician}`), role explicit local sign-up (`/sign-up/{owner|technician}`), JWT bearer auth with a `role` claim (JJWT 0.13.0), rotating refresh tokens (`/refresh-token`), `/logout` and `/me`
- **Immutable roles** — every account is `OWNER_ROLE` or `TECHNICIAN_ROLE`, chosen once at registration, and gets its `OwnerProfile` / `TechnicianProfile` in the same transaction
- **IAM + Profiles** bounded contexts (`/api/v1/authentication`, `/api/v1/users`, `/api/v1/roles`, `/api/v1/profiles/owners`, `/api/v1/profiles/technicians`)
- **Edge device authentication** — IoT boards call the edge routes (`POST /api/v1/telemetry`, `POST /api/v1/telemetry/events`, `/api/v1/devices/me/**`) with an `X-API-Key` header and no user JWT; see [Edge devices](#edge-devices)
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
| `SPRING_DATASOURCE_PASSWORD` | none (required) | DB password; startup fails without it |
| `SPRING_PROFILES_ACTIVE` | `dev` | Set to `dev` by `docker-compose.dev.yml` |
| `JWT_SECRET` | none (required) | Base64-encoded HS512 signing key of at least 64 decoded bytes, e.g. `openssl rand -base64 64`; startup fails when it is missing, blank, not Base64 or too short |
| `JWT_EXPIRATION_MINUTES` | `30` | Access token TTL |
| `REFRESH_TOKEN_EXPIRATION_DAYS` | `7` | Refresh token TTL (each refresh rotates the token) |
| `REFRESH_TOKEN_REUSE_GRACE_SECONDS` | `20` | Window in which re-presenting a just-rotated refresh token is a concurrent refresh (`401 REFRESH_TOKEN_RECENTLY_ROTATED`, retry once) instead of a replay (`401 REFRESH_TOKEN_REPLAY_DETECTED`, revokes all sessions); `0` disables it |
| `REFRESH_TOKEN_PURGE_CRON` | `0 0 3 * * *` | Schedule of the purge of expired refresh tokens (Spring cron) |
| `GOOGLE_CLIENT_ID` | `google-client-id` | Google OIDC client ID for `id_token` validation |

`server.port` is `8080`. Hibernate `ddl-auto` is `update`.

## Edge devices

Two separate security filter chains guard the API:

| Chain | Routes | Credential |
|---|---|---|
| Edge | `POST /api/v1/telemetry`, `POST /api/v1/telemetry/events`, `/api/v1/devices/me/**` | `X-API-Key: it_dev_<64 hex>` only; a user JWT is not accepted |
| User | everything else | `Authorization: Bearer <JWT>` |

Only `POST /api/v1/telemetry` has a controller today; the other two edge routes are already claimed by the edge chain so their future endpoints are key-protected from the start. A missing, unknown, revoked or malformed key answers `401`. The key identifies the device and its equipment, so telemetry is stored under the identifiers the key is paired with. `equipment_id` / `device_id` in the body are optional and, when sent, must match (`403` otherwise). There is no device authentication endpoint.

**Device lifecycle** (`/api/v1/devices`, owner JWT):

| Action | Endpoint | Result |
|---|---|---|
| Pair | `POST /api/v1/devices` | `201` with the API key, shown only once. `404` if the equipment is not the caller's; `409` "Device already paired" if the board is linked to an equipment, or "Equipment already has a paired device" |
| Rotate key | `POST /{deviceId}/rotate-api-key` | `200` with a new key; the old one stops working. Also how a `REVOKED` device resumes |
| Revoke | `PUT /{deviceId}/status` `{"new_status": "REVOKED"}` | Key invalidated immediately; device stays linked to its equipment |
| Unpair | `POST /{deviceId}/unpair` | Equipment link and key cleared; both the board and the equipment can be paired again |

Statuses are `UNPAIRED`, `PAIRED` and `REVOKED`. Keys are stored only as SHA-256 digests (unique index on `devices.api_key_digest`).

**Telemetry ingestion** (`POST /api/v1/telemetry`) answers `201` for a new batch and `200` with the stored reading when the same `reading_uid` is resubmitted.

Each batch carries temperatures twice:

- `raw_min/max/avg_temperature`: what the DHT11 measured.
- `min/max/avg_temperature`: the raw values corrected by the calibration offset the edge applied. Thresholds are evaluated on these.

Both sets are stored. Optional fields are `light_level_max` (0–100 %), `door_open_seconds` and `presence_detected`; an omitted presence is stored as unavailable (`null`), never as `false`.

Any of the following is rejected with `400` "Invalid reading payload" and nothing is stored:

- a raw temperature outside 0–50 °C, or a humidity outside 20–90 % (DHT11 range)
- a calibrated temperature more than 5 °C away from its raw value
- `min <= avg <= max` not holding, for either set
- `light_level_max` outside 0–100, or a negative `door_open_seconds`
- `sample_count` outside 1–360
- a missing `applied_config_version`
- a non-numeric value

**Equipment rules:** thresholds must lie within 0–30 °C with `min_celsius < max_celsius`. Types are `DISPLAY_CASE`, `COLD_ROOM`, `REFRIGERATOR` and `OTHER`. Statuses are `ACTIVE`, `MAINTENANCE`, `REPAIR` and `DEACTIVATED`, with these transitions:

- `ACTIVE` → `MAINTENANCE` / `REPAIR` / `DEACTIVATED`
- `MAINTENANCE` → `ACTIVE` / `REPAIR` / `DEACTIVATED`
- `REPAIR` → `ACTIVE` / `DEACTIVATED`
- `DEACTIVATED` → `ACTIVE`

## Run locally with Docker

Create the network:

```bash
docker network create ice-track-net
```

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