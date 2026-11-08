# Govener backend

Java 21, Spring Boot 3.5, Maven, and MongoDB. This monolith implements the manager/resident [API contract](../docs/openapi.json). The web UI now connects to these services; see [the frontend guide](../frontend/README.md) for the development proxy and browser tests.

## Run locally

Install Java 21 and start Docker Desktop with Linux containers. From the repository root:

```powershell
docker compose up -d --wait
.\mvnw.cmd -pl backend spring-boot:run "-Dspring-boot.run.profiles=dev"
```

The API listens on `http://localhost:8080/api/v1`. Health: `http://localhost:8080/actuator/health`. MongoDB must be a replica set because financial writes, audit records, and the event outbox commit together.

The `dev` profile explicitly enables synthetic data and non-secure localhost cookies. Demo users are `manager@govener.test`, `resident@govener.test`, `neighbor@govener.test`, `staff@govener.test`, and `outsider@govener.test`; their development-only password is `DemoPass123!`. Two isolated communities are seeded. The seed is idempotent and never overwrites existing data. Production profiles exclude the fixture component.

For production, supply the settings in [.env.example](.env.example) through your runtime environment. Spring Boot does not automatically load that example file. Use HTTPS, a strong stable `GOVENER_CURSOR_SECRET`, secure cookies, and your deployment's MongoDB credentials. Sessions currently live in the application process, so restarting the monolith signs users out.

Password reset requests return the same accepted response for known and unknown users. Tokens expire after 15 minutes, can be used once, and revoke existing sessions. Only token hashes are stored in reset records. The email outbox encrypts its short-lived payload. Set `GOVENER_MAIL_ENABLED=true` and configure `SPRING_MAIL_*` for delivery; disabled email does not send reset links. Changing the root secret invalidates cursors and queued encrypted reset emails.

## Authentication

1. `GET /auth/csrf`, retaining the `govener_session` cookie.
2. Send `POST /auth/login` with `email`, `password`, and `X-CSRF-Token`.
3. Use the rotated `csrfToken` in the session response for subsequent writes.
4. Include `Idempotency-Key` for financial commands. Keep the same key and payload when retrying an uncertain result.

Roles and permissions are resolved from the authenticated user's community membership on every operation. Login does not accept a client-supplied role or community grant. Logout invalidates the session. Unknown JSON fields are rejected.

## Design

Each business module contains pure Java `domain` models and `application` use cases/ports. MongoDB, HTTP, cryptography, file validation, and mail adapters sit outside these layers. Spring wiring lives under `configuration`. ArchUnit checks enforce the dependency direction.

| Module | Responsibility |
|---|---|
| Identity | Credentials, profile, preferences, password reset, session revocation |
| Community | Membership permissions, units, dated relationships, physical residence directory |
| Finance | Charges, manual allocations, unit accounts, transfers, expense accrual and settlement |
| Service | Requests, assignment, status history, comments |
| Communication | Draft/published announcements and selected audiences |
| Attachment | Private upload metadata, GridFS content, ownership and parent binding |
| Notification | Transactional outbox and per-user in-app notifications |
| Shared | Value objects, transactions, aggregate repositories, signed cursor pagination, audit |

The repository and transaction ports implement hexagonal boundaries. Explicit allocation and financial visibility use policies; idempotent financial execution is a reusable application component. Aggregate state transitions enforce lifecycle rules. The transactional outbox separates committed business changes from notification delivery.

MongoDB stores one document per aggregate, with community scope and optimistic version checks. Comments, request history, ledger entries, audit records, and outbox events have separate collections. Financial mutations update their related aggregates in a single snapshot/majority transaction. Failed batches roll back completely. A competing write returns 409; the server does not silently retry mutations.

Money uses integer kuruş. Charges belong to units. Manual payment allocations must reference charges in the same unit and cannot exceed the payment or outstanding charge. Excess stays as unallocated credit; it is not automatically applied to future charges. Payment notifications do not move money. Matching an existing payment does not record it again. Corrections create traceable cancellations/reversals.

Residents see records only within their owner/tenant relationship periods; retrospective charges use the billing period. A partially overlapping charge is withheld pending a separate policy. Resident summaries and statements are projections of visible records. Current ownership/tenancy access and physical residence are separate: the directory lists current residents' names and unit labels, without financial records, contact details, or relationship history.

Attachments are private, limited to 5 MB, validated by purpose/content, and bound to one parent resource. Unbound files expire after 24 hours; cleanup uses a version check before deleting content. The outbox uses deterministic per-recipient notification IDs and checks current membership/preferences during delivery.

## Verification

```powershell
.\mvnw.cmd verify
```

Unit and architecture tests run in Surefire. Gherkin acceptance tests run in Failsafe through Cucumber, using real HTTP, cookie/CSRF authentication, and a disposable Testcontainers MongoDB replica set. Docker must be running; integration tests are not silently skipped when Docker is unavailable. Test data is reset only inside that disposable test database.

To regenerate Java transport records after changing the contract, run `node backend/scripts/generate-dtos.cjs` from the repository root, followed by `.\mvnw.cmd -pl backend spotless:apply`. Formatting checks run during Maven validation.

The verified implementation currently passes **59 unit/architecture/endpoint-contract tests** and **26 Gherkin acceptance scenarios**, with no skipped tests. JaCoCo reports 2,548 of 2,776 lines covered (91.8%) for this run; the report includes generated transport records.

Reports:

- `backend/target/surefire-reports`: unit/architecture results.
- `backend/target/failsafe-reports`: acceptance results.
- `backend/target/cucumber/report.html`: Gherkin scenarios and steps.
- `backend/target/site/jacoco/index.html`: measured coverage.

## Deliberate boundaries

Admin provisioning screens, online payment gateways, company accounting, native Android delivery, automatic interest/proration, and legal assignment of debt liability remain outside this release. Production onboarding must supply memberships, permissions, dated unit access, units, and financial accounts; public registration is not exposed. Push and email preference flags are stored, but this release delivers business notifications in-app only; SMTP is used for password reset.

Collection reads currently load the authorized community's matching aggregates before applying signed cursor pagination. This is correct for the initial scope but is a scaling limit: larger deployments should add repository-level query ports and indexed keyset pagination. The API does not promise snapshot pagination while records are concurrently inserted. Orphan cleanup covers registered upload metadata; storage reconciliation after a process crash during GridFS upload is an operational follow-up.
