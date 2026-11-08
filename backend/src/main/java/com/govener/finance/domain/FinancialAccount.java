package com.govener.finance.domain;

import com.govener.shared.domain.*;

public record FinancialAccount(
    String id,
    String communityId,
    String name,
    String kind,
    long openingMinor,
    long balanceMinor,
    long version)
    implements ScopedAggregate {
  public FinancialAccount {
    Checks.id(id);
    Checks.id(communityId);
    name = Checks.text(name, 150);
    if (!java.util.Set.of("BANK", "CASH").contains(kind))
      throw RuleViolation.invalid("Invalid financial account type.");
    new Money(openingMinor);
    new Money(balanceMinor);
  }

  public FinancialAccount move(long delta) {
    return new FinancialAccount(
        id,
        communityId,
        name,
        kind,
        openingMinor,
        new Money(balanceMinor).plus(new Money(delta)).minor(),
        version + 1);
  }
}
