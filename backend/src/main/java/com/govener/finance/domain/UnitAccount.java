package com.govener.finance.domain;

import com.govener.shared.domain.*;

public record UnitAccount(
    String id,
    String communityId,
    String unitId,
    long chargedMinor,
    long paidMinor,
    long outstandingMinor,
    long creditMinor,
    long version)
    implements ScopedAggregate {
  public UnitAccount {
    new Money(chargedMinor);
    new Money(paidMinor);
    new Money(outstandingMinor);
    new Money(creditMinor);
    if (chargedMinor < 0 || paidMinor < 0 || outstandingMinor < 0 || creditMinor < 0)
      throw RuleViolation.conflict("Account invariant violated.");
    if (chargedMinor - paidMinor != outstandingMinor - creditMinor)
      throw RuleViolation.conflict("Account does not balance.");
  }

  public UnitAccount charge(long amount) {
    return change(amount, 0, amount, 0);
  }

  public UnitAccount cancel(long amount) {
    return change(-amount, 0, -amount, 0);
  }

  public UnitAccount receive(long amount, long allocated) {
    return change(0, amount, -allocated, amount - allocated);
  }

  public UnitAccount reverse(long amount, long allocated) {
    return change(0, -amount, allocated, -(amount - allocated));
  }

  private UnitAccount change(long charged, long paid, long outstanding, long credit) {
    return new UnitAccount(
        id,
        communityId,
        unitId,
        new Money(chargedMinor).plus(new Money(charged)).minor(),
        new Money(paidMinor).plus(new Money(paid)).minor(),
        new Money(outstandingMinor).plus(new Money(outstanding)).minor(),
        new Money(creditMinor).plus(new Money(credit)).minor(),
        version + 1);
  }

  public long netMinor() {
    return outstandingMinor - creditMinor;
  }
}
