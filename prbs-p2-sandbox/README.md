# prbs-p2-sandbox

P2 (Authentication & Notifications) sandbox for PRBS — Spring Boot 3.2.x, Java 17, jjwt 0.12.x.

Standalone project so P2 work is testable in isolation while P1 builds entities and repositories. When P1 publishes the shared repo, the `com.auca.prbs.*` classes here will be copied over (package names already match).

## Prerequisites

| Tool | Version |
|---|---|
| Java | **17 LTS exactly** — Lombok's annotation processor is broken on Java 22+. POM targets 17 either way |
| Maven | 3.9.x |
| Mailpit (or MailHog) | latest — for visually inspecting rendered templates. `EmailServiceTest` itself uses embedded GreenMail, so no external SMTP is needed for `mvn test`. |

If your default `java` is newer than 17, point `JAVA_HOME` at a JDK 17 before running Maven:

```bash
export JAVA_HOME="$(/usr/libexec/java_home -v 17)"   # macOS
mvn test
```

Install Mailpit (macOS) — MailHog was removed from Homebrew core; Mailpit is the actively-maintained drop-in (same default ports 1025 / 8025):
```bash
brew install mailpit
mailpit
```
Then open http://localhost:8025

## Commands

Run all P2 tests:
```bash
mvn test
```

Run one class:
```bash
mvn test -Dtest=JwtTokenProviderTest
```

Run one method (`#` here is Surefire's method-selector syntax — fine in a shell, but do **not** add trailing `# comments` because IntelliJ's Maven runner doesn't strip them):
```bash
mvn test -Dtest=OtpServiceTest#correctCode_verifiesSuccessfully
```

Boot the app (port 8080):
```bash
mvn spring-boot:run
```

## Status

Track progress against [`../task_list.md`](../task_list.md). Phases 1–4 are independently buildable here. Phases 5–7 require P1's JPA entities and repositories.
