# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repo contains

| Directory | Role | Tech |
|---|---|---|
| `prbs/` | Original 2025 prototype, Babel Standalone + globals. Reference only — not the active dev target. | HTML + `<script type="text/babel">` |
| `prbs-app/` | Active React frontend. Still uses mocked data; the next workstream wires it to the backend. | React 18 + Vite |
| **`prbs-backend/`** | **Active backend.** Modular monolith — one Spring Boot deployable, package-level module boundaries that mirror what microservices would have. | Spring Boot 3.2.5 + Java 17 |

The P2 sandbox (`prbs-p2-sandbox/`) and the prior `prbs-services/` microservices workspace have both been retired — their code lives in `prbs-backend/` now.

## Branches and history

- **`dev`** — default branch on GitHub. All PRs target this.
- **`prototype`** — legacy branch with the original React-only state. Kept for history; nothing new lands here.
- **`p1/entities-for-p2`** — merged into `dev` (PR #1). Monolith with P1 entities + P2 auth/notifications.
- **`p1/booking-availability-admin`** — merged into `dev` (PR #2). P1 REST controllers + admin + CSV import + 4 hardening gaps closed.
- **`feat/microservices`** — superseded. Split into `prbs-services/`; subsequently collapsed back into a modular monolith.
- **`refactor/modular-monolith`** — active. Collapses `prbs-services/` into `prbs-backend/` with package-level module boundaries.

## Package layout (the module boundaries)

```
prbs-backend/src/main/java/com/auca/prbs/
├── PrbsApplication.java
│
├── config/         cross-cutting only
│       ApiException, ErrorResponse, GlobalExceptionHandler,
│       SecurityConfig, JwtAuthenticationFilter
│
├── auth/           OTP, JWT, AuthController, rate limiter, token revocation
│       controller/ AuthController
│       service/    AuthService, OtpService, OtpEmailSender,
│                   JwtTokenProvider, TokenRevocationStore, RateLimiter
│       entity/     OtpToken
│       repository/ OtpTokenRepository
│       dto/        SendOtpRequest, VerifyOtpRequest, RefreshRequest, AuthResponse
│       exception/  OtpInvalidException, TokenInvalidException, TooManyRequestsException
│
├── user/           User + Settings, admin endpoints, CSV import
│       controller/ MeController, UserController, SettingsController
│       service/    UserImportService
│       entity/     User, UserRole, UserStatus, Settings
│       repository/ UserRepository, SettingsRepository
│       dto/        UserResponse, UserSummary, UpdateUserStatusRequest,
│                   ImportUsersResponse, ImportUserError,
│                   SettingsResponse, UpdateSettingsRequest
│       exception/  UserNotFoundException
│
├── booking/        Booking, BookingController, BookingService rules
│       controller/ BookingController
│       service/    BookingService
│       entity/     Booking, BookingStatus
│       repository/ BookingRepository
│       dto/        CreateBookingRequest, UpdateBookingStatusRequest, BookingResponse
│       exception/  BookingNotFoundException, SlotConflictException,
│                   SlotOutsideAvailabilityException, CancelWindowExceededException,
│                   BookingAccessDeniedException, InvalidBookingStatusException
│
├── availability/   Availability + slot generation
│       controller/ AvailabilityController
│       service/    AvailabilityService
│       entity/     Availability
│       repository/ AvailabilityRepository
│       dto/        AvailabilityRequest, AvailabilityResponse, SlotResponse
│       exception/  AvailabilityNotFoundException, AvailabilityAccessDeniedException
│
└── notification/   Booking-related emails + ReminderScheduler
        service/    EmailService, ReminderScheduler
```

One Maven module, one JVM, one database. The discipline lives in the package layout: each module owns its `entity/`, `repository/`, `controller/`, `dto/`, `exception/`, `service/` subpackages. Cross-module reads go through the owning service or repository injected at the boundary — never by reaching into another module's internals.

## Running locally

Requires JDK 17 (Lombok 1.18.30 breaks on Java 22+) and Mailpit (`brew install mailpit`).

```bash
mailpit &
cd prbs-backend
export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
mvn spring-boot:run
```

Then:
- API: <http://localhost:8080>
- Swagger UI: <http://localhost:8080/swagger-ui.html>
- Mailpit: <http://localhost:8025>

Or all-in-one with `docker compose up --build` (mailpit + prbs-backend).

## Source of truth

- **[task_list.md](task_list.md)** — original P2 (Auth & Notifications) checklist. All done.
- **[P1_TaskList.md](P1_TaskList.md)** — P1 (entities + admin + bookings + availability) checklist. All done.
- **[prbs-backend/README.md](prbs-backend/README.md)** — how to run everything.

## Endpoints

```
POST   /api/v1/auth/send-otp                  any
POST   /api/v1/auth/verify-otp                any
POST   /api/v1/auth/refresh                   any
POST   /api/v1/auth/logout                    any

GET    /api/v1/me                             authenticated
GET    /api/v1/users                          ADMIN
PATCH  /api/v1/users/{id}/status              ADMIN
POST   /api/v1/users/import                   ADMIN
GET    /api/v1/settings                       ADMIN
PATCH  /api/v1/settings                       ADMIN

POST   /api/v1/bookings                       STUDENT
GET    /api/v1/bookings/me                    authenticated
PATCH  /api/v1/bookings/{id}/cancel           booking participant
PATCH  /api/v1/bookings/{id}/status           SUPERVISOR

GET    /api/v1/availability                   authenticated
POST   /api/v1/availability                   SUPERVISOR
DELETE /api/v1/availability/{id}              owning SUPERVISOR
GET    /api/v1/availability/{id}/slots        authenticated
```

## Conventions

- **Module ownership.** Each module owns its tables, entities, repositories, controllers, and exceptions. Other modules read through the owning repository or service injected at the boundary — never by importing another module's DTOs into a controller, or reaching past a service to a repository.
- **`config/` is for cross-cutting only.** Security, the JWT filter, the global error handler. Nothing domain-specific.
- **One Flyway migration sequence** at `src/main/resources/db/migration/`. Tables owned by their module but versioned together.
- **JWT validation** happens inside `JwtAuthenticationFilter` once per request. `SecurityConfig` is the single filter chain.
- **HTML-escape all user input** before substituting it into email templates (see `EmailService`).

## Production env vars

```bash
export JWT_SECRET="<32+ chars from secrets manager>"
export MAIL_HOST=smtp.sendgrid.net
export MAIL_PORT=587
export MAIL_USERNAME=apikey
export MAIL_PASSWORD=SG.xxx
export MAIL_FROM=noreply@auca.ac.rw
export CORS_ALLOWED_ORIGINS=https://prbs.auca.ac.rw
export SPRING_PROFILES_ACTIVE=postgres
export DB_URL=jdbc:postgresql://...
export DB_USER=...
export DB_PASSWORD=...
```

## What the React `prbs-app/` still needs

The frontend is unchanged from its mocked-data state. The contracts above are stable, so wiring is a transport swap:

- Replace `Login.jsx`'s simulated OTP with `POST /api/v1/auth/send-otp` + `/verify-otp`
- Store the access + refresh tokens in `sessionStorage`
- Replace the email-substring role match in `App.jsx` with `user.role` from the `AuthResponse`
- Add a fetch interceptor that calls `/api/v1/auth/refresh` on 401
- Configure CORS in the backend (`CORS_ALLOWED_ORIGINS`) if the React dev server runs on a different host

`prbs-app/`'s internal data model (`MOCK_BOOKINGS_INIT`, `MOCK_AVAILABILITY`, etc.) deliberately matches the backend's DTO shapes — wiring is a transport swap, not a model rewrite.

## Why modular monolith and not microservices

The microservices split (now retired) was a useful exercise — it forced clean module boundaries — but in operation it bought us complexity (6 JVMs, Eureka, Feign, distributed token validation) for no scale we actually need at prototype stage. The modular monolith keeps the same discipline as a future-state insurance policy: when any one of these packages outgrows the deployable, its entity/repository/service/controller/dto/exception split is already what you'd carve into a service.
