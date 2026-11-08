package com.govener.finance.domain;

import com.govener.shared.domain.*;
import java.time.*;

public record ExpensePayment(
    String id,
    String communityId,
    String expenseId,
    String accountId,
    LocalDate paidOn,
    Money amount,
    Instant reversedAt,
    String reversalReason,
    long version)
    implements ScopedAggregate {
  public ExpensePayment {
    Checks.id(id);
    Checks.id(communityId);
    Checks.id(expenseId);
    Checks.id(accountId);
    Checks.required(paidOn);
    Money.positive(amount.minor());
    reversalReason = Checks.optional(reversalReason, 500);
  }

  public ExpensePayment reverse(String reason, Instant now) {
    if (reversedAt != null) throw RuleViolation.conflict("Expense payment is already reversed.");
    return new ExpensePayment(
        id,
        communityId,
        expenseId,
        accountId,
        paidOn,
        amount,
        now,
        Checks.text(reason, 500),
        version + 1);
  }
}
