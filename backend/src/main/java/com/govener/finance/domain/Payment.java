package com.govener.finance.domain;

import com.govener.shared.domain.*;
import java.time.*;
import java.util.*;

public record Payment(
    String id,
    String communityId,
    String unitId,
    String accountId,
    Money amount,
    LocalDate paidOn,
    String reference,
    List<Allocation> allocations,
    String notificationId,
    Instant reversedAt,
    String reversalReason,
    long version)
    implements ScopedAggregate {
  public Payment {
    Checks.id(id);
    Checks.id(communityId);
    Checks.id(unitId);
    Checks.id(accountId);
    Money.positive(amount.minor());
    Checks.required(paidOn);
    reference = Checks.text(reference, 100);
    allocations = List.copyOf(allocations);
    long allocated = 0;
    Set<String> ids = new HashSet<>();
    for (Allocation allocation : allocations) {
      if (!ids.add(allocation.chargeId())) throw RuleViolation.invalid("Duplicate allocation.");
      allocated = Math.addExact(allocated, allocation.amountMinor());
    }
    if (allocated > amount.minor())
      throw RuleViolation.invalid("Allocations exceed the payment amount.");
    reversalReason = Checks.optional(reversalReason, 500);
  }

  public long allocatedMinor() {
    return allocations.stream().mapToLong(Allocation::amountMinor).sum();
  }

  public long unallocatedMinor() {
    return amount.minor() - allocatedMinor();
  }

  public Payment reverse(String reason, Instant now) {
    if (reversedAt != null) throw RuleViolation.conflict("Payment is already reversed.");
    return new Payment(
        id,
        communityId,
        unitId,
        accountId,
        amount,
        paidOn,
        reference,
        allocations,
        notificationId,
        now,
        Checks.text(reason, 500),
        version + 1);
  }

  public Payment link(String notification) {
    if (reversedAt != null || notificationId != null)
      throw RuleViolation.conflict("Payment cannot be linked.");
    return new Payment(
        id,
        communityId,
        unitId,
        accountId,
        amount,
        paidOn,
        reference,
        allocations,
        Checks.id(notification),
        null,
        reversalReason,
        version + 1);
  }
}
