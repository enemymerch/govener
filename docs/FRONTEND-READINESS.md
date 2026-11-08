# Frontend readiness — live API integration

The manager/resident web application now uses the implemented Spring Boot API. Business records persist in MongoDB; refreshing the browser does not reset them. Demo role cards, role switching, local profile records, and fixture data loaders have been removed from the runtime.

| Area | Connected behavior |
|---|---|
| Session | Real cookie login/logout, CSRF rotation, bootstrap, 401 clearing, generic password recovery |
| Context | Paginated membership communities, active own units, scoped requests and aborted stale reads |
| Dashboard | Server monthly summary, due date, counts, authorized own-account projection |
| Accounts | Server totals, charge/payment pages, paginated ledger entries |
| Finance | Versioned charge/payment/expense commands, explicit allocations, credit, matching, corrections |
| Transfers | Private receipts, persisted notifications, review/rejection/withdrawal |
| Requests | Persisted create/photos, comments, assignment, status and full paginated histories |
| Announcements | Persisted drafts/publication, audience IDs from server units/blocks |
| Directory | Current explicit residents, authorized manager relationships, server account summaries |
| Profile | Separate versioned profile and preference saves; server contact data |
| Notifications | Per-user reads and read-all cutoff via API |
| Internationalization | Turkish/English interface, stable codes, Istanbul dates and TRY kuruş formatting |

See [the frontend guide](../frontend/README.md) for startup and browser-test commands. The test harness boots a real backend with an isolated disposable MongoDB replica set; it never clears the development database. Browser scenarios exercise actual cookie/CSRF requests, persistent records, financial retry protection, authorization, context switching and error states.

## Boundaries

The current workspace exhausts cursor pages before local filtering. This avoids incomplete totals/lists at the initial scale, but larger communities should move to incremental server-filtered screen queries. Financial summaries and cash balances are server projections.

No authenticated business view falls back to mock data. A failed startup/read is shown with retry. Financial commands are not automatically retried; an uncertain retry retains its operation key while the same form/payload remains in use. Post-write refresh failure blocks further writes until refreshed. Browser storage contains language/context preferences and opaque intent-key digests, not session secrets or financial payloads.

Admin provisioning, native Android delivery, payment gateways, company accounting, and business push/email notifications remain outside this release. SMTP must be configured for password reset delivery.

Verification: 26 browser tests passed against the real API, along with the TypeScript/Vite build, OpenAPI contract drift check, and formatting check. Desktop Turkish and mobile English layouts were inspected.
