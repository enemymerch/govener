Feature: Manager and resident backend services
  All workflows use a real HTTP server and a real MongoDB replica set.

  Background:
    Given a community with a manager, residents and a separate community

  Scenario: Authorized community browsing
    When the manager and resident browse their authorized community
    Then the workflow preserves its financial and access rules

  Scenario: Community and permission isolation
    When a resident attempts to cross community and manager boundaries
    Then the workflow preserves its financial and access rules

  Scenario: Current household directory without financial data
    When a resident opens the resident directory
    Then the workflow preserves its financial and access rules

  Scenario: Idempotent financial writes
    When a financial command is retried with an idempotency key
    Then the workflow preserves its financial and access rules

  Scenario: Atomic charge batch rollback
    When the second item in a charge batch has a stale version
    Then the workflow preserves its financial and access rules

  Scenario: Partial payment and traceable reversal
    When a partial payment is recorded and reversed
    Then the workflow preserves its financial and access rules

  Scenario: Unallocated overpayment credit
    When an overpayment leaves unallocated credit
    Then the workflow preserves its financial and access rules

  Scenario: Allocation isolation by unit
    When a payment tries to allocate another unit's charge
    Then the workflow preserves its financial and access rules

  Scenario: Concurrent payment protection
    When two payments compete for the same account version
    Then the workflow preserves its financial and access rules

  Scenario: Financial history limited to the tenancy period
    When a new tenant views retrospective and current charges
    Then the workflow preserves its financial and access rules

  Scenario: Traceable charge cancellation
    When an unpaid charge is cancelled with a reason
    Then the workflow preserves its financial and access rules

  Scenario: Transfer review and existing-payment matching
    When a transfer notification is reviewed and matched without double counting
    Then the workflow preserves its financial and access rules

  Scenario: Resident withdrawal of pending transfers
    When a resident withdraws a pending transfer notification
    Then the workflow preserves its financial and access rules

  Scenario: Partial expense settlement and reversal
    When an expense is paid in parts and a payment is reversed
    Then the workflow preserves its financial and access rules

  Scenario: Private service request collaboration
    When a resident and manager collaborate on a service request
    Then the workflow preserves its financial and access rules

  Scenario: Draft and targeted announcement visibility
    When a manager publishes a draft to a selected unit
    Then the workflow preserves its financial and access rules

  Scenario: Idempotent outbox delivery and private notification reads
    When an outbox event becomes a private notification
    Then the workflow preserves its financial and access rules

  Scenario: Versioned profile and notification preferences
    When a user updates their profile and notification preferences
    Then the workflow preserves its financial and access rules

  Scenario: Signed cursor scope binding
    When a pagination cursor is reused with another filter or user
    Then the workflow preserves its financial and access rules

  Scenario: CSRF protection, strict input validation and logout
    When a request without CSRF or with unknown fields is submitted
    Then the workflow preserves its financial and access rules

  Scenario: Single-use password reset and session revocation
    When a password reset is consumed and then replayed
    Then the workflow preserves its financial and access rules

  Scenario: Receipt ownership and authorized parent access
    When a private receipt is uploaded and attached to a transfer notification
    Then the workflow preserves its financial and access rules

  Scenario: Atomic immediate expense settlement
    When an expense with immediate settlement references a foreign bank account
    Then the workflow preserves its financial and access rules

  Scenario: Exact transfer notification matching
    When a recorded payment must exactly match its transfer notification
    Then the workflow preserves its financial and access rules

  Scenario: Financial command header and date validation
    When a financial command omits its idempotency key or has an invalid date interval
    Then the workflow preserves its financial and access rules

  Scenario: Login brute force protection
    When invalid credentials repeatedly attempt to sign in
    Then the workflow preserves its financial and access rules
