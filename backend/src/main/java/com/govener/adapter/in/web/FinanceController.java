package com.govener.adapter.in.web;

import static com.govener.adapter.in.web.Views.map;

import com.govener.adapter.in.web.dto.*;
import com.govener.community.application.*;
import com.govener.finance.application.*;
import com.govener.identity.adapter.in.security.CurrentActor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class FinanceController {
  private final FinanceService finance;
  private final FinanceQueries queries;
  private final CurrentActor current;
  private final ViewMapper views;
  private final PageResponder pages;

  public FinanceController(
      FinanceService finance,
      FinanceQueries queries,
      CurrentActor current,
      ViewMapper views,
      PageResponder pages) {
    this.finance = finance;
    this.queries = queries;
    this.current = current;
    this.views = views;
    this.pages = pages;
  }

  @GetMapping("/communities/{communityId}/financial-accounts")
  public Object listFinancialAccounts(
      @PathVariable String communityId,
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
    return pages.page(
        queries.financialAccounts(current.id(), communityId),
        communityId,
        "listFinancialAccounts",
        map(),
        cursor,
        limit,
        views::cash);
  }

  @GetMapping("/communities/{communityId}/unit-accounts")
  public Object listUnitAccounts(
      @PathVariable String communityId,
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
    return pages.page(
        queries.accounts(current.id(), communityId),
        communityId,
        "listUnitAccounts",
        map(),
        cursor,
        limit,
        item -> item.unitId(),
        views::account);
  }

  @GetMapping("/communities/{communityId}/unit-accounts/{unitId}")
  public Object getUnitAccount(@PathVariable String communityId, @PathVariable String unitId) {
    return views.account(queries.account(current.id(), communityId, unitId));
  }

  @GetMapping("/communities/{communityId}/unit-accounts/{unitId}/entries")
  public Object listAccountEntries(
      @PathVariable String communityId,
      @PathVariable String unitId,
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
      @RequestParam(required = false)
          @org.springframework.format.annotation.DateTimeFormat(
              iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
          LocalDate from,
      @RequestParam(required = false)
          @org.springframework.format.annotation.DateTimeFormat(
              iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
          LocalDate to) {
    if (from != null && to != null && from.isAfter(to))
      throw com.govener.shared.domain.RuleViolation.invalid("Start date must not follow end date.");
    return pages.page(
        queries.entries(current.id(), communityId, unitId).stream()
            .filter(
                item ->
                    (from == null
                            || !item.occurredAt()
                                .atZone(ZoneId.of("Europe/Istanbul"))
                                .toLocalDate()
                                .isBefore(from))
                        && (to == null
                            || !item.occurredAt()
                                .atZone(ZoneId.of("Europe/Istanbul"))
                                .toLocalDate()
                                .isAfter(to)))
            .toList(),
        communityId,
        "listAccountEntries",
        map("unitId", unitId, "from", from, "to", to),
        cursor,
        limit,
        item -> String.format(java.util.Locale.ROOT, "%020d:%s", item.version(), item.id()),
        views::entry);
  }

  @GetMapping("/communities/{communityId}/charges")
  public Object listCharges(
      @PathVariable String communityId,
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
      @RequestParam(required = false) String unitId,
      @RequestParam(required = false) @Pattern(regexp = "OUTSTANDING|PARTIALLY_PAID|PAID|CANCELLED")
          String status) {
    return pages.page(
        queries.charges(current.id(), communityId, unitId, status),
        communityId,
        "listCharges",
        map("unitId", unitId, "status", status),
        cursor,
        limit,
        views::charge);
  }

  @PostMapping("/communities/{communityId}/charge-batches")
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  public Object createChargeBatch(
      @PathVariable String communityId,
      @RequestHeader("Idempotency-Key") String key,
      @Valid @RequestBody ChargeBatchRequest body) {
    return views.batch(
        finance.createCharges(
            current.actor(),
            communityId,
            key,
            new FinanceCommands.ChargeBatch(
                body.items().stream()
                    .map(
                        item ->
                            new FinanceCommands.ChargeItem(
                                item.unitId(),
                                item.description(),
                                item.periodStart(),
                                item.periodEnd(),
                                item.dueOn(),
                                item.amountMinor(),
                                item.expectedAccountVersion()))
                    .toList())));
  }

  @GetMapping("/communities/{communityId}/charge-batches/{batchId}")
  public Object getChargeBatch(@PathVariable String communityId, @PathVariable String batchId) {
    return views.batch(queries.batch(current.id(), communityId, batchId));
  }

  @PostMapping("/communities/{communityId}/charges/{chargeId}/cancellations")
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  public Object cancelCharge(
      @PathVariable String communityId,
      @PathVariable String chargeId,
      @RequestHeader("Idempotency-Key") String key,
      @Valid @RequestBody ReasonCommand body) {
    return views.charge(
        finance.cancelCharge(
            current.actor(),
            communityId,
            chargeId,
            key,
            new FinanceCommands.Reason(body.reason(), body.expectedVersion())));
  }

  @GetMapping("/communities/{communityId}/payments")
  public Object listPayments(
      @PathVariable String communityId,
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
      @RequestParam(required = false) String unitId) {
    return pages.page(
        queries.payments(current.id(), communityId, unitId),
        communityId,
        "listPayments",
        map("unitId", unitId),
        cursor,
        limit,
        views::payment);
  }

  @PostMapping("/communities/{communityId}/payments")
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  public Object recordPayment(
      @PathVariable String communityId,
      @RequestHeader("Idempotency-Key") String key,
      @Valid @RequestBody PaymentCreate body) {
    return views.payment(
        finance.recordPayment(
            current.actor(),
            communityId,
            key,
            new FinanceCommands.RecordPayment(
                body.unitId(),
                body.accountId(),
                body.amountMinor(),
                body.paidOn(),
                body.reference(),
                body.allocations().stream()
                    .map(
                        item ->
                            new com.govener.finance.domain.Allocation(
                                item.chargeId(), item.amountMinor()))
                    .toList(),
                body.notificationId(),
                body.expectedAccountVersion())));
  }

  @PostMapping("/communities/{communityId}/payments/{paymentId}/reversals")
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  public Object reversePayment(
      @PathVariable String communityId,
      @PathVariable String paymentId,
      @RequestHeader("Idempotency-Key") String key,
      @Valid @RequestBody ReasonCommand body) {
    return views.payment(
        finance.reversePayment(
            current.actor(),
            communityId,
            paymentId,
            key,
            new FinanceCommands.Reason(body.reason(), body.expectedVersion())));
  }

  @GetMapping("/communities/{communityId}/payment-notifications")
  public Object listPaymentNotifications(
      @PathVariable String communityId,
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
      @RequestParam(required = false) String unitId,
      @RequestParam(required = false)
          @Pattern(regexp = "PENDING|REVIEWED|REJECTED|WITHDRAWN|RECORDED")
          String status) {
    return pages.page(
        queries.notifications(current.id(), communityId, unitId, status),
        communityId,
        "listPaymentNotifications",
        map("unitId", unitId, "status", status),
        cursor,
        limit,
        views::notification);
  }

  @PostMapping("/communities/{communityId}/payment-notifications")
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  public Object createPaymentNotification(
      @PathVariable String communityId, @Valid @RequestBody PaymentNotificationCreate body) {
    return views.notification(
        finance.createNotification(
            current.actor(),
            communityId,
            new FinanceCommands.Notify(
                body.unitId(),
                body.amountMinor(),
                body.paymentDate(),
                body.reference(),
                body.note(),
                body.receiptId())));
  }

  @PostMapping("/communities/{communityId}/payment-notifications/{notificationId}/reviews")
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  public Object reviewPaymentNotification(
      @PathVariable String communityId,
      @PathVariable String notificationId,
      @Valid @RequestBody NotificationReview body) {
    return views.notification(
        finance.reviewNotification(
            current.actor(),
            communityId,
            notificationId,
            new FinanceCommands.Review(body.decision(), body.note(), body.expectedVersion())));
  }

  @PostMapping("/communities/{communityId}/payment-notifications/{notificationId}/withdrawals")
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  public Object withdrawPaymentNotification(
      @PathVariable String communityId,
      @PathVariable String notificationId,
      @Valid @RequestBody VersionCommand body) {
    return views.notification(
        finance.withdrawNotification(
            current.actor(), communityId, notificationId, body.expectedVersion()));
  }

  @PostMapping("/communities/{communityId}/payment-notifications/{notificationId}/payment-links")
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  public Object linkPaymentNotification(
      @PathVariable String communityId,
      @PathVariable String notificationId,
      @RequestHeader("Idempotency-Key") String key,
      @Valid @RequestBody PaymentLink body) {
    return views.notification(
        finance.linkNotification(
            current.actor(),
            communityId,
            notificationId,
            key,
            new FinanceCommands.Link(
                body.paymentId(), body.expectedVersion(), body.expectedPaymentVersion())));
  }

  @GetMapping("/communities/{communityId}/expense-payments")
  public Object listExpensePayments(
      @PathVariable String communityId,
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
      @RequestParam(required = false) String expenseId) {
    return pages.page(
        queries.expensePayments(current.id(), communityId, expenseId),
        communityId,
        "listExpensePayments",
        map("expenseId", expenseId),
        cursor,
        limit,
        views::expensePayment);
  }

  @GetMapping("/communities/{communityId}/expenses")
  public Object listExpenses(
      @PathVariable String communityId,
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
    return pages.page(
        queries.expenses(current.id(), communityId),
        communityId,
        "listExpenses",
        map(),
        cursor,
        limit,
        views::expense);
  }

  @PostMapping("/communities/{communityId}/expenses")
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  public Object createExpense(
      @PathVariable String communityId,
      @RequestHeader("Idempotency-Key") String key,
      @Valid @RequestBody ExpenseCreate body) {
    return views.expense(
        finance.createExpense(
            current.actor(),
            communityId,
            key,
            new FinanceCommands.RecordExpense(
                body.description(),
                body.category(),
                body.vendor(),
                body.incurredOn(),
                body.amountMinor(),
                body.documentId(),
                body.immediatePayment() == null
                    ? null
                    : new FinanceCommands.ImmediatePayment(
                        body.immediatePayment().accountId(), body.immediatePayment().paidOn()))));
  }

  @GetMapping("/communities/{communityId}/expenses/{expenseId}")
  public Object getExpense(@PathVariable String communityId, @PathVariable String expenseId) {
    return views.expense(queries.expense(current.id(), communityId, expenseId));
  }

  @PostMapping("/communities/{communityId}/expenses/{expenseId}/payments")
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  public Object payExpense(
      @PathVariable String communityId,
      @PathVariable String expenseId,
      @RequestHeader("Idempotency-Key") String key,
      @Valid @RequestBody ExpensePaymentCreate body) {
    return views.expensePayment(
        finance.payExpense(
            current.actor(),
            communityId,
            expenseId,
            key,
            new FinanceCommands.PayExpense(
                body.accountId(), body.paidOn(), body.amountMinor(), body.expectedVersion())));
  }

  @PostMapping("/communities/{communityId}/expense-payments/{expensePaymentId}/reversals")
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  public Object reverseExpensePayment(
      @PathVariable String communityId,
      @PathVariable String expensePaymentId,
      @RequestHeader("Idempotency-Key") String key,
      @Valid @RequestBody ReasonCommand body) {
    return views.expensePayment(
        finance.reverseExpensePayment(
            current.actor(),
            communityId,
            expensePaymentId,
            key,
            new FinanceCommands.Reason(body.reason(), body.expectedVersion())));
  }
}
