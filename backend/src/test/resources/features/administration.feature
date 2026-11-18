Feature: Separate administrative authority
  Administrators provision only assigned communities; root manages administrators globally.

  Scenario: Admin authority is separate from manager and resident membership
    When administrative scope boundaries are exercised
    Then only assigned communities and allowed administrative actions succeed

  Scenario: Root provisions communities and administrators and revokes access
    When root provisions an administrator and changes its assignments
    Then the administrator immediately follows the current assignments

  Scenario: Resident provisioning preserves dated unit relationships atomically
    When an administrator provisions a resident with a dated tenancy
    Then the new resident can sign in and access only its assigned unit

  Scenario: Invalid provisioning and stale unit updates leave no partial changes
    When invalid user provisioning and stale unit updates are attempted
    Then user creation is atomic and unit versions are protected
