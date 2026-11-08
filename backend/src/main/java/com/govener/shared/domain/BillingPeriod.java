package com.govener.shared.domain;

import java.time.LocalDate;

public record BillingPeriod(LocalDate start, LocalDate end) {
  public BillingPeriod {
    Checks.required(start);
    Checks.required(end);
    if (end.isBefore(start)) throw RuleViolation.invalid("Billing period end precedes its start.");
  }
}
