# Backend Handoff — Manager and Resident Web Application

Status: contract version 0.1.0. The manager/resident Spring Boot backend is implemented under `backend/`; see [the backend guide](../backend/README.md). The web workspace now uses the live API for sessions, reads, uploads, and writes.

The React frontend uses the typed transport and server-backed repositories. Local business-state mutations and demo role entry have been replaced. See [the frontend guide](../frontend/README.md) for startup, failure behavior, pagination, and browser verification.

## Deliverables and source of truth

| File | Purpose |
|---|---|
| `docs/openapi.json` | Machine-readable OpenAPI 3.0.3 contract: 59 operations, 69 schemas. |
| `docs/API-ENDPOINTS.md` | Generated endpoint inventory, access scope, and input/output schemas. |
| `docs/FRONTEND-READINESS.md` | Implemented screens, validation evidence, and remaining integration tasks. |
| `frontend/scripts/build-contract.cjs` | Editable contract builder; regenerates OpenAPI. |
| `frontend/scripts/generate-api-types.cjs` | Generates DTOs and operation metadata from OpenAPI. |
| `frontend/src/api/generated.ts` | Generated DTOs and typed request/response definitions. |
| `frontend/src/api/client.ts` | Typed HTTP transport, credentials, CSRF, cancellation, timeout, and errors. |
| `frontend/src/api/repositories.ts` | Prepared auth, profile, context, request, announcement, notification, and financial operation adapters. Unwrapped operations remain available through `raw`. |
| `frontend/src/api/adapters.ts` | Server DTO to view-model conversion; preserves versions, opaque IDs, and user-authored content. |
| `frontend/src/workspace/Workspace.tsx` | Live server snapshot and community/unit context boundary. |

Edit the builder, then run:

```powershell
cd frontend
npm run api:generate
npm run api:validate
npm run build
npm test
```

The contract is a proposal for implementation, not an assertion that every open DDD rule has been approved. Existing DDD decisions remain the product decision record.

## Transport conventions

- Base URL: `/api/v1`; configure the prepared client using `VITE_API_BASE_URL`.
- JSON names use camelCase. Entity IDs are opaque, case-sensitive strings, never display labels or translated text. Path IDs use letters, numbers, `_`, and `-`, up to 100 characters.
- Wire enums are stable uppercase codes. Request statuses map to localized view labels through adapters.
- Currency is `TRY`. Money uses integer minor units (kuruş), never binary floating-point amounts. Transaction inputs allow up to 999,999,999 minor units. Aggregated integers must remain within JavaScript's safe integer range.
- Calendar dates are `YYYY-MM-DD`; timestamps are UTC ISO instants. Community calendar calculations and display use `Europe/Istanbul`.
- Collection endpoints return `{ items, nextCursor, hasMore }`. `limit` is 1–100, default 20. A cursor is opaque and must not cross user/community/filter contexts.
- Mutable entities return `version`. Write commands that change an existing resource require `expectedVersion`; a stale version returns 409.
- No mutation automatically retries. Preserve the same idempotency key and identical payload when explicitly retrying an uncertain financial command.
- Authenticated writes use `X-CSRF-Token`; financial writes also require `Idempotency-Key`.
- Errors use `application/problem+json` with stable `code`, optional `traceId`, and field errors. Do not expose stack traces or sensitive account details.
- Optional related objects are omitted when absent. Primitive IDs/timestamps explicitly marked nullable may use null.

## Authentication and authorization

The proposed web session uses an HttpOnly, Secure, SameSite cookie named `govener_session`. Session secrets are not stored in browser storage. The prepared client uses `credentials: include` and keeps the CSRF token in memory.

Recommended bootstrap:

1. `getCsrf` obtains CSRF material.
2. `getSession` restores an existing authenticated session or returns 401; `login` creates a session after explicit sign-in.
3. `listCommunities` returns authorized memberships.
4. `getContext` returns the selected community, applicable roles, permissions, own units, and profile.
5. Load each authorized feature using the selected community ID and an AbortSignal.

