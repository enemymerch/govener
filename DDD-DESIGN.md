# Govener — DDD Design Decisions

Last updated: November 8, 2026

This document records decisions discussed for the apartment and residential community management application. **Confirmed decisions** are separate from **draft proposals**. Draft proposals are not approved implementation requirements. When development starts with Qwen, unresolved questions must not be converted into business rules through assumptions.

Documentation language: English. Discussion may continue in Turkish.

Confirmed frontend language decision: the UI supports Turkish and English through i18n. The visible application brand is **yönetici**; the repository remains **Govener**.

## 1. Product and Technical Scope — Confirmed

- The initial service area is Istanbul, Türkiye.
- Target customers include independent apartment/community management teams and professional property management companies.
- The product owner may use the application while providing management services through their own company. Customers may also purchase only the software.
- The backend will be a Java Spring Boot monolith.
- The database will be MongoDB.
- The user application will have web and Android versions.
- The first product to be developed is the shared application for managers and residents.
- The admin frontend will be separate. Admin requirements and implementation are deferred.
- Managers will also be able to perform management operations from their phones. The detailed Android feature scope is not finalized.
- Manual bank transfer tracking is sufficient for the first release. Online payments are deferred.
- Accounting for the management company's own business is excluded from the first release. Community financial tracking is a separate concern.

## 2. Ubiquitous Language and Roles — Confirmed

| Term | Definition |
|---|---|
| Community Admin | A role with full authority over the apartment/community within its authorized scope. Uses a separate frontend. |
| Community Manager | A role with limited management permissions using the shared user application. |
| Resident | A role with limited access using the same application as managers. |
| Unit | An independently tracked property such as an apartment, shop, or office. The agreed unit-based account model uses this concept. |
| Owner/Tenant Relationship | A person's dated relationship with a unit, separate from management authority. |

Managers and residents sign in through the same frontend, with different access rights. The detailed permission matrix is not finalized. The Manager role does not imply unrestricted access.

## 3. Core Domain Decisions — Explicitly Approved

### DDD-001 — Unit-Based Debt Account

Debt is tracked against the unit rather than the person.

Consequences:

- The unit's account and history remain intact when its owner or tenant changes.
- The account holding a charge and the people associated with that charge are separate concepts.
- This tracking model does not, by itself, establish legal payment liability.
- Rules for assigning or displaying historical debt to a new person are not finalized.

### DDD-002 — Dated Owner/Tenant Relationships

Owner and tenant relationships have start and end dates.

Consequences:

- When a person changes, the previous relationship is ended rather than deleted, and a new relationship is created.
- Historical relationships are retained.
- Retaining a historical relationship does not imply indefinite application access for a former user.
- Period boundaries, mid-month changes, and retrospective corrections require further definition.

### DDD-003 — Separate Management Authority and Unit Relationships

Management authority is maintained independently from owner/tenant relationships.

Consequences:

- A manager does not need to own or rent a unit in the community.
- A person may be both a manager and a resident.
- Ownership or tenancy does not automatically grant management authority.
- Ending management authority does not delete financial records or unit relationships.

### DDD-004 — Resident Access to the Current Community Directory

Residents may view the people currently living in their own units and in other units of the same community.

Consequences:

- Residence is explicit and separate from ownership, tenancy, and management permissions. Household members may be residents without being owners or tenants.
- Resident directory access is read-only and shows current names and unit associations.
- This access does not expand permission to view other units' finances, historical owner/tenant relationships, or historical residence records.
- A dedicated resident projection is used rather than exposing the manager response and hiding fields in the UI.

## 4. Conceptual Model — Draft

The following names and boundaries were proposed during discussion. They are not final class, aggregate, or collection designs.

| Concept | Proposed Code Name | Responsibility |
|---|---|---|
| Managed community | `Community` | The managed apartment building or residential community, not the management company. |
| Block | `Block` | An optional building/block within a community. |
| Unit | `Unit` | An apartment, shop, or office and its account context. |
| Person | `Person` | A person who may be recorded without having an application account. |
| User account | `UserAccount` | The authentication and sign-in account. |
| Unit relationship | `UnitRelationship` | A dated owner/tenant relationship between a person and a unit. |
| Community membership/authority | `CommunityMembership` | A user's role and permissions within a community. |
| Charge | `Charge` | A maintenance fee or additional charge issued to a unit. |
| Charge batch | `ChargeBatch` | Issuing charges to multiple units in one operation. |
| Charge party association | `ChargeParty` | A proposed link between a charge and relevant dated unit relationships. |
| Payment notification | `PaymentNotification` | A resident's notification that they have made a payment. |
| Recorded payment | `Payment` | An incoming payment verified by a manager. |
| Payment allocation | `PaymentAllocation` | The amounts by which a payment settles specific charges. |
| Expense | `Expense` | An expense incurred for the community. |
| Expense payment | `ExpensePayment` | An outgoing payment against an expense. |
| Financial account | `FinancialAccount` | A bank account or cash account. |
| Announcement | `Announcement` | Content published to a specified audience. |
| Service request | `ServiceRequest` | A fault report, complaint, or service request. |

