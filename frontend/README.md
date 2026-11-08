# Govener web frontend

React, TypeScript, Vite, and i18next. The visible brand is **yönetici**. The application uses the Spring Boot API for authentication and business data; there is no demo role selector or fallback to in-memory business records.

## Run locally

Start Docker Desktop with Linux containers. From the repository root:

```powershell
docker compose up -d --wait
.\mvnw.cmd -pl backend spring-boot:run "-Dspring-boot.run.profiles=dev"
```

In a second terminal:

```powershell
cd frontend
npm ci
npm run dev
```

Open the URL printed by Vite (normally `http://localhost:5173`). The Vite proxy sends `/api` to `http://127.0.0.1:8080`; override `GOVENER_API_TARGET` to use another backend. The proxy is development-only. Deployments must route `/api/v1` to the backend or configure `VITE_API_BASE_URL`, HTTPS, cookie behavior, and the backend's allowed origins for the actual deployment.

Development-only accounts are seeded by the backend's `dev` profile:

| Account | Login | Password |
|---|---|---|
| Manager | `manager@govener.test` | `DemoPass123!` |
| Resident | `resident@govener.test` | `DemoPass123!` |
| Neighbor | `neighbor@govener.test` | `DemoPass123!` |
| Request manager with limited permissions | `staff@govener.test` | `DemoPass123!` |
| Resident of another community | `outsider@govener.test` | `DemoPass123!` |

These accounts are synthetic backend fixtures, not frontend authentication shortcuts. Real records persist in MongoDB across browser reloads. Provisioning production users/sites remains an admin responsibility.

## Connected workflows

- Cookie-session bootstrap, CSRF acquisition/rotation, real login/logout, password reset requests and reset consumption.
- Membership-provided communities, authorized own units, role/capability-based navigation and actions.
- Server dashboard, unit-account totals, charge/payment lists, and ledger entries.
- Charge batches, explicit payment allocation, unallocated credit, existing-payment matching, cancellation and reversal.
- Expense accrual, optional immediate settlement, later settlement of the unpaid amount, and settlement reversal.
- Transfer notifications, private receipt upload/download, review, rejection, and withdrawal.
- Service requests, photo uploads, comments, staff assignment, status changes and complete paginated event/comment histories.
- Draft/published announcements with community, block, and unit audiences.
- Current resident directory and manager relationship history.
- Versioned profile/preferences and persisted notification read state.

Server-authored records are displayed without translating user content. Interface text, operation errors, dates, and monetary formatting support Turkish and English. Browser language selection is retained locally; saving profile preferences also stores the selected language on the server.

## State and failure handling

`src/session/Session.tsx` owns the authenticated session. Session cookies are HttpOnly; tokens and passwords are not stored in browser storage. A 401 clears authenticated views. Startup/network failures show a retry state and never substitute fixture data.

`src/workspace/Workspace.tsx` loads a server snapshot scoped to the user/community/own-unit selection. Context changes abort outstanding reads and reject stale results. Capabilities control which endpoints are loaded. Mutations refresh affected server data rather than changing balances locally. A failed refresh blocks further writes until the user reloads the snapshot; it does not resubmit the mutation.

`src/api/server.ts` follows every cursor page and rejects malformed/repeating cursors. The current workspace eagerly collects authorized collections before displaying them, which keeps existing local filters complete but is a client-side scaling limit. Large deployments should move screens to incremental, server-filtered page queries. Account totals and cash balances always come from server projections rather than loaded list arithmetic.

Financial submissions retain an idempotency key for the same payload after an uncertain network/server outcome. Session storage contains only a digest and random intent key, never the financial payload. Known rejected submissions and confirmed successes release the key. Uploaded file IDs are reused within the same form when explicitly retrying its parent command. No mutation retries automatically. An explicit browser reload clears form/file state; inspect server records before recreating an uncertain transaction after leaving a form.

## Verify

```powershell
# From the repository root; build the executable backend for browser tests:
.\mvnw.cmd -pl backend -DskipTests package
cd frontend
npx playwright install chromium
npm run build
npm run api:check
npm test
```

Browser tests start a real backend and disposable MongoDB replica set on Docker, then remove them afterward. They do not reset the development database. Java 21, Docker, and the backend jar are required. `PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH` can select an installed Chromium executable. See `test-results` for failure traces. The backend remains independently verified with `mvnw verify`.

Edit the OpenAPI builder, regenerate types, and validate with `npm run api:generate` / `npm run api:check`. Business authorization and financial invariants remain backend responsibilities.

Admin UI, company accounting, online payment processing, native Android delivery, and business push/email notification delivery remain outside this release. Password reset delivery requires the backend SMTP configuration.

The latest full browser run passed 26 tests against the real API and disposable MongoDB, including mobile layout/sign-out, 100+ record pagination, stale-response isolation, and uncertain financial retries. TypeScript/Vite build, OpenAPI drift validation, and formatting checks also passed.
