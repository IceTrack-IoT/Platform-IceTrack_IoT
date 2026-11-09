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
- [API conventions](#api-conventions)
- [User authentication](#user-authentication)
- [Alerts and notifications](#alerts-and-notifications)
- [Edge devices](#edge-devices)
- [Edge configuration, events and grants](#edge-configuration-events-and-grants)
- [Maintenance windows](#maintenance-windows)
- [Sites, address verification and reports](#sites-address-verification-and-reports)
- [Real-time telemetry stream](#real-time-telemetry-stream)
- [Integration events and technician metrics](#integration-events-and-technician-metrics)
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
- **Maintenance windows** — gateway-reported windows put the unit in `MAINTENANCE`, suppress excursion alerts for the instants they cover, link to the service request in progress and are audited with `GET /api/v1/maintenance-windows`; see [Maintenance windows](#maintenance-windows)
- **Transactional outbox and technician metrics** — service request events are delivered at least once to a technician metrics read model and to notifications; owners read `GET /api/v1/profiles/technicians/{id}/public`; see [Integration events and technician metrics](#integration-events-and-technician-metrics)
- **Real-time telemetry stream** — owners receive readings and alert status changes of their own units over Server-Sent Events, opened with a single-use stream ticket; see [Real-time telemetry stream](#real-time-telemetry-stream)
- **OpenAPI first** — springdoc-openapi 3.1.1 with Swagger UI; every operation documents the standard error body for `400`, `401`, `429`, `500` and `503`; see [API conventions](#api-conventions)
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
| `JWT_SECRET` | none (required) | Base64-encoded HS512 signing key of at least 64 decoded bytes, e.g. `openssl rand -base64 64 \| tr -d '\n'` (the command wraps its output, so without stripping the line break only the first line reaches `.env` and the key is too short); startup fails when it is missing, blank, not Base64 or too short |
| `JWT_EXPIRATION_MINUTES` | `30` | Access token TTL |
| `REFRESH_TOKEN_EXPIRATION_DAYS` | `7` | Refresh token TTL (each refresh rotates the token) |
| `REFRESH_TOKEN_REUSE_GRACE_SECONDS` | `20` | Window in which re-presenting a just-rotated refresh token is a concurrent refresh (`401 REFRESH_TOKEN_RECENTLY_ROTATED`, retry once) instead of a replay (`401 REFRESH_TOKEN_REPLAY_DETECTED`, revokes all sessions); `0` disables it |
| `REFRESH_TOKEN_PURGE_CRON` | `0 0 3 * * *` | Schedule of the purge of expired refresh tokens (Spring cron) |
| `GOOGLE_CLIENT_ID` | `google-client-id` | Google OIDC client ID for `id_token` validation |
| `SIGN_IN_RATE_LIMIT_MAX_FAILURES` | `5` | `401` answers a client address or an account may collect within one window before sign-in answers `429` |
| `SIGN_IN_RATE_LIMIT_WINDOW_MINUTES` | `5` | Length of the sign-in throttling window |
| `EDGE_GRANT_TTL_HOURS` | `24` | Lifetime of the edge grant issued when a technician accepts a service request |
| `MAINTENANCE_WINDOW_EXPIRY_GRACE_MINUTES` | `30` | Minutes after its expiration an open maintenance window is closed by the platform as `EXPIRED`, when its gateway never reported the end |
| `MAINTENANCE_WINDOW_EXPIRY_SWEEP_MS` | `60000` | Interval of the sweep that closes those overdue windows |
| `OUTBOX_DISPATCH_DELAY_MS` | `2000` | Interval at which the transactional outbox delivers pending integration events |
| `ADDRESS_VALIDATION_SECRET` | none (required) | Base64-encoded key, at least 32 decoded bytes, that signs address validation tokens (`openssl rand -base64 32`); startup fails when it is missing, not Base64 or too short. It is separate from `JWT_SECRET` |
| `GOOGLE_MAPS_API_KEY` | `YOUR_MAPS_DEMO_KEY` | Google Maps Geocoding key; with the placeholder every address validation answers `503` |
| `OUTBOX_MAX_ATTEMPTS` | `10` | Failed deliveries after which an outbox event is no longer tried (it stays in `outbox_events`, unpublished) |
| `TELEMETRY_STREAM_MAX_PENDING_TICKETS_PER_OWNER` | `10` | Unredeemed stream tickets one owner may hold; one more answers `429` |
| `TELEMETRY_STREAM_MAX_PENDING_TICKETS` | `10000` | Stream tickets the instance keeps in total; one more answers `429` |
| `TELEMETRY_STREAM_MAX_PER_OWNER` | `10` | Telemetry streams one owner may keep open; opening one more closes the oldest |
| `TELEMETRY_STREAM_HEARTBEAT_MS` | `25000` | Interval of the heartbeat comment written to every open stream |
| `APP_TASK_POOL_CORE_SIZE` / `APP_TASK_POOL_MAX_SIZE` / `APP_TASK_POOL_QUEUE_CAPACITY` | `8` / `16` / `5000` | Default `@Async` pool (push notifications, maintenance window notices) |
| `TOMCAT_MAX_THREADS` / `TOMCAT_MAX_CONNECTIONS` | `200` / `8192` | Request threads and open connections; an open telemetry stream holds a connection, not a thread |
| `DB_POOL_MAX_SIZE` / `DB_CONNECTION_TIMEOUT_MS` | `10` / `5000` | Database connection pool size, and how long a request waits for a free connection before failing |

`server.port` is `8080`. Hibernate `ddl-auto` is `update`.

**Thread pools.** Readings are ingested on the request threads and hold a database connection for a few milliseconds. Slow work has its own bounded pool: the default `@Async` pool for push notifications, `reportTaskExecutor` (1 to 2 threads) for reports, and `streamTaskExecutor` (4 to 8 threads, queue of 1000) for every write to a telemetry stream. When the stream pool's queue is full, the oldest pending update is dropped with a warning. Two scheduler threads run the periodic jobs, so a slow outbox run never delays the stream heartbeat. These defaults are sized for 500 reading batches per minute with a p95 ingestion latency of at most 500 ms; the device API key check on every edge request is a SHA-256 digest looked up through the unique index on `devices.api_key_digest`, so it costs microseconds.

## API conventions

**Naming.** Every JSON field and every query parameter is `snake_case`. The query parameters below were renamed in this version, and the old names are no longer read, so clients must be updated with it:

| Endpoint | Old name | New name |
|---|---|---|
| `GET /api/v1/notifications` | `unreadOnly` | `unread_only` |
| `GET /api/v1/service-requests` | `assignedTo` | `assigned_to` |
| `GET /api/v1/assets/equipments` | `equipmentType`, `siteId` | `equipment_type`, `site_id` |
| `GET /api/v1/maintenance-windows` | `equipmentId` | `equipment_id` |

Path variables keep their names (`{equipmentId}`, `{siteId}`, ...): they only appear in the documentation, never on the wire.

**Errors.** Error answers carry the standard body `{"code": "...", "message": "...", "details": "..."}` (`ErrorResponse` in the OpenAPI document). The `401` of the edge routes is empty, and an endpoint documents its own body where it differs, such as the candidates of `422 ADDRESS_NOT_VERIFIED`. `message` is always present (a code without a message of its own falls back to a generic text), and validation `details` name the snake_case request properties (`Field min_celsius: ...`), sorted and without repetitions. The OpenAPI document declares the standard body for these codes on every operation, without per-method repetition:

| Code | Documented on |
|---|---|
| `400 VALIDATION_ERROR` | every operation taking parameters or a body; a required query parameter that is missing also answers `400` |
| `401 UNAUTHORIZED` | every operation that requires a JWT; a missing or invalid bearer token is now answered with the standard body instead of the container's default error page |
| `429 TOO_MANY_REQUESTS` | `sign-in/local`, `google/verify`, `google/link`, `validate-address` and `stream-ticket`, with a `Retry-After` header |
| `500 UNEXPECTED_ERROR` | every operation; `details` carries only a reference to the server log |
| `503 GEOCODING_UNAVAILABLE` | `validate-address`, with a `Retry-After` header |

Public routes (sign-in, sign-up, the Google token exchange, refresh, logout and the telemetry stream) are documented as requiring no security scheme.

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

**Password policy.** Local sign-up (`/sign-up/owner`, `/sign-up/technician`) requires a password of 8 to 64 characters with at least one uppercase letter, one lowercase letter and one digit, otherwise `400 VALIDATION_ERROR`. Sign-in does not check the policy, so existing accounts keep working.

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

**Notifications** (`/api/v1/notifications`, the recipient is always the authenticated caller, owner or technician):

- `GET /api/v1/notifications?page=0&size=20&unread_only=false` returns `{items, page, size, total_elements, total_pages}`, most recent first. Dismissed notifications are never listed. `size` defaults to `20` and is capped at `100`; a negative `page` or a `size` below `1` answers `400`.
- `GET /api/v1/notifications/unread-count` returns `{"unread_count": 7}`: notifications neither read nor dismissed.
- `PUT`/`DELETE /api/v1/notifications/device-tokens` register or remove the caller's FCM token; `403` only for an account with neither an owner nor a technician profile.

What creates a notification, and for whom:

| Source | Recipient | `type` / `severity` |
|---|---|---|
| Alert raised | Owner of the unit | `TEMPERATURE_EXCURSION` (or the alert type) / the alert severity |
| Technician assigned or reassigned | New technician (request, service type, priority, unit, site, date); the replaced technician, if any | `SERVICE_REQUEST_UPDATE` / `INFO` |
| Request rejected | Owner, with the technician's reason | `SERVICE_REQUEST_UPDATE` / `WARNING` |
| Request canceled | Technician assigned at the time, if any, with the owner's reason | `SERVICE_REQUEST_UPDATE` / `WARNING` |
| Maintenance window opened or closed | Owner of the unit (origin, times, order and technician when known, closure reason) | `MAINTENANCE_WINDOW` / `INFO` |
| Maintenance window opened with no service request | Owner of the unit, at once | `UNLINKED_MAINTENANCE_WINDOW` / `WARNING` |

Texts come from the `notification.*` keys of `messages.properties`. Profiles record no language preference yet, so every notification is composed in the default (English) bundle.

## Edge devices

Two separate security filter chains guard the API:

| Chain | Routes | Credential |
|---|---|---|
| Edge | `POST /api/v1/telemetry`, `POST /api/v1/telemetry/events`, `/api/v1/devices/me/**` | `X-API-Key: it_dev_<64 hex>` only; a user JWT is not accepted |
| User | everything else | `Authorization: Bearer <JWT>`; `GET /api/v1/telemetry/stream` takes a stream ticket instead (see [Real-time telemetry stream](#real-time-telemetry-stream)) |

The edge endpoints are `POST /api/v1/telemetry`, `POST /api/v1/telemetry/events` and `GET /api/v1/devices/me/config`. A missing, unknown, revoked or malformed key answers `401`. The key identifies the device and its equipment, so telemetry is stored under the identifiers the key is paired with. `equipment_id` / `device_id` in the body are optional and, when sent, must match (`403` otherwise). There is no device authentication endpoint.

**Device lifecycle** (`/api/v1/devices`, owner JWT):

| Action | Endpoint | Result |
|---|---|---|
| Pair | `POST /api/v1/devices` | `201` with the API key, shown only once. `404` if the equipment is not the caller's; `409` "Device already paired" if the board is linked to an equipment, or "Equipment already has a paired device" |
| Rotate key | `POST /{deviceId}/rotate-api-key` | `200` with a new key; the old one stops working. Also how a `REVOKED` device resumes |
| Revoke | `PUT /{deviceId}/status` `{"new_status": "REVOKED"}` | Key invalidated immediately; device stays linked to its equipment |
| Unpair | `POST /{deviceId}/unpair` | Equipment link and key cleared; both the board and the equipment can be paired again |

Statuses are `UNPAIRED`, `PAIRED` and `REVOKED`. Keys are stored only as SHA-256 digests (unique index on `devices.api_key_digest`).

**Telemetry ingestion** (`POST /api/v1/telemetry`) answers `201` for a new batch and `200` with the stored reading when the same `reading_uid` is resubmitted. Every new batch, in the same transaction, sets the unit `online` (unless `DEACTIVATED`) and moves its `last_reading_at` / `last_known_temperature` to the batch's `recorded_at` / `avg_temperature` (never backwards for a batch delivered late), sets the device's `last_seen_at` to the arrival time, and resolves the unit's active `DEVICE_OFFLINE` alert.

**Device offline detection.** A unit is offline once no reading arrived for `60 s × missed_sync_windows_for_offline` of its alert policy (3 minutes by default). `EquipmentConnectivityCommandService` marks such units offline and raises one `OPEN`, `WARNING` `DEVICE_OFFLINE` alert each (none during a maintenance window, never a second one while one is active), which notifies the owner. Nothing schedules the detection yet: the periodic trigger is pending, so until it is added no `DEVICE_OFFLINE` alert is raised at runtime and `EQUIPMENT_UPTIME` reports read 100 %.

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

`MAINTENANCE` is also entered and left automatically by maintenance windows (see [Maintenance windows](#maintenance-windows)): an `ACTIVE` unit moves to `MAINTENANCE` when a window opens and returns to `ACTIVE` when it closes. A unit `REPAIR` or `DEACTIVATED` keeps its status during a window, and a status the owner sets while a window is open is kept when the window closes.

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
| `THERMAL_EXCURSION_DETECTED` with `occurred_at` inside a maintenance window | Nothing opened; existing alerts untouched | `200` `SUPPRESSED_BY_MAINTENANCE` |
| `THERMAL_EXCURSION_NORMALIZED` (optional `peak_temperature_celsius`) | Resolves the active alert, recording peak and total duration | `200` `ALERT_RESOLVED` |
| `THERMAL_EXCURSION_NORMALIZED` with no active alert | Nothing changes | `200` `NO_ACTIVE_ALERT` |
| `MAINTENANCE_WINDOW_STARTED` (requires `origin`; optional `technician_id`, `service_request_id`) | Opens the unit's maintenance window at `occurred_at` | `201` `MAINTENANCE_WINDOW_OPENED` |
| `MAINTENANCE_WINDOW_STARTED` with `origin: APP` while a `BUTTON` window is active | The active window becomes `APP`, keeps its id, gains technician and order, expires 2 h after `occurred_at` | `200` `MAINTENANCE_WINDOW_PROMOTED` |
| `MAINTENANCE_WINDOW_STARTED` while any other window is active | The active window is kept | `200` `MAINTENANCE_ALREADY_ACTIVE` |
| `MAINTENANCE_WINDOW_ENDED` (requires `closure_reason`; optional `start_event_uid`) | Closes the window opened by `start_event_uid`, or the active one, at `occurred_at` | `201` `MAINTENANCE_WINDOW_CLOSED` |
| `MAINTENANCE_WINDOW_ENDED` with no matching open window | Nothing changes | `200` `NO_ACTIVE_MAINTENANCE` |

Every response carries `alert_id` and `maintenance_window_id` (each `null` when it does not apply). A resent `event_uid` answers `200` with the stored outcome; one already used by another device answers `409`. Reading batches no longer open alerts. They only resolve the active one as a fallback for a lost normalization event, and only once every batch of the last `sustained_excursion_minutes` (at least one per minute, all recorded after the excursion began, none taken during maintenance) lies back within range by `hysteresis_margin_celsius` on the breached side.

## Maintenance windows

A maintenance window is the period a unit is being worked on, as reported by its gateway with `MAINTENANCE_WINDOW_STARTED` / `MAINTENANCE_WINDOW_ENDED` events (see above). A unit has at most one active window, shared by the mobile app and the hardware button.

| | `APP` | `BUTTON` |
|---|---|---|
| Started by | The technician, from the mobile app with its edge grant | Pressing the gateway's button |
| Lifetime | 2 hours | 30 minutes |

Closure reasons are `MANUAL_APP`, `MANUAL_BUTTON`, `INTERVENTION_COMPLETED`, `EXPIRED` and `SAFETY_LIMIT`. Timestamps are the gateway's `occurred_at` (read in the server's zone), so a window covers what happened during it even when the events or readings arrive late from the gateway's offline buffer.

While a window covers an instant:

- a reading batch recorded then is stored with `under_maintenance: true` and changes no alert; it is also left out of the fallback resolution described above;
- a `THERMAL_EXCURSION_DETECTED` dated then opens no alert (`SUPPRESSED_BY_MAINTENANCE`); alerts already active are left as they are. The offline detection applies the same coverage check: a unit silent during a window is neither marked offline nor alerted.

Other effects:

- **Equipment status** — opening a window moves an `ACTIVE` unit to `MAINTENANCE`; closing it restores the previous status.
- **Order attribution** — a start without `service_request_id` is attributed to the service request that was `IN_PROGRESS` on the unit at that instant, and its technician, when there is one; otherwise the window is *unlinked* and the owner is notified at once.
- **Service request link** — a window attributed to a request is recorded on it, in `maintenance_windows` of `GET /api/v1/service-requests/{id}` (oldest first: `window_id`, `origin`, `technician_id`, `started_at`, `ended_at`, `closure_reason`), only when the request was `IN_PROGRESS` at the window's start (or, for a window taken over from the app, at that moment); otherwise the inconsistency is logged and nothing is linked. Duplicate events never link twice, and closing the window completes the link.
- **Expiration** — a window still open `MAINTENANCE_WINDOW_EXPIRY_GRACE_MINUTES` after its expiration is closed by the platform as `EXPIRED`, dated at its expiration. A later start on a unit whose active window is already past its expiration closes that window the same way first.

**Audit** (`GET /api/v1/maintenance-windows?equipment_id=10&from=2026-11-01T00:00:00Z&to=2026-11-30T23:59:59Z`, owner JWT): the windows of one of the caller's units, newest first, each with `id`, `equipment_id`, `origin`, `started_at`, `expires_at`, `ended_at`, `closure_reason`, `technician_id`, `service_request_id`, `unlinked` and `active`. `from` and `to` are optional ISO-8601 instants bounding the start inclusively; `from` after `to` answers `400`. A unit the caller does not own answers `404 EQUIPMENT_NOT_FOUND`.

## Sites, address verification and reports

**Address verification.** An owner verifies an address before a site is saved: `POST /api/v1/assets/sites/validate-address` (owner JWT, body `{"address": "...", "place_id": null}`) asks Google Maps synchronously, with at most 2 seconds to connect and 3 to answer. A single match inside Metropolitan Lima or the Constitutional Province of Callao answers `200` with `formatted_address`, `latitude`, `longitude`, `place_id`, `district`, `area` (`LIMA_METROPOLITANA` or `CALLAO`) and a `validation_token`. Nothing is saved. The token is signed with `ADDRESS_VALIDATION_SECRET`, bound to the owner, and valid for 15 minutes.

| Outcome | Status and code |
|---|---|
| Outside the coverage area | `422 ADDRESS_OUT_OF_COVERAGE` |
| No match, or several (`details` lists up to five candidates, one per line; the body carries them as `candidates`) | `422 ADDRESS_NOT_VERIFIED` |
| Maps service timed out, unreachable, or refused | `503 GEOCODING_UNAVAILABLE`, with `Retry-After: 30` |
| More than 20 validations per owner in a minute | `429 TOO_MANY_REQUESTS`, with `Retry-After: 60`; Google is not queried |
| Caller is a technician | `403` |

The coverage check uses bounding boxes around the two jurisdictions. It is an approximation: an address near the border of the two areas can be accepted or refused by a few hundred meters.

**Registering and moving a site.** `POST /api/v1/assets/sites` takes `name`, `contact_name`, `phone`, `validation_token` and an optional `pin` (`{"latitude": ..., "longitude": ...}`). `PUT /api/v1/assets/sites/{siteId}/address` takes `validation_token` and an optional `pin`. Both save the site as `RESOLVED` at the verified point, or at the pin when one is given; Google Maps is not queried. A pin farther than 150 meters from the verified point answers `400 PIN_OUT_OF_RANGE`. A missing, forged, expired or foreign token answers `400 ADDRESS_VALIDATION_REQUIRED`. `PUT /api/v1/assets/sites/{siteId}` now changes only `name`, `contact_name` and `phone`; the address changes only through the address endpoint.

A site's `location` and `navigation_url` are omitted from the JSON when it has no coordinates. Sites created before verification keep their stored `geocoding_status` (`PENDING`, `FAILED` or `NOT_FOUND`) and carry no location until they are moved to a verified address.

**Report requests.** `POST /api/v1/reports` (owner JWT) takes `name`, `type`, `format` (`PDF`, `XLSX` or `CSV`) and `filters` (`site_id`, `equipment_id`, `technician_id`, `from`, `to`). The requester is the authenticated caller; there is no `user_id`. The answer is `202 Accepted` with the report in `PENDING`. Poll `GET /api/v1/reports/{id}`: a `COMPLETED` report carries a `download_url` signed for 15 minutes, a `FAILED` one carries `failure_reason`. A report of another owner answers `404 NOT_FOUND`. The download route `GET /api/v1/reports/{id}/download` has been removed.

For a `TEMPERATURE_EXCURSION` report, each unit gets its Mean Kinetic Temperature (Arrhenius form, activation energy 83.144 kJ/mol), the standard deviation of its temperatures, the degree-minutes outside its threshold band, and the share of time in range. Readings taken during maintenance are excluded, so are dismissed alerts and any alert opened inside a maintenance window, and durations lose the part that overlaps a window; the same window exclusion applies to the uptime report. A unit's time between two readings is credited at most 60 minutes. If no unit in scope recorded any telemetry in the period, the report is `FAILED` with `Insufficient telemetry data for the period`.

Files are stored at `reports/{ownerId}/{reportId}.{ext}` in the private bucket. A storage failure is retried three times with backoff before the report is marked `FAILED`.

Clients must be updated for the renamed fields: the report request uses `name` and `filters`, and the report response uses `name` and `failure_reason`.

## Real-time telemetry stream

The dashboard receives the readings and alert status changes of the owner's units over Server-Sent Events, without polling. A browser `EventSource` cannot send an `Authorization` header, so the stream is opened in two steps:

1. `POST /api/v1/telemetry/stream-ticket` (owner JWT) answers `200` with `{"ticket": "st_<64 hex>", "expires_in": 60}` and `Cache-Control: no-store`. The ticket is bound to the authenticated owner, valid for 60 seconds and redeemable once. An owner may hold up to `TELEMETRY_STREAM_MAX_PENDING_TICKETS_PER_OWNER` unredeemed tickets; one more answers `429 TOO_MANY_REQUESTS` with `Retry-After: 60`. A technician gets `403`.
2. `GET /api/v1/telemetry/stream?ticket=st_...` (no JWT) consumes the ticket, whatever the outcome, and opens a `text/event-stream` response for 30 minutes. A missing, malformed, unknown, used or expired ticket answers `401 UNAUTHORIZED` with the standard JSON body; `EventSource` does not retry a `401`, so request a new ticket and reconnect.

```js
const { ticket } = await (await fetch('/api/v1/telemetry/stream-ticket', {
  method: 'POST', headers: { Authorization: `Bearer ${jwt}` } })).json();
const stream = new EventSource(`/api/v1/telemetry/stream?ticket=${ticket}`);
stream.addEventListener('telemetry-update', (e) => render(JSON.parse(e.data)));
```

| Event | Data |
|---|---|
| `stream-opened` | First event: `timeout_seconds`, `heartbeat_seconds`, and a `retry` of 5 seconds |
| `telemetry-update` with `"type": "READING_RECORDED"` | `reading_uid`, `equipment_id`, `device_id`, `min/max/avg_temperature`, `humidity`, `sample_count`, `recorded_at`, `received_at`, `under_maintenance` |
| `telemetry-update` with `"type": "ALERT_STATUS_CHANGED"` | `alert_id`, `equipment_id`, `alert_type`, `severity`, `previous_status` (`null` when just raised), `status`, `peak_temperature`, `changed_at` |
| comment `heartbeat` | Every `TELEMETRY_STREAM_HEARTBEAT_MS`, so proxies keep idle streams open |

An alert update is sent when an alert opens and on every later transition, manual or automatic. Events are pushed only once the change is committed, so a rejected or duplicate batch is never streamed.

**Tenant isolation.** Streams are kept per owner profile, the one the ticket was issued for. The recipient of an event is resolved from the unit itself (unit, then site, then owner), and the event is written to that owner's streams only; a unit whose owner cannot be resolved is streamed to nobody.

**Limits.** An owner keeps up to `TELEMETRY_STREAM_MAX_PER_OWNER` streams open (browser tabs); opening one more closes the oldest. A stream is dropped as soon as a write to it fails, or when it times out; the browser then needs a new ticket. Writes run on `streamTaskExecutor`, never on a request thread, and opening a stream uses no database connection. Tickets and streams live in the instance's memory: with several replicas, route a client's ticket and stream to the same instance (sticky sessions) and fan events out between instances, for example through PostgreSQL `LISTEN/NOTIFY`. Behind nginx, the `X-Accel-Buffering: no` header the stream sends disables response buffering.

## Integration events and technician metrics

**Transactional outbox.** Service request assignments, rejections, cancellations and completions, and review submissions and edits, are stored as integration events in `outbox_events` in the same transaction as the change. A dispatcher delivers them every `OUTBOX_DISPATCH_DELAY_MS`, each in its own transaction (rows are locked `FOR UPDATE SKIP LOCKED`, so several instances never deliver the same event at once), and sets `published_at`. A consumer that fails rolls back with its delivery, which is retried on a later run; after `OUTBOX_MAX_ATTEMPTS` failures the event stays unpublished, with `attempts` and `last_error`, for inspection. Delivery is at least once, so every consumer is idempotent. Maintenance window events are not part of the outbox: Assets moves the unit's status in the window's transaction, and the service request link and the owner's notification run after it commits.

**Technician metrics read model.** Profiles projects the outbox events into `technician_metrics_projections` (one row per technician: `completed_services_count`, `rating_sum`, `rating_count`, `average_rating`):

- a completion counts once per service request;
- a review counts once per review; an edit replaces that review's score, and an event older than the score already counted is ignored;
- `average_rating = rating_sum / rating_count`, and stays `null` until the first review.

The projection only covers events stored from this version on; completions and reviews recorded before it are not back-filled.

**Public technician profile** (`GET /api/v1/profiles/technicians/{technicianId}/public`, owner JWT): `id`, `full_name`, `speciality`, `certification_number`, `completed_services`, `average_rating` and `rating_available` (`false` while `average_rating` is `null`). Email, phone and address are never included. Unknown technician: `404`.

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