Proposed relationships:

```text
Community
  ├── Block (optional)
  ├── Unit
  │     ├── UnitRelationship → Person
  │     └── Charge
  └── CommunityMembership → UserAccount

UserAccount → Person
Payment → PaymentAllocation → Charge
PaymentNotification → Payment (after approval/matching)
Expense → ExpensePayment → FinancialAccount
```

This diagram shows conceptual relationships. It does not prescribe aggregate ownership or MongoDB embedding.

## 5. Module Boundaries — Draft

Proposed modules within a single monolith:

| Module | Responsibility |
|---|---|
| `identity` | User accounts and authentication. |
| `community` | Communities, blocks, units, people, and relationships. |
| `access` | Manager/resident access and permissions. |
| `finance` | Charges, payments, allocations, expenses, and financial accounts. |
| `communication` | Announcements and audiences. |
| `service` | Requests, assignments, and status history. |
| `notification` | In-app notifications and Android push delivery. |

Module boundaries do not imply separate services. Exact aggregate boundaries and inter-module contracts are not defined yet.

## 6. Financial Business Rules — Proposals Awaiting Approval

- Payment notifications and recorded payments are separate records. Uploading a transfer receipt alone does not settle debt.
- A manager may convert a notification into a recorded payment or match it to an existing payment.
- A manual payment may also be recorded without a notification.
- Partial payments and overpayments are supported.
- A payment may be allocated across multiple charges. In the first release, allocation stays within the same unit.
- Total allocations cannot exceed the payment amount, and allocation to a charge cannot exceed its outstanding amount.
- Unallocated funds remain available as credit on the unit's account.
- The charge period, issue date, and due date are separate.
- People associated with a charge cannot be inferred solely from its creation date; a charge may be issued retrospectively for an earlier period.
- Incurring an expense and paying it are separate events.
- Financial records are not silently deleted. Corrections and reversals remain traceable.
- Repeated requests do not produce duplicate charges or payments.

Proposed payment notification statuses: `PENDING`, `ACCEPTED`, `REJECTED`, `WITHDRAWN`.

Payment allocation order, late-charge calculations, cancellation/reversal mechanisms, and charge-party determination remain undecided.

## 7. Financial Visibility ? Confirmed

Residents may see financial records only for periods covered by their dated owner or tenant relationship with the unit. This decision applies to both owners and tenants. It determines visibility, not legal liability for a charge.

- Charge visibility uses the billing period, not the issue date. The relationship must cover the entire billing period; overlapping periods are not automatically prorated.
- Payment visibility requires a payment date within the relationship period and visible allocated charges.
- Resident account totals and statements are computed from visible records. The unit's unrestricted historical balance must not leak through summaries.
- Managers retain complete financial history within their authorized scope.
- New residents do not gain access to records preceding their relationship.

Continued access after a relationship ends, partially overlapping billing periods, and access to another person's payer details or receipts require separate decisions. This decision does not grant those permissions.

## 8. Technical Design Principles — Proposed

- DDD concepts are not mapped directly one-to-one to MongoDB collections.
- The entire community, its units, and its financial history are not embedded in one growing document.
- Community-scoped operations use `communityId` and server-side authorization.
- Units, charges, and financial accounts from different communities cannot be linked within the same financial operation.
- Monetary calculations do not use `double`; an appropriate decimal representation and currency are used together.
- Consistency across payments, allocations, and account movements is designed as a whole, including MongoDB transactions and concurrency control.
- Financial operations record the actor, time, and reason.
- Event timestamps are distinguished from calendar dates such as billing periods and due dates. User-facing times account for the Türkiye time zone.
- Separate frontends do not replace authorization.

## 9. Next Domain Design Work

1. Finalize charge and payment visibility rules.
2. Define the operation-level permission matrix for managers and residents.
3. Specify examples for charging, partial/overpayments, resident changes, and corrections.
4. Define aggregate boundaries, invariants, and transaction boundaries.
5. Define domain events and application commands.
6. Design the MongoDB physical model and indexes.
7. Prepare Qwen development tasks with API contracts and acceptance criteria.

