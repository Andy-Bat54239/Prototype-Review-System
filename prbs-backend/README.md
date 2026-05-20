# prbs-backend

Integrated PRBS backend — Spring Boot 3.2.5, Java 17. Combines P1's entities/repos and P2's auth + notifications into a single runnable app.

## Requirements

- **JDK 17 exactly** (Lombok's annotation processor breaks on Java 22+). On macOS:
  ```bash
  export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
  ```
- Maven 3.9+
- (Optional) Mailpit for visual email inspection: `brew install mailpit && mailpit`

## Run

```bash
mvn spring-boot:run                       # H2 in-memory, app at :8080
mvn spring-boot:run -Dspring-boot.run.profiles=postgres   # against Postgres
mvn test                                  # full suite
mvn test -Dtest=AuthControllerTest        # one class
```

H2 is the default for local dev — no DB setup required. Flyway runs the migrations on boot; seed users (alice/supervisor/admin) are created so login works immediately.

## What lives here

| Package | Owner | Role |
|---|---|---|
| `entity/`     | P1 | JPA entities: User, OtpToken, Settings, Booking, Availability |
| `repository/` | P1 | Spring Data JPA repositories |
| `security/`   | P2 | JWT provider/filter, SecurityConfig, rate-limit filter, token revocation |
| `service/`    | P1+P2 | OtpService, EmailService, AuthService, BookingService, ReminderScheduler |
| `controller/` | P2 | `/auth/*` endpoints (Phase 5) |
| `dto/`        | P2 | Request/response DTOs |
| `exception/`  | P2 | Typed errors + global handler |

## Endpoints

```
POST /api/v1/auth/send-otp     {email}                              → 200
POST /api/v1/auth/verify-otp   {email, code}                        → {accessToken, refreshToken, user}
POST /api/v1/auth/refresh      {refreshToken}                       → {accessToken}
POST /api/v1/auth/logout       {refreshToken}                       → 204
```

Authorization: `Bearer <accessToken>` on all non-`/auth/**` routes.

## Production deploy checklist

Set these env vars (defaults are dev-only and will fire a startup WARN):

```bash
export JWT_SECRET="<32+ chars from secrets manager>"
export MAIL_FROM="noreply@auca.ac.rw"
export CORS_ALLOWED_ORIGINS="https://prbs.auca.ac.rw"
export SPRING_PROFILES_ACTIVE=postgres
export DB_URL="jdbc:postgresql://..."
export DB_USER="..."
export DB_PASSWORD="..."
```
