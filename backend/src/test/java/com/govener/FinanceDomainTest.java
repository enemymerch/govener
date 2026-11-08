package com.govener;

import static org.assertj.core.api.Assertions.*;

import com.govener.finance.application.*;
import com.govener.finance.domain.*;
import com.govener.shared.domain.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FinanceDomainTest {
  private final LocalDate date = LocalDate.of(2026, 11, 8);

  private Charge charge() {
    return new Charge(
        "charge",
        "site",
        "unit",
        "Dues",
        new BillingPeriod(date.withDayOfMonth(1), date.withDayOfMonth(30)),
        date,
        date,
        new Money(10000),
        0,
        null,
        "",
        0);
  }

  private Payment payment(long amount, List<Allocation> allocations) {
    return new Payment(
        "payment",
        "site",
        "unit",
        "bank",
        new Money(amount),
        date,
        "Transfer",
        allocations,
        null,
        null,
        "",
        0);
  }

  @ParameterizedTest
  @ValueSource(longs = {0, -1, 1000000000L, Long.MAX_VALUE})
  void rejectsInvalidTransactionAmounts(long amount) {
    assertThatThrownBy(() -> Money.positive(amount)).isInstanceOf(RuleViolation.class);
  }

  @Test
  void rejectsUnsafeAggregateTotals() {
    assertThatThrownBy(() -> new Money(Money.MAX_SAFE).plus(new Money(1)))
        .isInstanceOf(RuleViolation.class);
  }

  @Test
  void calculatesKurusExactly() {
    assertThat(new Money(100).plus(new Money(29)).minus(new Money(1)).minor()).isEqualTo(128);
  }

  @Test
  void validatesBillingPeriod() {
    assertThatThrownBy(() -> new BillingPeriod(date, date.minusDays(1)))
        .isInstanceOf(RuleViolation.class);
  }

  @Test
  void supportsPartialAllocationAndRelease() {
    Charge partial = charge().allocate(4000);
    assertThat(partial.status()).isEqualTo("PARTIALLY_PAID");
    assertThat(partial.outstandingMinor()).isEqualTo(6000);
    assertThat(partial.release(4000).allocatedMinor()).isZero();
    assertThat(partial.allocate(6000).status()).isEqualTo("PAID");
  }

  @Test
  void rejectsOverAllocation() {
    assertThatThrownBy(() -> charge().allocate(10001)).isInstanceOf(RuleViolation.class);
  }

  @Test
  void allocatedChargeCannotBeCancelled() {
    assertThatThrownBy(() -> charge().allocate(1).cancel("Correction", Instant.now()))
        .isInstanceOf(RuleViolation.class);
  }

  @Test
  void cancellationIsTraceableAndFinal() {
    Charge cancelled = charge().cancel("Duplicate charge", Instant.now());
    assertThat(cancelled.status()).isEqualTo("CANCELLED");
    assertThat(cancelled.cancellationReason()).isEqualTo("Duplicate charge");
    assertThatThrownBy(() -> cancelled.allocate(1)).isInstanceOf(RuleViolation.class);
    assertThatThrownBy(() -> cancelled.cancel("Again", Instant.now()))
        .isInstanceOf(RuleViolation.class);
  }

  @Test
  void rejectsDuplicateChargeAllocations() {
    assertThatThrownBy(
            () -> payment(100, List.of(new Allocation("charge", 50), new Allocation("charge", 50))))
        .isInstanceOf(RuleViolation.class);
  }

  @Test
  void rejectsAllocationBeyondPayment() {
    assertThatThrownBy(() -> payment(100, List.of(new Allocation("charge", 101))))
        .isInstanceOf(RuleViolation.class);
  }

  @Test
  void leavesExcessPaymentAsCredit() {
    Payment value = payment(12000, List.of(new Allocation("charge", 10000)));
    assertThat(value.unallocatedMinor()).isEqualTo(2000);
  }

  @Test
  void allocationPolicyRejectsOtherUnits() {
    Charge foreign =
        new Charge(
            "charge",
            "site",
            "other",
            "Dues",
            charge().period(),
            date,
            date,
            new Money(10000),
            0,
            null,
            "",
            0);
    assertThatThrownBy(
            () ->
                new ManualAllocationPolicy()
                    .allocate(payment(100, List.of(new Allocation("charge", 100))), id -> foreign))
        .isInstanceOf(RuleViolation.class);
  }

  @Test
  void allocationPolicyRejectsOtherCommunities() {
    Charge foreign =
        new Charge(
            "charge",
            "other",
            "unit",
            "Dues",
            charge().period(),
            date,
            date,
            new Money(10000),
            0,
            null,
            "",
            0);
    assertThatThrownBy(
            () ->
                new ManualAllocationPolicy()
                    .allocate(payment(100, List.of(new Allocation("charge", 100))), id -> foreign))
        .isInstanceOf(RuleViolation.class);
  }

  @Test
  void accountConservesBalancesAcrossPaymentAndReversal() {
    UnitAccount initial = new UnitAccount("account", "site", "unit", 0, 0, 0, 0, 0);
    UnitAccount charged = initial.charge(10000);
    UnitAccount paid = charged.receive(12000, 10000);
    assertThat(paid.netMinor()).isEqualTo(-2000);
    assertThat(paid.outstandingMinor()).isZero();
    assertThat(paid.creditMinor()).isEqualTo(2000);
    UnitAccount reversed = paid.reverse(12000, 10000);
    assertThat(reversed.netMinor()).isEqualTo(10000);
    assertThat(reversed.paidMinor()).isZero();
    assertThat(reversed.cancel(10000).netMinor()).isZero();
  }

  @Test
  void invalidSummaryCannotBePersisted() {
    assertThatThrownBy(() -> new UnitAccount("account", "site", "unit", 100, 0, 50, 0, 0))
        .isInstanceOf(RuleViolation.class);
  }

  @Test
  void paymentReversalRequiresReasonAndCannotRepeat() {
    Payment original = payment(100, List.of());
    assertThatThrownBy(() -> original.reverse("", Instant.now())).isInstanceOf(RuleViolation.class);
    assertThatThrownBy(
            () -> original.reverse("Wrong transfer", Instant.now()).reverse("Again", Instant.now()))
        .isInstanceOf(RuleViolation.class);
  }

  @Test
  void notificationReviewDoesNotRecordMoney() {
    PaymentNotification pending =
        new PaymentNotification(
            "notice",
            "site",
            "user",
            "unit",
            new Money(100),
            date,
            "Ref",
            "",
            null,
            "PENDING",
            "",
            null,
            0);
    assertThatThrownBy(() -> pending.review("REJECTED", "")).isInstanceOf(RuleViolation.class);
    var reviewed = pending.review("REVIEWED", "Verified");
    assertThat(reviewed.paymentId()).isNull();
    assertThat(reviewed.record("payment").release().status()).isEqualTo("REVIEWED");
    assertThatThrownBy(() -> reviewed.withdraw()).isInstanceOf(RuleViolation.class);
  }

  @Test
  void expenseSupportsPartialPaymentAndReversal() {
    Expense expense =
        new Expense(
            "expense",
            "site",
            "Repair",
            "MAINTENANCE",
            "Vendor",
            date,
            new Money(1000),
            0,
            null,
            0);
    assertThat(expense.pay(400).paidMinor()).isEqualTo(400);
    assertThat(expense.pay(400).reverse(400).paidMinor()).isZero();
    assertThatThrownBy(() -> expense.pay(1001)).isInstanceOf(RuleViolation.class);
  }
}
