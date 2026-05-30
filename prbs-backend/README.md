# prbs-backend — modular monolith

Single-deployable Spring Boot 3 app with package-level module boundaries that mirror what microservices would have. One JVM, one database, one Maven module — but the package layout enforces ownership: each module owns its entities, repositories, controllers, and exceptions, and cross-module calls go through service classes only.

## Module layout

```
com.auca.prbs/
├── config/         cross-cutting: SecurityConfig, JwtAuthenticationFilter,
│                   GlobalExceptionHandler, ApiException
├── auth/           OTP, JWT, AuthController, rate limiter, token revocation
├── user/           User + Settings entities, admin endpoints, CSV import
├── booking/        Booking entity, BookingController, BookingService rules
├── availability/   Availability entity, slot generation
└── notification/   EmailService, ReminderScheduler
```

The same Maven module compiles them all; the discipline lives in the package layout. Each module's `entity/`, `repository/`, `controller/`, `dto/`, `exception/`, and `service/` subpackages own their data — other modules read through the owning service or repository injected at the boundary.

## Run locally

Requires Java 17 (Lombok 1.18.30 breaks on Java 22+) and Mailpit (`brew install mailpit`).

```bash
mailpit &
cd prbs-backend
export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
mvn spring-boot:run
```

Then:

- API: <http://localhost:8080>
- Swagger UI: <http://localhost:8080/swagger-ui.html>
- Mailpit web UI: <http://localhost:8025>

Or, all-in-one with Docker:

```bash
docker compose up --build
```

## End-to-end auth flow

```bash
curl -X POST http://localhost:8080/api/v1/auth/send-otp \
  -H 'Content-Type: application/json' \
  -d '{"email":"alice@university.ac.rw"}'

# OTP arrives in Mailpit. Then:
curl -X POST http://localhost:8080/api/v1/auth/verify-otp \
  -H 'Content-Type: application/json' \
  -d '{"email":"alice@university.ac.rw","code":"<6 digits>"}'
# → { accessToken, refreshToken, user: { id, email, name, role } }
```

## Environment variables

```bash
JWT_SECRET=<32+ chars>            # required for non-dev
MAIL_HOST=smtp.sendgrid.net       # default: localhost (Mailpit)
MAIL_PORT=587
MAIL_USERNAME=apikey
MAIL_PASSWORD=SG.xxx
MAIL_FROM=noreply@auca.ac.rw
CORS_ALLOWED_ORIGINS=https://prbs.auca.ac.rw
SPRING_PROFILES_ACTIVE=postgres   # switch from H2 to Postgres
DB_URL=jdbc:postgresql://localhost:5432/prbs
DB_USER=prbs
DB_PASSWORD=prbs
```

## Migrations

A single Flyway sequence at `src/main/resources/db/migration/`:

```
V1__users.sql
V2__settings.sql
V3__seed_demo_users.sql
V4__otp_tokens.sql
V5__bookings.sql
V6__availability.sql
```

## Why modular monolith and not microservices

The microservices split was a useful exercise — it forced clean boundaries — but in operation it bought us complexity (6 JVMs, Eureka, Feign, distributed token validation) for no scale we actually need. The modular monolith keeps the same boundary discipline as a future-state insurance policy: when any one of these packages outgrows the deployable, the entity/repository/service/controller/dto/exception split inside it is already what you'd carve into a service.
