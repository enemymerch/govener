# Manager navigation and financial pages

The manager application uses this menu order: Overview, Directory, Financial management, Announcements, Service requests, Polls, My profile, Notifications. The separate administration application and resident menu remain distinct.

## Directory

`/units` lists units, blocks, current owners and current tenants. Search matches unit labels and names; blocks can be filtered. Selecting a unit opens its identity and current owner/tenant relationships. Previous relationships are collapsed separately. Financial summaries no longer appear in this directory. Relationship dates are inclusive and evaluated using the Istanbul calendar date. Missing people are identified explicitly rather than inferred from residence records.

## Financial management

| Route | Purpose |
| --- | --- |
| `/finance` | Dues tracking, dues creation, payment recording and incoming transfer notifications |
| `/finance/balance` | Cash/bank balances, recorded bank metadata, expenses and settlements |
| `/finance/other` | Other collections, charge creation and payment recording |

The dues table displays unit, billing month, description, amount, paid amount, remaining amount and status. Search, year, month and status filters combine. The two summary widgets show all outstanding dues and outstanding dues belonging to the current Istanbul billing month. These are unpaid amounts, including partial balances; they are not restricted to overdue charges. Cancelled charges contribute zero. Summary widgets cover the community and are independent of table filters.

Charge creation explicitly records `DUES` or `OTHER`. The domain stores this classification and preserves it through allocations, reversals and cancellation. Existing records lacking a category deserialize as `UNSPECIFIED`; they are labelled under Other charges and excluded from dues totals. No description matching or destructive migration is performed. Existing clients may omit the optional request category and obtain `UNSPECIFIED`.

Payment recording continues to use explicit allocations, expected versions, server transactions and idempotency keys. Payments can cover multiple categories; the receipt history is therefore community-wide. Expense entry and settlement remain available under Balance. Transfer notifications are reviewed under the dues page; reviewing a notification does not itself record money. Existing account and payment URLs remain supported for backward compatibility.

Bank/cash balances come from stored financial accounts, including opening balances and committed movements. Optional `bankName` and `iban` metadata are displayed when recorded and retained during movements. Empty bank metadata is clearly identified; no bank connectivity, fabricated account details or automatic bank synchronization is implied. Account provisioning/editing is a separate administration concern.

## Responsive menu

The sidebar can be closed on desktop. On phones it starts closed and opens as a drawer, with a close button, background dismissal and Escape support. Selecting a route closes the mobile drawer. Hidden navigation is inert so it cannot receive keyboard focus.

## Polls

Managers can create and publish polls, while eligible owners or residents can vote by unit. Results and open voting records are visible while a poll is running. See [poll domain, permissions and API](POLLS.md).
