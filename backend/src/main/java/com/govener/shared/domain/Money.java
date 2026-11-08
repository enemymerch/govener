package com.govener.shared.domain;

public record Money(long minor) {
  public static final long MAX_SAFE = 9_007_199_254_740_991L;
  public static final long MAX_TRANSACTION = 999_999_999L;

  public Money {
    if (minor > MAX_SAFE || minor < -MAX_SAFE)
      throw RuleViolation.invalid("Money is outside the supported range.");
  }

  public static Money positive(long minor) {
    if (minor <= 0 || minor > MAX_TRANSACTION)
      throw RuleViolation.invalid("Amount must be positive and within the transaction limit.");
    return new Money(minor);
  }

  public Money plus(Money other) {
    try {
      return new Money(Math.addExact(minor, other.minor));
    } catch (ArithmeticException ex) {
      throw RuleViolation.invalid("Money overflow.");
    }
  }

  public Money minus(Money other) {
    try {
      return new Money(Math.subtractExact(minor, other.minor));
    } catch (ArithmeticException ex) {
      throw RuleViolation.invalid("Money overflow.");
    }
  }

  public String currency() {
    return "TRY";
  }
}
