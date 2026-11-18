package com.govener.finance.domain;

import com.govener.shared.domain.*;

public record FinancialAccount(
    String id,
    String communityId,
    String name,
    String kind,
    long openingMinor,
    long balanceMinor,
    long version,
    String bankName,
    String iban)
    implements ScopedAggregate {
  public FinancialAccount(
      String id,
      String communityId,
      String name,
      String kind,
      long openingMinor,
      long balanceMinor,
      long version) {
    this(id, communityId, name, kind, openingMinor, balanceMinor, version, "", "");
  }

  public FinancialAccount {
    bankName = Checks.optional(bankName, 150);
    iban = Checks.optional(iban, 50);
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
        version + 1,
        bankName,
        iban);
  }
}
