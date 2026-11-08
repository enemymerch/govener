package com.govener.finance.application;

import com.govener.finance.domain.*;
import com.govener.shared.application.AggregateRepository;
import java.util.Optional;

public final class FinancePorts {
  private FinancePorts() {}

  public interface Charges extends AggregateRepository<Charge> {}

  public interface Payments extends AggregateRepository<Payment> {}

  public interface Accounts extends AggregateRepository<FinancialAccount> {}

  public interface UnitAccounts extends AggregateRepository<UnitAccount> {
    Optional<UnitAccount> forUnit(String communityId, String unitId);
  }

  public interface Entries extends AggregateRepository<AccountEntry> {}

  public interface Expenses extends AggregateRepository<Expense> {}

  public interface ExpensePayments extends AggregateRepository<ExpensePayment> {}

  public interface Notifications extends AggregateRepository<PaymentNotification> {}

  public interface Batches extends AggregateRepository<ChargeBatch> {}

  public interface Idempotency extends AggregateRepository<IdempotentResult> {}
}
