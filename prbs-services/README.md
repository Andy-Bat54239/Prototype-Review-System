# prbs-services — microservices architecture

Multi-module Maven workspace for the PRBS backend split into independently-deployable services. Co-exists with `prbs-backend/` (the monolith) during the strangler-fig migration; `prbs-backend/` will be deleted once all services land.

## Services

| Module | Port | Role |
|---|---|---|
| `eureka-server` | 8761 | Service discovery (Spring Cloud Netflix Eureka) |
| `api-gateway` | 8080 | Single public entry point. Routes by path. CORS lives here. |
| `auth-service` | 8081 | **Full** — OTP, JWT issuance + refresh + logout + revocation, rate limiting |
| `user-service` | 8082 | **Stub** — `/api/v1/users/by-email` for auth-service; full migration next |
| `booking-service` | 8083 | **Stub** — `/bookings/**`, `/availability/**` return 501 |
| `notification-service` | 8085 | **Stub** — booking emails + reminder scheduler land here |
| `prbs-shared` | (library) | `JwtTokenProvider`, `JwtAuthenticationFilter`, shared DTOs |

## Run locally

Requires Java 17, Mailpit (`brew install mailpit`).

```bash
mailpit &
cd prbs-services
export JAVA_HOME="$(/usr/libexec/java_home -v 17)"

# In four terminals (order matters — Eureka first):
( cd eureka-server   && mvn spring-boot:run )
( cd api-gateway     && mvn spring-boot:run )
( cd user-service    && mvn spring-boot:run )
( cd auth-service    && mvn spring-boot:run )
```

Or `docker-compose up` (see `docker-compose.yml`).

Check Eureka dashboard at <http://localhost:8761> — you should see 3 services registered.

## End-to-end auth flow through the gateway

```bash
# Request OTP (goes gateway → auth-service → calls user-service for the lookup)
curl -X POST http://localhost:8080/api/v1/auth/send-otp \
  -H 'Content-Type: application/json' \
  -d '{"email":"alice@university.ac.rw"}'

# OTP arrives in Mailpit at http://localhost:8025
# Then:
curl -X POST http://localhost:8080/api/v1/auth/verify-otp \
  -H 'Content-Type: application/json' \
  -d '{"email":"alice@university.ac.rw","code":"<6 digits>"}'
# → { accessToken, refreshToken, user: { id, email, name, role } }
```

## Communication patterns

- **Synchronous service-to-service:** Spring Cloud OpenFeign + Eureka-resolved logical names. Example: `auth-service` calls `user-service` via `@FeignClient(name = "user-service")`.
- **Token validation everywhere:** every service includes `prbs-shared`'s `JwtAuthenticationFilter` and shares the same `jwt.secret`. The gateway does NOT validate tokens — it just routes; services validate independently so a bypass of the gateway still gets rejected.
- **Async events (future):** when notification-service is migrated, booking-service will publish events ("booking-created", "booking-cancelled") that notification-service subscribes to. For the prototype, we'll start with sync HTTP and swap to RabbitMQ if it becomes a bottleneck.

## See also

- [`MIGRATION.md`](MIGRATION.md) — what's done, what's next, exact steps to carve out the remaining services from `prbs-backend/`.
- [`../prbs-backend/`](../prbs-backend/) — the monolith. Still runs end-to-end; deleted when migration completes.
