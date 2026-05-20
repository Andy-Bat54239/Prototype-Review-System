# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repo contains now

Three coexisting projects:

| Directory | Role | Tech |
|---|---|---|
| `prbs/` | Original 2025 prototype, Babel Standalone + globals. Reference only — not the active dev target. | HTML + `<script type="text/babel">` |
| `prbs-app/` | Active React frontend. Still uses mocked data; will be wired to `prbs-backend` by P3. | React 18 + Vite |
| **`prbs-backend/`** | **Canonical backend.** Integrates P1 (entities/repos/migrations/business rules) and P2 (auth + notifications). Runnable, 75+ tests passing. | Spring Boot 3.2.5 + Java 17 + JPA + Flyway |

The earlier `prbs-p2-sandbox/` standalone has been removed; all its classes live in `prbs-backend/` now.

## Branches and history

- **`dev`** — default branch on GitHub. All PRs target this.
- **`prototype`** — legacy branch with the original React-only state. Kept for history; nothing new lands here.
- **`p1/entities-for-p2`** — merged into `dev` (PR #1, May 20 2026). Brought in the prbs-backend scaffold + P1 entities/repos/migrations + the full P2 implementation + production hardening.
- **`p1/booking-availability-admin`** — current branch. Adds the P1 REST controllers (BookingController, AvailabilityController, UserController, SettingsController, MeController) and the business rules around them. PR open against `dev`.

## Source of truth

- **[task_list.md](task_list.md)** — the original P2 task list. Every checkbox is now done.
- **[P1_TaskList.md](P1_TaskList.md)** — P1 deliverables, mirrors the P2 list's structure. Tracks the work added on `p1/booking-availability-admin`.
- **[P2_Independent_Work.md](P2_Independent_Work.md)** — the reference implementation P2 worked from. Mostly historical at this point.
- **[prbs-backend/P1_Handoff_Report.pdf](prbs-backend/P1_Handoff_Report.pdf)** — the structured handoff for the P1 owner from the first wave (entities/repos/migrations only).

## Running `prbs-backend`

```bash
cd prbs-backend
export JAVA_HOME="$(/usr/libexec/java_home -v 17)"  # JDK 17 only — Lombok 1.18.30 breaks on Java 22+
mvn test                                              # full suite
mvn spring-boot:run                                   # H2 in-memory, app on :8080
mvn spring-boot:run -Dspring-boot.run.profiles=postgres   # real Postgres
```

Boot does Flyway migrations + seeds 8 demo users (`alice@`, `supervisor@`, `admin@`, …) so the React app and Postman collection work immediately.

To deliver real email instead of Mailpit, copy `.env.example` → `.env` and `source` it before `mvn spring-boot:run`. See `prbs-backend/README.md`.

## Backend at a glance

```
prbs-backend/src/main/java/com/auca/prbs/
├── PrbsBackendApplication.java   @EnableScheduling
├── entity/        User, OtpToken, Settings, Booking, Availability + 3 enums
├── repository/    5 JpaRepositories with the queries the services need
├── security/      JwtTokenProvider, JwtAuthenticationFilter, SecurityConfig,
│                   RateLimitFilter, TokenRevocationStore
├── service/       AuthService, OtpService, EmailService (4 templates),
│                   BookingService (incl. slot validation + cancel window +
│                   conflict detection), AvailabilityService (slot generation),
│                   ReminderScheduler
├── controller/    AuthController, BookingController, AvailabilityController,
│                   UserController, SettingsController, MeController
├── dto/           Validated request/response shapes
└── exception/     Typed ApiException subclasses + GlobalExceptionHandler
                   ({code, message} envelope)
```

## Endpoints

```
POST   /api/v1/auth/send-otp                {email}                      → 200
POST   /api/v1/auth/verify-otp              {email, code}                → {accessToken, refreshToken, user}
POST   /api/v1/auth/refresh                 {refreshToken}               → {accessToken, refreshToken, user}
POST   /api/v1/auth/logout                  {refreshToken}               → 204
GET    /api/v1/me                                                        → UserSummary
POST   /api/v1/bookings                     {availabilityId, slotTime, project, groupNumber}  (STUDENT)
GET    /api/v1/bookings/me                                               → role-aware list
PATCH  /api/v1/bookings/{id}/cancel                                      → BookingResponse
PATCH  /api/v1/bookings/{id}/status         {status}                     (SUPERVISOR)
GET    /api/v1/availability?supervisorId=&from=                          → list
POST   /api/v1/availability                 {date, startTime, endTime, durationMinutes, meetUrl}  (SUPERVISOR)
DELETE /api/v1/availability/{id}                                         (owning SUPERVISOR)
GET    /api/v1/availability/{id}/slots                                   → SlotResponse[]
GET    /api/v1/users                                                     (ADMIN)
PATCH  /api/v1/users/{id}/status            {status}                     (ADMIN)
GET    /api/v1/settings                                                  (ADMIN)
PATCH  /api/v1/settings                     partial update               (ADMIN)
```

## Other things to know

- **Postman collection** at `prbs-backend/postman/PRBS-Auth.postman_collection.json` — auto-captures the OTP from Mailpit and tokens between requests. Covers the auth flow + rate-limit testing.
- **JDK 17 only** — Lombok 1.18.30 (pinned by Spring Boot 3.2.5) crashes on Java 22+. The README documents the `JAVA_HOME` workaround.
- **Production env vars** — `JWT_SECRET`, `MAIL_*`, `CORS_ALLOWED_ORIGINS`, plus `SPRING_PROFILES_ACTIVE=postgres` + `DB_URL` / `DB_USER` / `DB_PASSWORD` for a real deploy. Defaults are dev-only and fire startup WARNs to make the gap obvious.

## What `prbs-app/` still needs

The React frontend is unchanged from its prototype state — simulated OTP, email-string role detection. The contracts are now stable, so wiring it up is P3 (or whoever picks frontend integration):

- Replace `Login.jsx`'s simulated OTP with `POST /auth/send-otp` + `/verify-otp`
- Store the access + refresh tokens in `sessionStorage`
- Replace the email-substring role match in `App.jsx` with `user.role` from `AuthResponse`
- Add a fetch interceptor that calls `/auth/refresh` on 401
- Configure CORS origin if served from a different host

`prbs-app/`'s internal data model (MOCK_BOOKINGS_INIT, MOCK_AVAILABILITY, etc.) deliberately matches the backend's DTO shapes — wiring is a transport swap, not a model rewrite.
