package com.govener.finance.application;

import com.govener.finance.domain.Allocation;
import java.time.LocalDate;
import java.util.List;

public final class FinanceCommands {
  private FinanceCommands() {}

  public record ChargeItem(
      String unitId,
      String description,
      LocalDate periodStart,
      LocalDate periodEnd,
      LocalDate dueOn,
      long amountMinor,
      long expectedAccountVersion) {}

  public record ChargeBatch(List<ChargeItem> items) {
    public ChargeBatch {
      items = List.copyOf(items);
    }
  }

  public record RecordPayment(
      String unitId,
      String accountId,
      long amountMinor,
      LocalDate paidOn,
      String reference,
      List<Allocation> allocations,
      String notificationId,
      long expectedAccountVersion) {
    public RecordPayment {
      allocations = List.copyOf(allocations);
    }
  }

  public record Reason(String reason, long expectedVersion) {}

  public record Notify(
      String unitId,
      long amountMinor,
      LocalDate paymentDate,
      String reference,
      String note,
      String receiptId) {}

  public record Review(String decision, String note, long expectedVersion) {}

  public record Link(String paymentId, long expectedVersion, long expectedPaymentVersion) {}

  public record ImmediatePayment(String accountId, LocalDate paidOn) {}

  public record RecordExpense(
      String description,
      String category,
      String vendor,
      LocalDate incurredOn,
      long amountMinor,
      String documentId,
      ImmediatePayment immediatePayment) {}

  public record PayExpense(
      String accountId, LocalDate paidOn, long amountMinor, long expectedVersion) {}
}