A user may hold both roles, or different roles in different communities. `Session.roles` is a union of available roles; `CommunityContext.roles` and `permissions` are authoritative for the selected community. A client-side role/view selector never grants authority.

The contract's `x-access` metadata records intended roles, scope, and capabilities for every operation. Spring Security and application services must enforce these again. Never authorize an object only because its ID is valid; all referenced units, charges, payments, files, staff assignments, and audience IDs must belong to the path community.

Residents can see the current community directory, but directory access does not grant access to another unit's finances or historical relationships. A missing/out-of-scope resource should use an opaque 404 where revealing its existence would leak information.

Password-reset requests return a generic 202. Reset delivery, token generation, expiry, single-use consumption, session invalidation policy, password hashing, rate limiting, and email ownership verification belong to the backend. Updating the profile contact email must not silently change verified sign-in identity.

## Frontend screen to API mapping

| Frontend flow | Operations |
|---|---|
| Sign-in and restore session | `getCsrf`, `login`, `getSession`, `logout` |
| Password recovery | `requestPasswordReset`, `resetPassword` |
| Community/unit selection | `listCommunities`, `getContext` |
| Overview | `getDashboard` |
| Manager unit directory/details | `listUnits`, `getUnit`, `listRelationships` |
| Resident directory/household | `listDirectory`, `getDirectoryUnit` |
| Account balances and ledger | `listUnitAccounts`, `getUnitAccount`, `listAccountEntries`, `listCharges`, `listPayments` |
| Charge creation/cancellation | `createChargeBatch`, `getChargeBatch`, `cancelCharge` |
| Manual collection and correction | `recordPayment`, `reversePayment` |
| Payment notifications | `listPaymentNotifications`, `createPaymentNotification`, `reviewPaymentNotification`, `withdrawPaymentNotification`, `linkPaymentNotification` |
| Expense capture and payment | `listExpenses`, `getExpense`, `createExpense`, `payExpense`, `listExpensePayments`, `reverseExpensePayment` |
| Cash/bank balances | `listFinancialAccounts` |
| Requests and conversations | `listRequests`, `getRequest`, `createRequest`, `listRequestComments`, `addRequestComment` |
| Request assignment/status/audit | `listAssignableStaff`, `assignRequest`, `changeRequestStatus`, `listRequestEvents` |
| Announcements | `listAnnouncements`, `getAnnouncement`, `createAnnouncement`, `updateAnnouncement` |
| Profile and preferences | `getProfile`, `updateProfile`, `getPreferences`, `updatePreferences` |
| Notification center | `listNotifications`, `markNotificationRead`, `markAllNotificationsRead` |
| Upload/download | `uploadAttachment`, `getAttachment`, `downloadAttachment` |

## Financial behavior expected by the screens

A charge belongs to a unit. Ownership, tenancy, residence, and management permission are separate concepts.

### Charge batch

The frontend currently submits a fixed, explicit amount per selected unit and a chosen period/due date. It does not decide area/land-share allocation, proration, recurring schedules, late charges, or legal liability. The API accepts explicit per-unit amounts so those policies are not hidden inside the UI.

A batch commits atomically or fails. Scope idempotency by actor, community, operation and key. Reusing an idempotency key with the same payload returns the original batch; reusing it with a different payload returns 409. Do not ban all charges for a unit/month: multiple legitimate charges may share a period.

### Recorded payment

A recorded payment is distinct from a payment notification. Recording it may atomically link a notification and apply explicit allocations to same-unit charges.

- Each allocation is positive and cannot exceed the charge's outstanding amount.
- Total allocations cannot exceed the payment amount.
- Allocation to a cancelled charge or another unit is rejected.
- Unallocated funds remain credit, shown separately from outstanding charges.
- No oldest-debt-first or other implicit allocation order is applied.
- Linking an existing payment annotates the existing payment; it never creates a second money movement.
- A notification cannot link to multiple active payments. The current UI requires matching unit and amount for a linked transfer.

### Corrections

Records are retained with actor, timestamp, and reason. Reversing a payment releases allocations and reverses its cash-account effect atomically. The current mock blocks cancellation of an allocated charge until related payments are corrected. This is a conservative prototype choice; confirm the desired correction policy before financial production rollout.