The admin panel, subscriptions/pricing, management-company accounting, and online payments are outside the current development scope.

## 10. Frontend Prototype Notes — Not Domain Approval

The web prototype now includes resident payment notifications and manager review. Its temporary REVIEWED state means only that a notification has been inspected. It does not create a Payment, allocate funds, or change balances. This UI prototype does not approve the draft financial rules or replace the proposed domain lifecycle. Attachments and new records are held only in session memory.

The read-only manager unit directory displays separate people, units, and dated relationships from synthetic fixtures. Its account summary separates charges, recorded payments, and allocations from payment notifications. No admin edits or financial writes are enabled. Date-boundary behavior is documented as a fixture-view convention and does not settle the open domain decisions.

Resident directory access is now confirmed by DDD-004. The prototype serves a current-only resident fixture separately from the manager fixture. This supersedes the earlier manager-only directory restriction; financial visibility decisions remain unchanged.

The frontend request prototype now uses explicit creator identity for resident visibility and supports descriptions, photos, comments, assignment, and status/assignment history. Announcement drafts/publication and community/block/unit audiences are implemented in the prototype. Residents cannot use editor routes or view drafts/out-of-audience announcements. These session-only behaviors do not replace server permissions or finalize the complete domain lifecycle, staff model, notification policy, or audit/versioning requirements.

## 11. Phase-One Web Workflows and API Preparation

The web frontend now includes charge batches, explicit payment allocations, unallocated credit, payment-notification matching, reasoned corrections, expense capture/payment/reversal, account movements, community/unit context selection, profile/preferences, notification reads and authentication previews.

These are executable mock workflows, not a backend implementation or final approval of unresolved financial rules. The mock uses fixed explicit charge amounts, manual allocation and conservative correction behavior. It does not infer legal liability, historical resident visibility, proration, late fees or interest.

A proposed OpenAPI 3.0.3 contract contains 59 operations and 69 schemas. Generated TypeScript DTOs/operation types, a typed HTTP client, repositories and wire/view adapters accompany it. See `docs/BACKEND-HANDOFF.md`, `docs/API-ENDPOINTS.md` and `docs/FRONTEND-READINESS.md` for scope, integration points, authority boundaries and outstanding decisions.

The backend must enforce community membership, object scope, capabilities, file access, financial transaction consistency, optimistic versions and idempotency. Browser role switches, profile storage and mock session expiry are preview conveniences only. The current frontend remains on its in-memory store until server-backed reads/writes and session bootstrap are connected.

## 12. Backend Implementation Decisions

The manager/resident backend is now implemented in `backend/`. Earlier prototype notes describe the historical frontend state; the web workspace still uses that mock state until live integration is completed.

- Domain models and application ports/use cases are plain Java. Spring Boot, MongoDB, HTTP, GridFS, mail, and cryptography remain in adapters or configuration.
- Financial consistency spans charges, explicit allocations, unit-account summaries, financial accounts, account entries, notification links, audit records, and outbox events. Related changes commit in one MongoDB transaction.
- Optimistic versions protect mutable aggregates. Financial command results and their payload fingerprints are stored atomically for idempotent retries.
- Payments allocate only to explicitly selected same-unit charges. Excess remains credit without automatic allocation. A notification review does not record money; linking an existing payment does not create a second cash movement.
- Payment reversal releases allocations and restores linked notifications to REVIEWED. An allocated charge cannot be cancelled before correcting its payments. Expense accrual, settlement, and settlement reversal are separate records.
- Comments, request events, account movements, audit records, and outbox events use separate collections. Unit-account versions provide stable movement order when timestamps coincide.
- The approved relationship-period financial visibility rule in section 7 is the default backend policy. It does not assign legal debt liability or grant access after a relationship ends.
- Demo provisioning is enabled only in explicit development/test profiles. Admin onboarding, online payments, and company accounting remain outside the current scope.

See [the backend guide](backend/README.md) for runtime configuration, architecture, limits, and verification commands. The current verification run passed 59 unit/architecture/contract tests and 26 real-HTTP Gherkin scenarios against a disposable MongoDB replica set. Frontend contract validation and its production build also passed. Live packaged-application startup, health, login, community context, and logout were verified against the Compose database.

## 13. Live Web API Integration

The web frontend now uses cookie sessions, server memberships/capabilities, paginated reads, private uploads, and versioned API commands. Backend projections supply financial totals. Local business fixtures and demo role entry have been removed. Browser tests use a separate disposable database; see the frontend guide for startup and verification. Admin and Android scope remain deferred.
