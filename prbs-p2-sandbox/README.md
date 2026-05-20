# prbs-p2-sandbox

P2 (Authentication & Notifications) sandbox for PRBS — Spring Boot 3.2.x, Java 17, jjwt 0.12.x.

Standalone project so P2 work is testable in isolation while P1 builds entities and repositories. When P1 publishes the shared repo, the `com.auca.prbs.*` classes here will be copied over (package names already match).

## Prerequisites

| Tool | Version |
|---|---|
| Java | **17 LTS exactly** — Lombok's annotation processor is broken on Java 22+. POM targets 17 either way |
| Maven | 3.9.x |
| MailHog | latest — for `EmailServiceTest` |

If your default `java` is newer than 17, point `JAVA_HOME` at a JDK 17 before running Maven:

```bash
export JAVA_HOME="$(/usr/libexec/java_home -v 17)"   # macOS
mvn test
```

Install MailHog (macOS):
```bash
brew install mailhog
mailhog   # SMTP on :1025, web UI at http://localhost:8025
```

## Commands

```bash
mvn test                                              # run all P2 tests
mvn test -Dtest=JwtTokenProviderTest                  # one class
mvn test -Dtest=OtpServiceTest#correct_code_verifies  # one method
mvn spring-boot:run                                   # boot the app (port 8080)
```

## Status

Track progress against [`../task_list.md`](../task_list.md). Phases 1–4 are independently buildable here. Phases 5–7 require P1's JPA entities and repositories.