After correction, return authoritative versions and notification-link status. A reversed linked payment must not leave a notification incorrectly displayed as an active recorded transfer. The mock derives the displayed link from active payments; the server should return a consistent status/paymentId.

### Expenses

An incurred expense and its payment are separate. Optional immediate payment is atomic with expense creation. The first UI pays an unpaid expense in full and supports reasoned payment reversal. The contract allows an explicit payment amount, so partial expense payments can be introduced without changing the core model.

Cash/bank balances derive from opening amounts and recorded/reversed movements. They must not be independently editable totals. Company accounting and online payments remain excluded.

MongoDB must run in a transaction-capable configuration for multi-document financial writes. Handle duplicate keys, concurrent allocations, stale versions, and uncertain client retries explicitly.

## Requests and announcements

Residents access their own request records and conversations; managers require appropriate community capabilities. Creator identity is assigned from the authenticated session, not accepted from the request body. Assignee IDs must be authorized staff in the community. Server-created audit events are append-only.

Announcements support draft/published status and ALL/BLOCKS/UNITS audiences. ALL uses an empty IDs array; selected audiences require nonempty same-community IDs. Residents receive only published notices matching any authorized unit membership. Apply the rule to both list and detail routes. A manager save with PUBLISHED status atomically publishes the updated content/audience. Real notification delivery should follow a committed publication event.

## Attachments

Uploads return metadata and an attachment ID. Forms reference IDs; JSON payloads never contain JavaScript File objects or blob URLs. The prepared repositories upload request photos in parallel before creating a request and upload receipts/documents before the parent command.

Validate size, content signature, media type, purpose, ownership, community, and parent-resource access server-side. The maximum is 5 MB per file. Request photos allow PNG/JPEG/WebP, at most 3; payment receipts and expense documents allow PNG/JPEG/PDF. Clean up unattached uploads using a retention policy.

Download endpoints authorize access again and return safe Content-Disposition and nosniff headers. The frontend's remote attachment hook downloads through the authenticated canonical endpoint, creates a temporary object URL, and revokes it after use. Do not rely on a client-supplied external URL.

## Frontend integration status

The integration is implemented. Session bootstrap uses the cookie session; login obtains and rotates CSRF material. Community context and actions use server memberships and capabilities. Feature collections follow cursor pages, and account/dashboard/cash totals use server projections. Writes use expected versions, financial idempotency keys, private attachment IDs, and post-write refreshes. A 401 clears authenticated views; 409, validation, and network failures remain visible without automatic mutation retries.

The workspace currently collects complete authorized collections before local filtering; large datasets should move to screen-specific server-filtered pagination. Production admin provisioning, online payments, push/business email delivery, and SMTP reset delivery configuration remain separate boundaries. The [frontend guide](../frontend/README.md) documents the exact behavior.

## Backend implementation order

1. Session/CSRF, profile/preferences, membership context, common errors, pagination.
2. Community/unit/directory projections and staff read models.
3. Attachments and parent-resource authorization.
4. Charges, unit accounts, payments, notifications, expenses, cash balances and transaction/audit invariants.
5. Requests, comments, assignments, status history, and announcements/audiences.
6. Notification center and publication/payment/request event delivery.
7. HTTP-backed frontend adapters, contract fixtures, and end-to-end validation.

## Product decisions still open

- Confirmed: resident financial visibility is limited to periods covered by their dated owner/tenant relationship. Retrospective charges use their billing period. Continued access after a relationship ends and treatment of partially overlapping billing periods remain separate decisions.
- Legal payment responsibility and any proration, area/share distribution, late charges, or interest policy.
- Final correction/refund rules beyond the conservative mock behavior.
- Full request lifecycle, reopening/closure policy, detailed staff permissions, and announcement version/audit history.
- Notification delivery/retention, email verification, and password policy details.

Implement explicit confirmed rules and these contract primitives first; do not silently turn the mock policy into a final business decision. Admin provisioning, subscriptions, management-company accounting, online payments, and the native Android implementation remain outside this web deliverable. Android can reuse the API contract after its session strategy is agreed.
