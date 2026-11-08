# Govener

Apartment and residential community management for managers and residents. The visible application brand is **yönetici**.

The phase-one [web frontend](frontend/README.md) includes financial workflows, unit/resident directories, requests, announcements, profile/notifications, context selection and authentication previews. It connects to the [Spring Boot backend](backend/README.md) for real sessions and persistent manager/resident workflows. The development profile seeds synthetic users and communities.

## Start

```powershell
cd frontend
npm ci
npm run dev
```

## Design and backend preparation

- [DDD decisions](DDD-DESIGN.md)
- [TODO and deferred topics](docs/TODO.md)
- [Domain and DDD guide](docs/DOMAIN-DDD-GUIDE.md)
- [Domain diagram gallery](docs/diagrams/index.html)
- [OpenAPI contract — 59 operations](docs/openapi.json)
- [Endpoint inventory](docs/API-ENDPOINTS.md)
- [Backend handoff](docs/BACKEND-HANDOFF.md)
- [Frontend readiness](docs/FRONTEND-READINESS.md)

## Backend

```powershell
docker compose up -d --wait
.\mvnw.cmd -pl backend spring-boot:run "-Dspring-boot.run.profiles=dev"
.\mvnw.cmd verify
```

See the [backend guide](backend/README.md) for Java 21, configuration, architecture and test reports.

## Validate

```powershell
cd frontend
npm run build
npm run api:check
npm test
```

The web application uses the API by default. Start both the backend and Vite as described in the [frontend guide](frontend/README.md). See the handoff for server authority and remaining product boundaries.

Documentation, code, comments and commit messages are English. The UI supports Turkish and English. Product discussions may be Turkish. Admin, online payments, company accounting and native Android delivery are deferred.
