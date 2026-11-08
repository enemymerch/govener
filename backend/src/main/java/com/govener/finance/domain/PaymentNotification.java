package com.govener.finance.domain;

import com.govener.shared.domain.*;
import java.time.LocalDate;

public record PaymentNotification(
    String id,
    String communityId,
    String userId,
    String unitId,
    Money amount,
    LocalDate paymentDate,
    String reference,
    String note,
    String receiptId,
    String status,
    String reviewNote,
    String paymentId,
    long version)
    implements ScopedAggregate {
  public PaymentNotification {
    Checks.id(id);
    Checks.id(communityId);
    Checks.id(userId);
    Checks.id(unitId);
    Money.positive(amount.minor());
    Checks.required(paymentDate);
    reference = Checks.text(reference, 100);
    note = Checks.optional(note, 500);
    reviewNote = Checks.optional(reviewNote, 500);
    if (!java.util.Set.of("PENDING", "REVIEWED", "REJECTED", "WITHDRAWN", "RECORDED")
        .contains(status)) throw RuleViolation.invalid("Invalid notification state.");
  }

  public PaymentNotification review(String decision, String reason) {
    if (!status.equals("PENDING"))
      throw RuleViolation.conflict("Only pending notifications can be reviewed.");
    if (!java.util.Set.of("REVIEWED", "REJECTED").contains(decision))
      throw RuleViolation.invalid("Invalid review decision.");
    if (decision.equals("REJECTED")) Checks.text(reason, 500);
    return copy(decision, reason, null);
  }

  public PaymentNotification withdraw() {
    if (!status.equals("PENDING"))
      throw RuleViolation.conflict("Only pending notifications can be withdrawn.");
    return copy("WITHDRAWN", reviewNote, null);
  }

  public PaymentNotification record(String payment) {
    if (!java.util.Set.of("PENDING", "REVIEWED").contains(status) || paymentId != null)
      throw RuleViolation.conflict("Notification cannot be linked.");
    return copy("RECORDED", reviewNote, payment);
  }

  public PaymentNotification release() {
    if (!status.equals("RECORDED")) throw RuleViolation.conflict("Notification is not linked.");
    return copy("REVIEWED", reviewNote, null);
  }

  private PaymentNotification copy(String state, String reason, String payment) {
    return new PaymentNotification(
        id,
        communityId,
        userId,
        unitId,
        amount,
        paymentDate,
        reference,
        note,
        receiptId,
        state,
        reason,
        payment,
        version + 1);
  }
}
