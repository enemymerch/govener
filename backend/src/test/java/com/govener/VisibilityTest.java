package com.govener;

import static org.assertj.core.api.Assertions.*;

import com.govener.community.domain.*;
import com.govener.finance.application.FinancialVisibility;
import com.govener.finance.domain.Charge;
import com.govener.shared.domain.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class VisibilityTest {
  private final LocalDate today = LocalDate.of(2026, 11, 8);

  private Membership resident(LocalDate start, LocalDate end) {
    return new Membership(
        "m",
        "site",
        "user",
        "person",
        Set.of(Role.RESIDENT),
        Set.of(Permission.FINANCE_READ),
        List.of(new UnitAccess("unit", start, end)),
        0);
  }

  private Charge charge(LocalDate start, LocalDate end) {
    return new Charge(
        "charge",
        "site",
        "unit",
        "Dues",
        new BillingPeriod(start, end),
        today,
        today,
        new Money(100),
        0,
        null,
        "",
        0);
  }

  @Test
  void hidesHistoricalDebtEvenWhenIssuedToday() {
    var member = resident(today.withDayOfMonth(1), null);
    assertThat(
            FinancialVisibility.relationshipPeriod()
                .chargeVisible(
                    member,
                    charge(
                        today.minusMonths(1).withDayOfMonth(1),
                        today.minusMonths(1).withDayOfMonth(31)),
                    today))
        .isFalse();
  }

  @Test
  void acceptsFullyCoveredBillingPeriod() {
    assertThat(
            FinancialVisibility.relationshipPeriod()
                .chargeVisible(
                    resident(today.withDayOfMonth(1), null),
                    charge(today.withDayOfMonth(1), today.withDayOfMonth(30)),
                    today))
        .isTrue();
  }

  @Test
  void doesNotAutomaticallyProratePartialPeriods() {
    assertThat(
            FinancialVisibility.relationshipPeriod()
                .chargeVisible(
                    resident(today, null),
                    charge(today.withDayOfMonth(1), today.withDayOfMonth(30)),
                    today))
        .isFalse();
  }

  @Test
  void endedRelationshipDoesNotGrantOngoingAccess() {
    assertThat(
            FinancialVisibility.relationshipPeriod()
                .chargeVisible(
                    resident(today.minusMonths(2), today.minusDays(1)),
                    charge(today.minusMonths(1), today.minusDays(2)),
                    today))
        .isFalse();
  }

  @Test
  void managerHasAuthorizedHistory() {
    var manager =
        new Membership(
            "m",
            "site",
            "manager",
            "p",
            Set.of(Role.MANAGER),
            Set.of(Permission.FINANCE_READ),
            List.of(),
            0);
    assertThat(
            FinancialVisibility.relationshipPeriod()
                .chargeVisible(manager, charge(today.minusYears(1), today.minusMonths(11)), today))
        .isTrue();
  }
}
