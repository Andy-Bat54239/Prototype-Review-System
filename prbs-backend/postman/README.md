# PRBS Auth — Postman collection

`PRBS-Auth.postman_collection.json` covers the AuthController end-to-end plus the rate-limit filter and the error envelope.

## Quick start

1. **Start dependencies:**
   ```bash
   mailpit &                                                # SMTP on :1025, web at :8025
   cd prbs-backend
   export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
   mvn spring-boot:run
   ```
2. **Import** `PRBS-Auth.postman_collection.json` into Postman.
3. **Run the *Happy Path* folder top to bottom.** Tokens and the 6-digit OTP code flow between requests automatically — the *Send OTP* test script reads the OTP out of Mailpit's REST API and sets it as a collection variable, so you never copy-paste a code by hand.
4. **For rate limiting:** open Collection Runner → select only the *Rate limit* folder → set Iterations ≥ 11 → Run.

The *Negative cases* folder validates the `{code, message}` envelope for `USER_NOT_FOUND`, `USER_INACTIVE`, `OTP_INVALID`, and Bean Validation failures.

## What this collection does *not* cover

`AuthController` exposes 4 endpoints. Three of the six P2 features show up there; three don't:

| P2 feature | Tested by this collection? | Why / why not |
|---|---|---|
| 1. OTP generation | ✅ via `Send OTP` |
| 2. Email delivery | ✅ Mailpit captures it |
| 3. JWT issuance + refresh | ✅ Verify, Refresh, Logout |
| 4. Role-based protection | ❌ Needs an authenticated route to hit | AuthController is `permitAll` — nothing in it requires a token |
| 5. Reminder cron | ❌ Lives in `ReminderScheduler` | Needs a Booking in the DB to fire against |
| 6. Booking confirmation email | ❌ Lives in `BookingService.createBooking` | No HTTP endpoint creates bookings yet |

## To cover features 4, 5, 6 — add two small controllers

The *Extensions* folder in the collection has requests pre-written for `GET /api/v1/me` and `POST /api/v1/_dev/bookings`. They'll work as soon as you add the two files below.

### 1. `MeController.java` — proves feature 4

Drop this in `src/main/java/com/auca/prbs/controller/`:

```java
package com.auca.prbs.controller;

import com.auca.prbs.dto.AuthResponse;
import com.auca.prbs.entity.User;
import com.auca.prbs.exception.UserNotFoundException;
import com.auca.prbs.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class MeController {

    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<AuthResponse.UserSummary> me(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        return ResponseEntity.ok(new AuthResponse.UserSummary(
                user.getId(), user.getEmail(), user.getName(), user.getRole().name()));
    }
}
```

The JWT filter sets the principal to the user's ID (Long); we look the user up and return a `UserSummary`. The endpoint isn't in any `permitAll` matcher, so it falls under `anyRequest().authenticated()` — exactly what proves feature 4 works.

### 2. `BookingDevController.java` — proves features 5 + 6

Drop this in `src/main/java/com/auca/prbs/controller/`:

```java
package com.auca.prbs.controller;

import com.auca.prbs.entity.Booking;
import com.auca.prbs.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dev-only shim. Calls BookingService.createBooking so the Phase 6 email hooks
 * and the Phase 7 reminder scheduler can be exercised before P3 ships
 * BookingController. Active only when the 'dev' profile is on; delete when the
 * real controller lands.
 */
@RestController
@RequestMapping("/api/v1/_dev/bookings")
@RequiredArgsConstructor
@Profile("dev")
public class BookingDevController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<Booking> create(@RequestBody Booking booking) {
        return ResponseEntity.ok(bookingService.createBooking(booking));
    }
}
```

Then start the backend with the `dev` profile:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

After running the `POST /_dev/bookings` request, you should see in Mailpit:
- **Booking Confirmed — …** (to `alice@university.ac.rw`) — feature 6
- **Session scheduled — Alice Uwase** (to `supervisor@university.ac.rw`) — Phase 6 supervisor alert

For the reminder (feature 5), set `slotAt` to **about 2 minutes from now** in the body, then wait. The `@Scheduled(cron = "0 * * * * *")` job fires at the top of each minute; the next reminder email arrives within a minute or two, and `reminder_sent` in the `bookings` table flips to `true` — no duplicate reminder fires after that.

## Other things you might want to enable

| Change | Why | How |
|---|---|---|
| **H2 console** at `/h2-console` | Inspect rows directly during testing | Add `spring.h2.console.enabled: true` to `application.yml`, restart |
| **Active `dev` profile** | Activates `BookingDevController` | `mvn spring-boot:run -Dspring-boot.run.profiles=dev` |
| **Real SendGrid SMTP** | Production email | Set `spring.mail.host`, `spring.mail.port`, credentials via env vars in your deploy; remove Mailpit |
| **Postgres backend** | Production DB | `SPRING_PROFILES_ACTIVE=postgres DB_URL=… DB_USER=… DB_PASSWORD=… mvn spring-boot:run` |
| **`JWT_SECRET` env var** | Stops the startup WARN about the dev-only fallback | `export JWT_SECRET="<32+ chars>"` before starting |
