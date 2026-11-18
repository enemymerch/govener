Feature: Open unit-based community polls
  Each currently eligible unit has one immutable vote, regardless of its number of occupants or owners.

  Scenario: Current residents vote once per unit and everyone eligible can read live results
    When residents cast open unit votes and another resident attempts a duplicate
    Then the workflow preserves its financial and access rules

  Scenario: An owner with three units can cast three equal votes in one atomic submission
    When an owner votes for three owned units
    Then the workflow preserves its financial and access rules

  Scenario: Managers publish and close polls while permissions and deadlines are enforced
    When poll publication closure and authorization rules are exercised
    Then the workflow preserves its financial and access rules

  Scenario: Simultaneous votes cannot overwrite the first vote for a unit
    When two residents concurrently vote for the same unit
    Then the workflow preserves its financial and access rules
