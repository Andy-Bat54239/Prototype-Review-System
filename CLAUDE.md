# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repo contains

| Directory | Role | Tech |
|---|---|---|
| `prbs/` | Original 2025 prototype, Babel Standalone + globals. Reference only — not the active dev target. | HTML + `<script type="text/babel">` |
| `prbs-app/` | Active React frontend. Still uses mocked data; the next workstream wires it to the gateway. | React 18 + Vite |
| **`prbs-services/`** | **Active backend.** 7-module Spring Cloud microservices: gateway + Eureka + 4 services + shared library. Replaced the monolith. | Spring Boot 3.2.5 + Spring Cloud 2023.0.1 + Java 17 |

The monolith (`prbs-backend/`) and the P2 sandbox (`prbs-p2-sandbox/`) have both been retired — their code lives in `prbs-services/` now.

## Branches and history

- **`dev`** — default branch on GitHub. All PRs target this.
- **`prototype`** — legacy branch with the original React-only state. Kept for history; nothing new lands here.
- **`p1/entities-for-p2`** — merged into `dev` (PR #1). The monolith with P1 entities + P2 auth/notifications.
- **`p1/booking-availability-admin`** — open against `dev` (PR #2). P1 REST controllers + admin + CSV import + 4 hardening gaps closed.
- **`feat/microservices`** — open against `dev` (PR #3). **Splits everything into prbs-services/, deletes prbs-backend.** This is the active branch.

## Microservices layout

```
prbs-services/
├── pom.xml                  parent (Spring Boot 3.2.5 + Spring Cloud 2023.0.1)
├── README.md                run instructions + architecture diagram
├── MIGRATION.md             how the monolith was carved up
├── docker-compose.yml       one-command local dev (Mailpit + 6 JVMs)
│
├── eureka-server/    :8761  service discovery
├── api-gateway/      :8080  Spring Cloud Gateway, route-by-path, CORS
├── prbs-shared/             cross-cutting: JwtTokenProvider + JwtAuthenticationFilter + ErrorResponse
│
├── auth-service/     :8081  OTP, JWT issue/refresh/logout, in-memory revocation,
│                            per-email rate limit. Owns otp_tokens table. Calls
│                            user-service for user lookups; notification-service
│                            (with inline SMTP fallback) for OTP email delivery.
│
├── user-service/     :8082  User + Settings entities, admin endpoints
│                            (UserController, SettingsController), CSV bulk-import,
│                            MeController. Owns users + settings tables. Exposes
│                            in-cluster /by-email, /by-id/{id}, /settings/internal
│                            for other services to read.
│
├── booking-service/  :8083  Booking + Availability entities, BookingController,
│                            AvailabilityController, slot generation, conflict +
│                            cancel-window business rules, ReminderScheduler
│                            (@Scheduled cron). Owns bookings + availability tables.
│                            Feign-calls user-service for participants and
│                            notification-service to fire emails.
│
└── notification-service/ :8085  EmailService with 4 AUCA-branded templates
                                  (OTP, booking confirmation, reminder, supervisor
                                  alert). No DB. Reachable only on /internal/emails/*;
                                  the gateway doesn't route /internal/*.
```

## Running locally

Requires JDK 17 (Lombok 1.18.30 breaks on Java 22+) and Mailpit (`brew install mailpit`).

```bash
mailpit &
cd prbs-services
export JAVA_HOME="$(/usr/libexec/java_home -v 17)"

# In separate terminals, in this order:
( cd eureka-server         && mvn spring-boot:run )
( cd api-gateway           && mvn spring-boot:run )
( cd user-service          && mvn spring-boot:run )
( cd auth-service          && mvn spring-boot:run )
( cd booking-service       && mvn spring-boot:run )
( cd notification-service  && mvn spring-boot:run )
```

Then the frontend talks to **`http://localhost:8080`** for everything. Eureka dashboard at `:8761`, Mailpit at `:8025`.

The full Postman collection (in the now-deleted `prbs-backend/postman/`, history preserved in git) works unchanged against the gateway.

## Source of truth

- **[task_list.md](task_list.md)** — original P2 (Auth & Notifications) checklist. All done.
- **[P1_TaskList.md](P1_TaskList.md)** — P1 (entities + admin + bookings + availability) checklist. All done.
- **[prbs-services/MIGRATION.md](prbs-services/MIGRATION.md)** — how the monolith was carved into microservices.
- **[prbs-services/README.md](prbs-services/README.md)** — how to run everything.

## Endpoints (all reachable via gateway on :8080)

```
POST   /api/v1/auth/send-otp                 auth-service       any
POST   /api/v1/auth/verify-otp               auth-service       any
POST   /api/v1/auth/refresh                  auth-service       any
POST   /api/v1/auth/logout                   auth-service       any

GET    /api/v1/me                            user-service       authenticated
GET    /api/v1/users                         user-service       ADMIN
PATCH  /api/v1/users/{id}/status             user-service       ADMIN
POST   /api/v1/users/import                  user-service       ADMIN
GET    /api/v1/settings                      user-service       ADMIN
PATCH  /api/v1/settings                      user-service       ADMIN

POST   /api/v1/bookings                      booking-service    STUDENT
GET    /api/v1/bookings/me                   booking-service    authenticated
PATCH  /api/v1/bookings/{id}/cancel          booking-service    booking participant
PATCH  /api/v1/bookings/{id}/status          booking-service    SUPERVISOR

GET    /api/v1/availability                  booking-service    authenticated
POST   /api/v1/availability                  booking-service    SUPERVISOR
DELETE /api/v1/availability/{id}             booking-service    owning SUPERVISOR
GET    /api/v1/availability/{id}/slots       booking-service    authenticated
```

In-cluster only (not routed by gateway):
```
GET  /api/v1/users/by-email                  user-service       called by auth-service
GET  /api/v1/users/by-id/{id}                user-service       called by booking-service
GET  /api/v1/settings/internal               user-service       called by booking-service
POST /internal/emails/{otp,booking-confirmation,reminder,supervisor-alert}   notification-service
```

## Conventions across the services

- **Each service validates JWTs independently.** Same `JWT_SECRET` env var across all. The gateway routes but does not validate.
- **No service touches another service's tables.** Cross-service reads go through Feign clients resolved via Eureka.
- **`prbs-shared` is for cross-cutting only** (JWT, error envelope). Domain DTOs live in their owning service.
- **Database per service** in production; one Postgres with separate schemas for prototype.
- **One Flyway migration directory per service**, owning a non-overlapping set of tables.

## Production env vars

```bash
export JWT_SECRET="<32+ chars from secrets manager>"     # required for all services
export MAIL_HOST=smtp.sendgrid.net                       # notification-service only
export MAIL_PORT=587
export MAIL_USERNAME=apikey
export MAIL_PASSWORD=SG.xxx
export MAIL_FROM=noreply@auca.ac.rw
export CORS_ALLOWED_ORIGINS=https://prbs.auca.ac.rw      # gateway only
export EUREKA_URL=http://eureka:8761/eureka/             # all services
export SPRING_PROFILES_ACTIVE=postgres                   # services with a DB
export DB_URL=jdbc:postgresql://...
export DB_USER=...
export DB_PASSWORD=...
```

## What the React `prbs-app/` still needs

The frontend is unchanged from its mocked-data state. The contracts above are stable, so wiring is a transport swap:

- Replace `Login.jsx`'s simulated OTP with `POST /api/v1/auth/send-otp` + `/verify-otp` (against the gateway)
- Store the access + refresh tokens in `sessionStorage`
- Replace the email-substring role match in `App.jsx` with `user.role` from the `AuthResponse`
- Add a fetch interceptor that calls `/api/v1/auth/refresh` on 401
- Configure CORS in the gateway (`CORS_ALLOWED_ORIGINS`) if the React dev server runs on a different host

`prbs-app/`'s internal data model (`MOCK_BOOKINGS_INIT`, `MOCK_AVAILABILITY`, etc.) deliberately matches the backend's DTO shapes — wiring is a transport swap, not a model rewrite.
