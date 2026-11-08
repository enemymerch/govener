package com.govener.finance.domain;

import com.govener.shared.domain.*;
import java.time.LocalDate;

public record Expense(
    String id,
    String communityId,
    String description,
    String category,
    String vendor,
    LocalDate incurredOn,
    Money amount,
    long paidMinor,
    String documentId,
    long version)
    implements ScopedAggregate {
  public Expense {
    Checks.id(id);
    Checks.id(communityId);
    description = Checks.text(description, 300);
    vendor = Checks.optional(vendor, 150);
    Checks.required(incurredOn);
    Money.positive(amount.minor());
    if (!java.util.Set.of("MAINTENANCE", "UTILITIES", "CLEANING", "OTHER").contains(category)
        || paidMinor < 0
        || paidMinor > amount.minor()) throw RuleViolation.invalid("Invalid expense.");
  }

  public Expense pay(long value) {
    Money.positive(value);
    if (value > amount.minor() - paidMinor)
      throw RuleViolation.invalid("Payment exceeds the unpaid expense.");
    return new Expense(
        id,
        communityId,
        description,
        category,
        vendor,
        incurredOn,
        amount,
        paidMinor + value,
        documentId,
        version + 1);
  }

  public Expense reverse(long value) {
    if (value <= 0 || value > paidMinor)
      throw RuleViolation.conflict("Expense payment cannot be reversed.");
    return new Expense(
        id,
        communityId,
        description,
        category,
        vendor,
        incurredOn,
        amount,
        paidMinor - value,
        documentId,
        version + 1);
  }
}
