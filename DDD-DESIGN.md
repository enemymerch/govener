# Govener — DDD Design Decisions

Last updated: November 8, 2026

This document records decisions discussed for the apartment and residential community management application. **Confirmed decisions** are separate from **draft proposals**. Draft proposals are not approved implementation requirements. When development starts with Qwen, unresolved questions must not be converted into business rules through assumptions.

Documentation language: English. Discussion may continue in Turkish.

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

## 7. Visibility — Open Decision

The following approach has been proposed but has not been approved by the user:

- A tenant sees only charges and payments associated with them.
- An owner sees the unit account for their ownership period.
- A manager sees the unit's complete financial history within their authorized scope.
- A new tenant is not automatically associated with historical debt.

The proposal must not be treated as an implemented rule until these questions are resolved:

1. How should historical debt be shown to or hidden from a new resident?
2. How should the unit's total balance be distinguished from the balance of transactions visible to a person?
3. How long and to what extent may a person access historical records after their relationship ends?
4. How should owner and tenant access differ within the same period?
5. Who may access payer details and personal documents such as bank transfer receipts?

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
