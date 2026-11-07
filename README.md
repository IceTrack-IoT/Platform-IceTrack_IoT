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
- [User authentication](#user-authentication)
- [Alerts and notifications](#alerts-and-notifications)
- [Edge devices](#edge-devices)
- [Edge configuration, events and grants](#edge-configuration-events-and-grants)
- [Run locally with Docker](#run-locally-with-docker)
- [Access](#access)
- [Useful commands](#useful-commands)
- [Troubleshooting](#troubleshooting)

---

## Features

- **Auth built-in** — local sign-up/sign-in + Google OIDC deferred registration (`/google/verify` → `/google/complete-registration/{owner|technician}`), explicit Google account linking (`/google/link`), role explicit local sign-up (`/sign-up/{owner|technician}`), JWT bearer auth with a `role` claim (JJWT 0.13.0), rotating refresh tokens (`/refresh-token`), `/logout` and `/me`; see [User authentication](#user-authentication)
- **Role-channel authorization** — owners sign in only through the `WEB` channel and technicians only through `MOBILE`; uniform `401` on bad credentials and per-address/per-account sign-in throttling (`429`)
- **Alert lifecycle** — `OPEN` → `ACKNOWLEDGED` → `RESOLVED` / `DISMISSED`, recording who did each transition and when; dismissal requires a reason; see [Alerts and notifications](#alerts-and-notifications)
- **Immutable roles** — every account is `OWNER_ROLE` or `TECHNICIAN_ROLE`, chosen once at registration, and gets its `OwnerProfile` / `TechnicianProfile` in the same transaction
- **IAM + Profiles** bounded contexts (`/api/v1/authentication`, `/api/v1/users`, `/api/v1/roles`, `/api/v1/profiles/owners`, `/api/v1/profiles/technicians`)
- **Edge device authentication** — IoT boards call the edge routes (`POST /api/v1/telemetry`, `POST /api/v1/telemetry/events`, `/api/v1/devices/me/**`) with an `X-API-Key` header and no user JWT; see [Edge devices](#edge-devices)
- **Edge configuration and grants** — gateways poll `GET /api/v1/devices/me/config` (ETag / `304`) for thresholds, alert policy and technician edge grants; excursion events open and resolve alerts; see [Edge configuration, events and grants](#edge-configuration-events-and-grants)
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
| `SIGN_IN_RATE_LIMIT_MAX_FAILURES` | `5` | `401` answers a client address or an account may collect within one window before sign-in answers `429` |
| `SIGN_IN_RATE_LIMIT_WINDOW_MINUTES` | `5` | Length of the sign-in throttling window |
| `EDGE_GRANT_TTL_HOURS` | `24` | Lifetime of the edge grant issued when a technician accepts a service request |

`server.port` is `8080`. Hibernate `ddl-auto` is `update`.

## User authentication

All endpoints live under `/api/v1/authentication`. Errors use the standard `{code, message, details}` body.

**Client channel.** `sign-in/local`, `google/verify` and `google/link` require `"channel": "WEB" | "MOBILE"`. `WEB` admits only `OWNER_ROLE`, `MOBILE` only `TECHNICIAN_ROLE`. Valid credentials on the wrong channel answer `403 ROLE_CHANNEL_MISMATCH` (the message names the application to use) and issue no token. The channel is checked after the credentials, so it never reveals an account's role to someone without its password. A session keeps its channel: `/refresh-token` accepts an optional `channel`, and one that differs from the session's answers `403 ROLE_CHANNEL_MISMATCH` without consuming the refresh token. `google/complete-registration/owner` implies `WEB` and `/technician` implies `MOBILE`, so an existing Google account of the other role gets `403` there too.

**Local sign-in** (`POST /sign-in/local`, body `username`, `password`, `channel`). `username` accepts a username or the profile's email (matched case-insensitively). An unknown account, a Google-only account and a wrong password all answer the same `401 BAD_CREDENTIALS` ("Invalid username, email, or password"). When no stored hash exists, the password is still compared against a dummy hash of the same cost, so response time does not reveal whether an account exists. Missing fields answer `400 VALIDATION_ERROR` naming the field.

**Google** (`POST /google/verify`, body `id_token`, `channel`):

| Situation | Answer |
|---|---|
| Token forged, expired, malformed, wrong audience, or email not verified | `401 INVALID_TOKEN` ("Unable to authenticate with Google"); the same on `/google/link` and `/google/complete-registration/*` |
| Google identity already bound to an account | `200` with tokens (subject to the channel) |
| Verified email unknown to the platform | `404 GOOGLE_ACCOUNT_NOT_FOUND`; complete the onboarding |
| Verified email belongs to an account registered with a password | `409 ACCOUNT_LINK_REQUIRED`; never linked silently |

`POST /google/link` (body `id_token`, `password`, `channel`) links the Google identity to the password account whose profile uses the verified Google email, after checking that account's password (`401 BAD_CREDENTIALS` otherwise), then signs it in. An email Google has not verified is never matched against existing accounts. A linked account keeps its password and can still sign in locally.

**Email normalization.** Emails are trimmed and lowercased at sign-up, at sign-in and from Google, and by the profiles `EmailAddress` value object, so `Ana@x.com` and `ana@x.com` are the same account. Rows stored before this change keep their original case; normalize them once with `UPDATE profiles SET email_address = lower(trim(email_address));` (check first that no two rows collide).

**Throttling.** `sign-in/local`, `google/verify` and `google/link` count every `401` against the client address, and `sign-in/local` also against the submitted username or email. After `SIGN_IN_RATE_LIMIT_MAX_FAILURES` failures within `SIGN_IN_RATE_LIMIT_WINDOW_MINUTES`, further requests answer `429 TOO_MANY_REQUESTS` with a `Retry-After` header until the window ends. A successful sign-in clears the account's count, not the address's. Counters are in memory, per instance. Behind a reverse proxy, set `server.forward-headers-strategy` so the client address is not the proxy's.

## Alerts and notifications

**Alert lifecycle** (`/api/v1/alerts`, owner JWT for transitions):

| Transition | Endpoint | Allowed from | Records |
|---|---|---|---|
| Acknowledge | `PUT /{alertId}/acknowledge` | `OPEN` | `acknowledged_by`, `acknowledged_at` |
| Resolve | `PUT /{alertId}/resolve` | `OPEN`, `ACKNOWLEDGED` | `resolved_by`, `resolved_at` |
| Dismiss | `PUT /{alertId}/dismiss`, body `{"reason": "...", "evidence": "..."}` | `OPEN`, `ACKNOWLEDGED` | `dismissed_by`, `dismissed_at`, `dismiss_reason`, `dismiss_evidence` |

The actor is always the authenticated caller (IAM user id). `reason` is mandatory, at most 500 characters; `evidence` is optional, at most 1000. A missing or blank reason answers `400 VALIDATION_ERROR`. A transition the status forbids answers `409 INVALID_STATUS_TRANSITION`; an alert the caller does not own answers `404 ALERT_NOT_FOUND`. An alert resolved automatically, because readings returned within range, has `resolved_by: null`. Dismissed alerts are excluded from the thermal excursion report figures.

Thermal excursion alerts are opened only by the edge gateway's `THERMAL_EXCURSION_DETECTED` event (see [Edge configuration, events and grants](#edge-configuration-events-and-grants)), with severity `CRITICAL`, never by a single reading batch. Every alert response also carries the triggering reading (`triggering_reading_id`, `triggering_temperature`, `triggering_recorded_at`) and `total_duration_seconds` once closed.

**Notifications** (`/api/v1/notifications`, the recipient is always the authenticated caller):

- `GET /api/v1/notifications?page=0&size=20&unreadOnly=false` returns `{items, page, size, total_elements, total_pages}`, most recent first. Dismissed notifications are never listed. `size` defaults to `20` and is capped at `100`; a negative `page` or a `size` below `1` answers `400`.
- `GET /api/v1/notifications/unread-count` returns `{"unread_count": 7}`: notifications neither read nor dismissed.

## Edge devices

Two separate security filter chains guard the API:

| Chain | Routes | Credential |
|---|---|---|
| Edge | `POST /api/v1/telemetry`, `POST /api/v1/telemetry/events`, `/api/v1/devices/me/**` | `X-API-Key: it_dev_<64 hex>` only; a user JWT is not accepted |
| User | everything else | `Authorization: Bearer <JWT>` |

The edge endpoints are `POST /api/v1/telemetry`, `POST /api/v1/telemetry/events` and `GET /api/v1/devices/me/config`. A missing, unknown, revoked or malformed key answers `401`. The key identifies the device and its equipment, so telemetry is stored under the identifiers the key is paired with. `equipment_id` / `device_id` in the body are optional and, when sent, must match (`403` otherwise). There is no device authentication endpoint.

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

## Edge configuration, events and grants

**Configuration version.** Every equipment has a `config_version` (returned in `EquipmentResponse`), starting at `1`. It goes up by one, atomically, whenever anything the gateway enforces changes:

- the threshold (`PUT /api/v1/assets/equipments/{id}/threshold`, whose response already carries the new version);
- the unit's alert policy;
- an edge grant of one of its service requests being issued or revoked.

**Configuration poll** (`GET /api/v1/devices/me/config`, `X-API-Key`): returns `config_version`, `min_celsius`, `max_celsius`, `sustained_excursion_minutes`, `hysteresis_margin_celsius`, `missed_sync_windows_for_offline` and `grants` (`sha256`, `technician_id`, `service_request_id`, `expires_at`) for the unit the key is paired with. The `ETag` header is `"<config_version>"`; sending it back in `If-None-Match` answers `304` with an empty body while nothing changed. `404` when the paired unit no longer exists.

**Edge grants** (`it_grt_` + 64 hex characters; only the SHA-256 digest is stored):

| Event | Effect |
|---|---|
| Technician accepts (`PUT /api/v1/service-requests/{id}/accept`) | A grant is issued; the response carries `edge_grant.token`, shown only there |
| Owner reassigns, owner cancels, technician completes the intervention | The request's grants are revoked |
| Technician regenerates (`POST /api/v1/service-requests/{id}/edge-grant`) | Previous grant revoked, a new one issued and returned once. Assigned technician only (`403` otherwise), request `ACCEPTED` or `IN_PROGRESS` (`409` otherwise) |

Grants expire after `EDGE_GRANT_TTL_HOURS`. Every issuance and revocation moves the unit's `config_version`, so the gateway learns about it on its next poll.

**Edge events** (`POST /api/v1/telemetry/events`, `X-API-Key`), idempotent by `event_uid`:

| `type` | Result | Status |
|---|---|---|
| `THERMAL_EXCURSION_DETECTED` (requires `temperature_celsius`) | Opens a `CRITICAL` alert dated at `occurred_at`; `reading_uid`, when sent and stored for this unit, becomes the triggering reading | `201` `ALERT_OPENED` |
| `THERMAL_EXCURSION_DETECTED` while an alert is `OPEN` or `ACKNOWLEDGED` | Nothing opened | `200` `ALERT_ALREADY_ACTIVE` |
| `THERMAL_EXCURSION_NORMALIZED` (optional `peak_temperature_celsius`) | Resolves the active alert, recording peak and total duration | `200` `ALERT_RESOLVED` |
| `THERMAL_EXCURSION_NORMALIZED` with no active alert | Nothing changes | `200` `NO_ACTIVE_ALERT` |

A resent `event_uid` answers `200` with the stored outcome; one already used by another device answers `409`. Reading batches no longer open alerts. They only resolve the active one as a fallback for a lost normalization event, and only once every batch of the last `sustained_excursion_minutes` (at least one per minute, all recorded after the excursion began) lies back within range by `hysteresis_margin_celsius` on the breached side.

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