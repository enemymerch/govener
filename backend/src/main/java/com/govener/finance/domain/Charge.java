package com.govener.finance.domain;

import com.govener.shared.domain.*;
import java.time.*;

public record Charge(
    String id,
    String communityId,
    String unitId,
    String description,
    BillingPeriod period,
    LocalDate issuedOn,
    LocalDate dueOn,
    Money amount,
    long allocatedMinor,
    Instant cancelledAt,
    String cancellationReason,
    long version)
    implements ScopedAggregate {
  public Charge {
    Checks.id(id);
    Checks.id(communityId);
    Checks.id(unitId);
    description = Checks.text(description, 300);
    Checks.required(period);
    Checks.required(issuedOn);
    Checks.required(dueOn);
    Money.positive(amount.minor());
    if (allocatedMinor < 0 || allocatedMinor > amount.minor())
      throw RuleViolation.invalid("Invalid allocated amount.");
    cancellationReason = Checks.optional(cancellationReason, 500);
  }

  public long outstandingMinor() {
    return cancelledAt == null ? amount.minor() - allocatedMinor : 0;
  }

  public String status() {
    return cancelledAt != null
        ? "CANCELLED"
        : allocatedMinor == 0
            ? "OUTSTANDING"
            : allocatedMinor == amount.minor() ? "PAID" : "PARTIALLY_PAID";
  }

  public Charge allocate(long value) {
    Money.positive(value);
    if (cancelledAt != null || value > outstandingMinor())
      throw RuleViolation.invalid("Allocation exceeds the outstanding charge.");
    return copy(allocatedMinor + value, null, cancellationReason);
  }

  public Charge release(long value) {
    if (value <= 0 || value > allocatedMinor)
      throw RuleViolation.conflict("Allocation cannot be released.");
    return copy(allocatedMinor - value, cancelledAt, cancellationReason);
  }

  public Charge cancel(String reason, Instant now) {
    if (cancelledAt != null) throw RuleViolation.conflict("Charge is already cancelled.");
    if (allocatedMinor > 0)
      throw RuleViolation.conflict("Correct allocated payments before cancelling a charge.");
    return copy(allocatedMinor, now, Checks.text(reason, 500));
  }

  private Charge copy(long allocated, Instant cancelled, String reason) {
    return new Charge(
        id,
        communityId,
        unitId,
        description,
        period,
        issuedOn,
        dueOn,
        amount,
        allocated,
        cancelled,
        reason,
        version + 1);
  }
}
