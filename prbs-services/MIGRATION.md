# Strangler-fig migration: monolith → microservices

`prbs-backend/` is the monolith. Each step here carves one service out of it. After all steps land, the monolith is deleted.

## Step 0 — done in this PR

- Workspace, parent pom, 7 modules
- `eureka-server`, `api-gateway`, `prbs-shared` complete
- `auth-service` complete (OTP + JWT + refresh + logout + revocation + rate limit + email)
- `user-service` stubbed with one in-cluster endpoint (`/api/v1/users/by-email`) so auth-service works
- `booking-service`, `notification-service` stubbed with 501 responses

## Step 1 — full `user-service`

Source files to copy from `prbs-backend/src/main/java/com/auca/prbs/`:
- `entity/User.java`, `entity/UserRole.java`, `entity/UserStatus.java`, `entity/Settings.java`
- `repository/UserRepository.java`, `repository/SettingsRepository.java`
- `controller/UserController.java`, `controller/SettingsController.java`, `controller/MeController.java`
- `service/UserImportService.java`
- `dto/UserResponse.java`, `dto/UpdateUserStatusRequest.java`, `dto/SettingsResponse.java`, `dto/UpdateSettingsRequest.java`, `dto/ImportUsersResponse.java`, `dto/ImportUserError.java`, `dto/AuthResponse.java` (UserSummary nested record)
- `exception/UserNotFoundException.java`, `exception/ApiException.java`, `exception/GlobalExceptionHandler.java`

Also: copy `db/migration/V1__users.sql`, `V3__settings.sql`, `V6__seed_demo_users.sql` (renumber to V1/V2/V3 inside user-service).

Add a `SecurityConfig` similar to monolith's that uses `prbs-shared`'s `JwtAuthenticationFilter`. Wire `/api/v1/users/**` and `/api/v1/settings/**` to require ROLE_ADMIN; `/api/v1/me` to require any authenticated user; `/api/v1/users/by-email` to be public (in-cluster only — handle via internal network).

Delete the `StubUserController`.

## Step 2 — `booking-service`

Copy from monolith:
- `entity/Booking.java`, `entity/BookingStatus.java`, `entity/Availability.java`
- `repository/BookingRepository.java`, `repository/AvailabilityRepository.java`
- `controller/BookingController.java`, `controller/AvailabilityController.java`
- `service/BookingService.java`, `service/AvailabilityService.java`
- `dto/CreateBookingRequest.java`, `dto/BookingResponse.java`, `dto/UpdateBookingStatusRequest.java`, `dto/AvailabilityRequest.java`, `dto/AvailabilityResponse.java`, `dto/SlotResponse.java`
- All booking-specific exceptions

**Cross-service calls:**
- `BookingService.notifyParticipants` currently calls `UserRepository.findById` to resolve student/supervisor names → replace with a Feign client to `user-service` (`/api/v1/users/{id}`).
- `BookingService.notifyParticipants` calls `EmailService.sendBookingConfirmation` and `sendSupervisorAlert` → replace with a Feign client to `notification-service` (POST `/internal/emails/booking-confirmation`, `/internal/emails/supervisor-alert`).
- Same for `ReminderScheduler`.

`SettingsRepository.findById(1)` is used twice (cancel window, reminder window) → either:
- (a) duplicate the Settings table into booking-service's DB and accept eventual consistency, or
- (b) Feign call to user-service `/api/v1/settings` (admin-only, but caller is in-cluster) — easier, do this.

Delete `StubBookingController`.

## Step 3 — `notification-service`

Copy from monolith:
- `service/EmailService.java`, `service/EmailDeliveryException.java`, `service/ReminderScheduler.java`

But `ReminderScheduler` currently queries `BookingRepository` directly. In microservices it must call `booking-service` instead. Two options:
- (a) Keep the scheduler in `booking-service` (it already owns the data); notification-service just exposes email-sending endpoints.
- (b) Move the scheduler to notification-service and have it call `booking-service` for the query.

**Recommendation:** (a). Keep the scheduler where the data is. notification-service becomes a pure email-sending service.

Endpoints exposed:
- POST `/internal/emails/booking-confirmation` — student email
- POST `/internal/emails/supervisor-alert` — supervisor email
- POST `/internal/emails/reminder` — reminder email
- POST `/internal/emails/otp` — (optional — auth-service has its own OtpEmailSender today; consolidate later)

## Step 4 — retire `prbs-backend/`

Once steps 1–3 are green and the gateway can serve every endpoint via the right service:
- Frontend points at the gateway exclusively
- All `prbs-backend/` tests are deleted or ported into their new service
- `prbs-backend/` directory is removed in a single commit

## Conventions across the migration

- **Database per service** (in production). Today the prototype can share one Postgres; each service runs its own Flyway migrations in its own schema or against tables it owns exclusively.
- **No service touches another service's tables.** Cross-service reads go through HTTP.
- **JWT validation in every service.** No service trusts another service's claim about a user's role — it verifies the bearer token.
- **`prbs-shared` for cross-cutting concerns only.** Anything else stays inside the owning service.
- **One DTO per direction.** When auth-service needs a user, it has its own `UserView` record; user-service has its own `UserResponse`. They happen to have the same fields today, but they're allowed to diverge.

## Testing strategy across services

- **Unit/slice tests** live in each service (same as monolith).
- **Contract tests** between services (e.g. Spring Cloud Contract) — out of scope for the prototype; add when a second team starts editing a service.
- **End-to-end** via docker-compose: bring up all services, run a Postman collection through the gateway. The existing `prbs-backend/postman/PRBS-Auth.postman_collection.json` works unchanged against the gateway on :8080.
