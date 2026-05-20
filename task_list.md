# P2 Task List — Auth & Notifications
**PRBS · AUCA · Branch: `p2/auth-notifications`**

---

## Phase 2 — Authentication & Security (Week 2)

### Setup
- [ ] Create sandbox Maven project (`prbs-p2-sandbox`) with all required dependencies
- [ ] Add `application.yml` with `jwt.secret`, MailHog SMTP config, and `mail.from`
- [ ] Verify MailHog runs locally at `http://localhost:8025`
- [ ] Push branch to remote: `git push -u origin p2/auth-notifications`

---

### 1. JWT — `JwtTokenProvider`
- [ ] Implement `generateAccessToken(userId, role)` — HS256, 15-minute expiry
- [ ] Implement `generateRefreshToken(userId)` — HS256, 7-day expiry
- [ ] Implement `parseToken(token)` — returns `Claims`
- [ ] Implement `isValid(token)` — catches all `JwtException` variants
- [ ] Implement `getUserId(token)` and `getRole(token)` helper methods
- [ ] Write `JwtTokenProviderTest`:
  - [ ] Access token round-trip (userId + role survive encode/decode)
  - [ ] Refresh token contains no role claim
  - [ ] Tampered token is rejected
  - [ ] Expired token is rejected
- [ ] Run: `mvn test -Dtest=JwtTokenProviderTest` — all pass

---

### 2. JWT Filter & Security — `JwtAuthenticationFilter` + `SecurityConfig`
- [ ] Implement `JwtAuthenticationFilter` — reads `Authorization: Bearer` header, validates token, sets `SecurityContext`
- [ ] Implement `SecurityConfig` filter chain:
  - [ ] `/api/v1/auth/**` — public
  - [ ] `/api/v1/users/**` — `ADMIN` only
  - [ ] `/api/v1/settings/**` — `ADMIN` only
  - [ ] `POST /api/v1/availability/**` — `SUPERVISOR` only
  - [ ] `DELETE /api/v1/availability/**` — `SUPERVISOR` only
  - [ ] All other routes — authenticated
- [ ] Write stub `StubController` in `src/test/java` for filter testing
- [ ] Write `JwtAuthenticationFilterTest`:
  - [ ] Valid token returns 200
  - [ ] Missing token returns 401
  - [ ] Wrong role returns 403
  - [ ] `/auth/**` routes return 200 with no token
- [ ] Run: `mvn test -Dtest=JwtAuthenticationFilterTest` — all pass

---

### 3. OTP Logic — `OtpService`
- [ ] Create temporary stub `OtpToken.java` POJO (delete when P1 delivers entity)
- [ ] Implement `generate(userId, expiryMinutes)` — `SecureRandom` 6-digit code, BCrypt hash, returns `OtpResult` record
- [ ] Implement `verify(submittedCode, token)` — checks used flag, expiry, BCrypt match
- [ ] Write `OtpServiceTest`:
  - [ ] Generated code is exactly 6 digits
  - [ ] Correct code verifies successfully
  - [ ] Wrong code fails verification
  - [ ] Used token is rejected
  - [ ] Expired token is rejected
  - [ ] Two consecutive codes are different
- [ ] Run: `mvn test -Dtest=OtpServiceTest` — all pass

---

### 4. Email Service — `EmailService` + HTML Templates
- [ ] Implement `send()` private helper using `JavaMailSender` + `MimeMessageHelper`
- [ ] Implement `sendOtp(email, name, otpCode)` + `buildOtpHtml()` template
  - [ ] AUCA dark blue header (`#0F2755`)
  - [ ] OTP code displayed large and centered in blue box
  - [ ] Expiry note in footer
- [ ] Implement `sendBookingConfirmation(email, name, project, date, time, meetUrl)` + template
  - [ ] Session details table (project, date, time)
  - [ ] "Join Google Meet →" button
- [ ] Implement `sendReminder(email, name, project, date, time, meetUrl)` + template
  - [ ] Session summary card
  - [ ] "Join Google Meet →" button
