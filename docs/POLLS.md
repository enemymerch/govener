# Unit-based community polls

Managers with `POLL_MANAGE` can create polls, publish drafts and close voting. Residents and managers can vote only when they have an eligible unit; management authority itself never grants votes. The manager and resident application exposes the same Polls page with capability-specific actions. The page and forms support Turkish and English.

## Rules

- The audience is either `RESIDENTS` (current explicit residence records) or `OWNERS` (current dated OWNER relationships). There is no combined audience.
- Eligibility uses the membership's community-specific `personId` and today's Istanbul calendar date. A tenancy alone is not evidence of physical residence. An ended residence/ownership does not grant a vote. An active community membership and `COMMUNITY_READ` are required.
- Each eligible unit has one equally weighted vote. An owner with three units can submit three unit ballots together, or choose a different option for each unit in separate submissions.
- The first vote is final for that unit, including when another eligible owner or occupant submits it. Replacements and withdrawals are not supported.
- A multi-unit submission is atomic: if any unit has already voted or is ineligible, none of that submission's votes are saved.
- Voter name, unit label, selected option and voting time are visible to current audience members and community managers. Login emails and contact details are not exposed. Vote-time names and unit labels are retained as historical snapshots.
- Results are visible before voting and while voting remains open. The frontend refreshes the collection every ten seconds while the page is visible and offers manual refresh. This is periodic refresh, not a WebSocket stream.
- Two to ten distinct, non-empty options are required. Titles, descriptions and options are bounded. Creation requires a future closing timestamp.
- Drafts are visible only to managers. A draft may be published before its deadline. Poll questions/options are immutable after creation; there is no edit or delete endpoint in this release.
- Voting ends at the closing instant, or earlier through manager closure. Results and recorded votes remain readable after closure while the viewer remains authorized. This is a community opinion poll, not a legal assembly-resolution workflow.

## Domain and boundaries

`poll.domain.Poll` is an aggregate with immutable options and unit ballots. Pure Java state transitions enforce draft/publication/closure, deadline, one-vote-per-unit and expected version checks. `poll.application.PollService` resolves eligibility, actor identities and community scope through existing ports. It never trusts client voter names, user identifiers or unit ownership claims.

`PollPorts.Polls` is the output repository port. `MongoPollRepository` stores the aggregate in the `polls` collection. Spring wiring is confined to `PollConfiguration`; HTTP transport is in `PollController`. Poll writes and audit records commit in the same MongoDB transaction.

Votes and poll state share one optimistic version. Competing votes or closure cannot silently overwrite each other. The loser receives HTTP 409 and must reload; neither the backend nor frontend automatically retries a mutation. Embedding ballots is appropriate for the initial community size but serializes writes per poll and remains subject to MongoDB document-size limits. Larger deployments should introduce a dedicated ballot repository with a unique poll/unit index and transactional lifecycle coordination.

## HTTP contract

All paths are under `/api/v1/communities/{communityId}` and use the existing HttpOnly session and CSRF token. See [OpenAPI](openapi.json).

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/polls` | Cursor-paginated visible polls, eligible units, results and open ballots |
| POST | `/polls` | Create an OPEN poll or DRAFT; returns 201 |
| GET | `/polls/{pollId}` | Read an authorized poll |
| POST | `/polls/{pollId}/publication` | Publish with `expectedVersion` |
| POST | `/polls/{pollId}/closure` | Close with `expectedVersion` |
| POST | `/polls/{pollId}/votes` | Submit `optionId`, distinct `unitIds`, and `expectedVersion` |

Publication, closure and voting return 200 with the updated view. Votes are counted by unit; the result contains `eligibleUnits[].voted` so a shared unit already represented in the poll cannot be selected again. Cross-community and audience-ineligible poll reads return 404. Unauthorized creation returns 403. Invalid dates/options return 422; version, state or duplicate-unit conflicts return 409.

## Rollout and verification

The synthetic dev/test manager receives `POLL_MANAGE`; the idempotent development fixture also upgrades its existing demo membership. Production memberships require explicit permission assignment. The administration frontend includes the permission when provisioning managers. Fixed resident read permissions do not include it.

Domain tests cover multiple-unit weighting, immutable first votes, atomic batches, options, deadlines, drafts, closure and versions. Gherkin scenarios exercise real HTTP/MongoDB, ownership/residence eligibility, audience isolation, permissions and concurrent voting. Playwright tests cover manager publication, resident voting, live public results, shared-unit locking, three-unit owner voting, persistence, closure and mobile Turkish UI.