- [ ] Write `EmailServiceTest`:
  - [ ] `sendOtp` delivers to MailHog without exception
  - [ ] `sendBookingConfirmation` delivers to MailHog without exception
  - [ ] `sendReminder` delivers to MailHog without exception
- [ ] Visually inspect all three emails at `http://localhost:8025` — check branding, layout, links
- [ ] Run: `mvn test -Dtest=EmailServiceTest` — all pass

---

### 5. Auth Endpoints — `AuthController` + `AuthService`
> Requires P1's `UserRepository`, `OtpTokenRepository`, `SettingsRepository`.
> Complete this after P1 delivers entities (end of Week 1).

- [ ] `POST /auth/send-otp`:
  - [ ] Look up user by email — return 404 if not found
  - [ ] Reject if user status is `INACTIVE` — return 403
  - [ ] Call `OtpService.generate()` with `settings.otpExpiry`
  - [ ] Persist `OtpToken` via `OtpTokenRepository`
  - [ ] Call `EmailService.sendOtp()`
  - [ ] Return 200
- [ ] `POST /auth/verify-otp`:
  - [ ] Find latest unused, non-expired token for user
  - [ ] Call `OtpService.verify()` — return 401 on failure
  - [ ] Mark token as used
  - [ ] Issue access token + refresh token via `JwtTokenProvider`
  - [ ] Return `{ accessToken, refreshToken }`
- [ ] `POST /auth/refresh`:
  - [ ] Validate refresh token
  - [ ] Issue new access token
  - [ ] Return `{ accessToken }`
- [ ] `POST /auth/logout`:
  - [ ] Invalidate refresh token (mark as used or delete)
  - [ ] Return 204
- [ ] Run all auth endpoint tests: `mvn test -Dtest=AuthControllerTest`

---

## Phase 5 — Notifications & Reminder Scheduler (Week 3–4)

### 6. Booking Confirmation & Supervisor Alert Emails
> Requires P1's `Booking` entity and `BookingService`.

- [ ] Hook `EmailService.sendBookingConfirmation()` into `BookingService.createBooking()` — fires after successful `POST /bookings`
- [ ] Implement supervisor alert email (`sendSupervisorAlert()`) + template:
  - [ ] Triggered on student booking
  - [ ] Triggered on student cancellation
  - [ ] Include student name, project, date, time
- [ ] Test: book a session via Postman, verify both emails arrive in MailHog

---

### 7. Reminder Scheduler — `ReminderScheduler`
> Requires P1's `BookingRepository` and `SettingsRepository`.

- [ ] Implement `@Scheduled(cron = "0 * * * * *")` method (every minute)
- [ ] Query: `status = CONFIRMED`, `reminder_sent = false`, `slot_time` within `settings.reminderTime` minutes from now
- [ ] For each result: call `EmailService.sendReminder()`, set `reminder_sent = true`, save
- [ ] Write `ReminderSchedulerTest` (mock repository):
  - [ ] Bookings inside the window are processed and `reminder_sent` flipped to `true`
  - [ ] Bookings outside the window are skipped
  - [ ] Already-reminded bookings (`reminder_sent = true`) are skipped
- [ ] Run: `mvn test -Dtest=ReminderSchedulerTest`
- [ ] Manual test: set a booking `slot_time` to 2 minutes from now, wait for scheduler to fire, verify email in MailHog

---

## Handoff Checklist (before merging into `prototype`)

- [ ] Delete stub `OtpToken.java` POJO — replaced by P1's JPA entity
- [ ] Replace hardcoded `otpExpiry = 10` with `settingsRepository.findById(1).getOtpExpiry()`
- [ ] All 4 independent test classes pass: `mvn test -Dtest="JwtTokenProviderTest,JwtAuthenticationFilterTest,OtpServiceTest,EmailServiceTest"`
- [ ] Full test suite passes: `mvn test`
- [ ] Rebase onto latest `prototype`: `git fetch origin && git rebase origin/prototype`
- [ ] Open pull request from `p2/auth-notifications` → `prototype`

---

*P2 Task List · PRBS · AUCA*
