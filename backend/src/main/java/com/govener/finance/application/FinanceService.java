package com.govener.finance.application;

import com.govener.attachment.application.AttachmentService;
import com.govener.community.application.AccessService;
import com.govener.community.domain.*;
import com.govener.finance.domain.*;
import com.govener.notification.application.ActivityPublisher;
import com.govener.shared.application.*;
import com.govener.shared.domain.*;
import java.time.*;
import java.util.*;

public final class FinanceService {
  private final FinanceRepositories store;
  private final AccessService access;
  private final IdempotentExecutor idempotency;
  private final TransactionPort transactions;
  private final AttachmentService attachments;
  private final ActivityPublisher activity;
  private final SerializationPort serialization;
  private final Clock clock;
  private final ManualAllocationPolicy allocations;

  public FinanceService(
      FinanceRepositories store,
      AccessService access,
      IdempotentExecutor idempotency,
      TransactionPort transactions,
      AttachmentService attachments,
      ActivityPublisher activity,
      SerializationPort serialization,
      Clock clock,
      ManualAllocationPolicy allocations) {
    this.store = store;
    this.access = access;
    this.idempotency = idempotency;
    this.transactions = transactions;
    this.attachments = attachments;
    this.activity = activity;
    this.serialization = serialization;
    this.clock = clock;
    this.allocations = allocations;
  }

  private Membership manager(Actor actor, String communityId, Permission permission) {
    Membership member = access.membership(actor.id(), communityId);
    member.requireManager(permission);
    return member;
  }

  private <T extends ScopedAggregate> T scoped(java.util.Optional<T> value, String communityId) {
    return value
        .filter(item -> item.communityId().equals(communityId))
        .orElseThrow(RuleViolation::missing);
  }

  public UnitAccount unitAccount(String communityId, String unitId) {
    return store
        .unitAccounts()
        .forUnit(communityId, unitId)
        .orElseGet(
            () ->
                new UnitAccount(
                    serialization.hash(communityId + ":" + unitId),
                    communityId,
                    unitId,
                    0,
                    0,
                    0,
                    0,
                    0));
  }

  private void saveUnit(UnitAccount original, UnitAccount next) {
    if (store.unitAccounts().find(original.id()).isPresent())
      store.unitAccounts().save(next, original.version());
    else store.unitAccounts().insert(next);
  }

  private List<String> residents(String communityId, String unitId) {
    return access.inCommunity(communityId).stream()
        .filter(member -> member.ownsAccess(unitId, access.today()))
        .map(Membership::userId)
        .toList();
  }

  private void notFuture(LocalDate value) {
    if (value == null || value.isAfter(access.today()))
      throw RuleViolation.invalid("A transfer or expense date cannot be in the future.");
  }

  private void entry(
      UnitAccount account, String type, long amount, String description, String source) {
    store
        .entries()
        .insert(
            new AccountEntry(
                UUID.randomUUID().toString(),
                account.communityId(),
                account.unitId(),
                type,
                amount,
                clock.instant(),
                description,
                source,
                account.netMinor(),
                account.version()));
  }

  public ChargeBatch createCharges(
      Actor actor, String communityId, String key, FinanceCommands.ChargeBatch command) {
    Membership member = manager(actor, communityId, Permission.CHARGE_CREATE);
    return idempotency.execute(
        actor.id(),
        communityId,
        "createChargeBatch",
        key,
        command,
        ChargeBatch.class,
        () -> {
          if (command.items().isEmpty()
              || command.items().size() > 1000
              || command.items().stream().map(FinanceCommands.ChargeItem::unitId).distinct().count()
                  != command.items().size())
            throw RuleViolation.invalid("Select one or more distinct units.");
          List<Charge> created = new ArrayList<>();
          for (var item : command.items()) {
            access.unit(member, item.unitId());
            UnitAccount account = unitAccount(communityId, item.unitId());
            Checks.version(account.version(), item.expectedAccountVersion());
            Charge charge =
                new Charge(
                    UUID.randomUUID().toString(),
                    communityId,
                    item.unitId(),
                    item.description(),
                    new BillingPeriod(item.periodStart(), item.periodEnd()),
                    access.today(),
                    item.dueOn(),
                    Money.positive(item.amountMinor()),
                    0,
                    null,
                    "",
                    0);
            UnitAccount next = account.charge(item.amountMinor());
            store.charges().insert(charge);
            saveUnit(account, next);
            entry(next, "CHARGE", charge.amount().minor(), charge.description(), charge.id());
            created.add(charge);
            activity.emit(
                actor,
                communityId,
                "CHARGE_CREATED",
                charge.id(),
                "",
                residents(communityId, item.unitId()),
                "CHARGE",
                "A charge was issued for your unit.",
                "ACCOUNT",
                item.unitId());
          }
          ChargeBatch batch =
              new ChargeBatch(
                  UUID.randomUUID().toString(), communityId, created, clock.instant(), 0);
          store.batches().insert(batch);
          return batch;
        });
  }

  public Charge cancelCharge(
      Actor actor, String communityId, String id, String key, FinanceCommands.Reason command) {
    manager(actor, communityId, Permission.CHARGE_CREATE);
    return idempotency.execute(
        actor.id(),
        communityId,
        "cancelCharge:" + id,
        key,
        command,
        Charge.class,
        () -> {
          Charge original = scoped(store.charges().find(id), communityId);
          Checks.version(original.version(), command.expectedVersion());
          Charge next = original.cancel(command.reason(), clock.instant());
          UnitAccount account = unitAccount(communityId, original.unitId());
          UnitAccount changed = account.cancel(original.amount().minor());
          store.charges().save(next, original.version());
          saveUnit(account, changed);
          entry(changed, "CHARGE_CANCELLATION", -original.amount().minor(), command.reason(), id);
          activity.emit(
              actor,
              communityId,
              "CHARGE_CANCELLED",
              id,
              command.reason(),
              residents(communityId, original.unitId()),
              "CHARGE",
              "A unit charge was corrected.",
              "ACCOUNT",
              original.unitId());
          return next;
        });
  }

  public Payment recordPayment(
      Actor actor, String communityId, String key, FinanceCommands.RecordPayment command) {
    Membership member = manager(actor, communityId, Permission.PAYMENT_RECORD);
    return idempotency.execute(
        actor.id(),
        communityId,
        "recordPayment",
        key,
        command,
        Payment.class,
        () -> {
          access.unit(member, command.unitId());
          notFuture(command.paidOn());
          UnitAccount account = unitAccount(communityId, command.unitId());
          Checks.version(account.version(), command.expectedAccountVersion());
          FinancialAccount cash = scoped(store.accounts().find(command.accountId()), communityId);
          Payment payment =
              new Payment(
                  UUID.randomUUID().toString(),
                  communityId,
                  command.unitId(),
                  command.accountId(),
                  Money.positive(command.amountMinor()),
                  command.paidOn(),
                  command.reference(),
                  command.allocations(),
                  command.notificationId(),
                  null,
                  "",
                  0);
          List<Charge> changed =
              allocations.allocate(payment, id -> scoped(store.charges().find(id), communityId));
          for (Charge charge : changed) store.charges().save(charge, charge.version() - 1);
          if (command.notificationId() != null) {
            PaymentNotification notification =
                scoped(store.notifications().find(command.notificationId()), communityId);
            validateMatch(notification, payment);
            store.notifications().save(notification.record(payment.id()), notification.version());
          }
          UnitAccount next = account.receive(payment.amount().minor(), payment.allocatedMinor());
          saveUnit(account, next);
          store.accounts().save(cash.move(payment.amount().minor()), cash.version());
          store.payments().insert(payment);
          entry(next, "PAYMENT", -payment.amount().minor(), payment.reference(), payment.id());
          activity.emit(
              actor,
              communityId,
              "PAYMENT_RECORDED",
              payment.id(),
              "",
              residents(communityId, payment.unitId()),
              "PAYMENT",
              "A payment was recorded for your unit.",
              "ACCOUNT",
              payment.unitId());
          return payment;
        });
  }

  private void validateMatch(PaymentNotification notification, Payment payment) {
    if (!notification.unitId().equals(payment.unitId())
        || notification.amount().minor() != payment.amount().minor())
      throw RuleViolation.invalid("Notification and payment must have the same unit and amount.");
  }

  public Payment reversePayment(
      Actor actor, String communityId, String id, String key, FinanceCommands.Reason command) {
    manager(actor, communityId, Permission.PAYMENT_RECORD);
    return idempotency.execute(
        actor.id(),
        communityId,
        "reversePayment:" + id,
        key,
        command,
        Payment.class,
        () -> {
          Payment original = scoped(store.payments().find(id), communityId);
          Checks.version(original.version(), command.expectedVersion());
          Payment next = original.reverse(command.reason(), clock.instant());
          for (Allocation allocation : original.allocations()) {
            Charge charge = scoped(store.charges().find(allocation.chargeId()), communityId);
            store.charges().save(charge.release(allocation.amountMinor()), charge.version());
          }
          UnitAccount account = unitAccount(communityId, original.unitId());
          UnitAccount changed =
              account.reverse(original.amount().minor(), original.allocatedMinor());
          saveUnit(account, changed);
          FinancialAccount cash = scoped(store.accounts().find(original.accountId()), communityId);
          store.accounts().save(cash.move(-original.amount().minor()), cash.version());
          store.payments().save(next, original.version());
          if (original.notificationId() != null) {
            PaymentNotification notification =
                scoped(store.notifications().find(original.notificationId()), communityId);
            if (!original.id().equals(notification.paymentId()))
              throw RuleViolation.conflict("Notification link changed.");
            store.notifications().save(notification.release(), notification.version());
          }
          entry(
              changed,
              "PAYMENT_REVERSAL",
              original.amount().minor(),
              command.reason(),
              original.id());
          activity.emit(
              actor,
              communityId,
              "PAYMENT_REVERSED",
              id,
              command.reason(),
              residents(communityId, original.unitId()),
              "PAYMENT",
              "A payment entry was corrected.",
              "ACCOUNT",
              original.unitId());
          return next;
        });
  }

  public PaymentNotification createNotification(
      Actor actor, String communityId, FinanceCommands.Notify command) {
    Membership member = access.membership(actor.id(), communityId);
    access.ownUnit(member, command.unitId());
    notFuture(command.paymentDate());
    return transactions.execute(
        () -> {
          String id = UUID.randomUUID().toString();
          attachments.bind(
              actor.id(),
              communityId,
              command.receiptId(),
              "PAYMENT_RECEIPT",
              "PAYMENT_NOTIFICATION",
              id);
          PaymentNotification notification =
              new PaymentNotification(
                  id,
                  communityId,
                  actor.id(),
                  command.unitId(),
                  Money.positive(command.amountMinor()),
                  command.paymentDate(),
                  command.reference(),
                  command.note(),
                  command.receiptId(),
                  "PENDING",
                  "",
                  null,
                  0);
          store.notifications().insert(notification);
          activity.emit(
              actor,
              communityId,
              "PAYMENT_NOTIFICATION_CREATED",
              id,
              "",
              access.inCommunity(communityId).stream()
                  .filter(
                      item ->
                          item.manager() && item.permissions().contains(Permission.PAYMENT_RECORD))
                  .map(Membership::userId)
                  .toList(),
              "PAYMENT",
              "A bank transfer notification is awaiting review.",
              "PAYMENT_NOTIFICATION");
          return notification;
        });
  }

  public PaymentNotification reviewNotification(
      Actor actor, String communityId, String id, FinanceCommands.Review command) {
    manager(actor, communityId, Permission.PAYMENT_RECORD);
    return transactions.execute(
        () -> {
          PaymentNotification original = scoped(store.notifications().find(id), communityId);
          Checks.version(original.version(), command.expectedVersion());
          PaymentNotification next = original.review(command.decision(), command.note());
          store.notifications().save(next, original.version());
          activity.emit(
              actor,
              communityId,
              "PAYMENT_NOTIFICATION_REVIEWED",
              id,
              command.note(),
              List.of(original.userId()),
              "PAYMENT",
              "Your transfer notification was reviewed.",
              "PAYMENT_NOTIFICATION");
          return next;
        });
  }

  public PaymentNotification withdrawNotification(
      Actor actor, String communityId, String id, long expected) {
    Membership member = access.membership(actor.id(), communityId);
    return transactions.execute(
        () -> {
          PaymentNotification original = scoped(store.notifications().find(id), communityId);
          if (!original.userId().equals(actor.id())
              || !member.ownsAccess(original.unitId(), access.today()))
            throw RuleViolation.missing();
          Checks.version(original.version(), expected);
          PaymentNotification next = original.withdraw();
          store.notifications().save(next, original.version());
          activity.emit(
              actor,
              communityId,
              "PAYMENT_NOTIFICATION_WITHDRAWN",
              id,
              "",
              List.of(),
              "PAYMENT",
              "",
              "PAYMENT_NOTIFICATION");
          return next;
        });
  }

  public PaymentNotification linkNotification(
      Actor actor, String communityId, String id, String key, FinanceCommands.Link command) {
    manager(actor, communityId, Permission.PAYMENT_RECORD);
    return idempotency.execute(
        actor.id(),
        communityId,
        "linkPaymentNotification:" + id,
        key,
        command,
        PaymentNotification.class,
        () -> {
          PaymentNotification notification = scoped(store.notifications().find(id), communityId);
          Checks.version(notification.version(), command.expectedVersion());
          Payment payment = scoped(store.payments().find(command.paymentId()), communityId);
          Checks.version(payment.version(), command.expectedPaymentVersion());
          validateMatch(notification, payment);
          Payment linked = payment.link(id);
          PaymentNotification next = notification.record(payment.id());
          store.payments().save(linked, payment.version());
          store.notifications().save(next, notification.version());
          activity.emit(
              actor,
              communityId,
              "PAYMENT_NOTIFICATION_LINKED",
              id,
              "",
              List.of(notification.userId()),
              "PAYMENT",
              "Your transfer notification was matched to an existing payment.",
              "PAYMENT_NOTIFICATION");
          return next;
        });
  }

  public Expense createExpense(
      Actor actor, String communityId, String key, FinanceCommands.RecordExpense command) {
    manager(actor, communityId, Permission.EXPENSE_WRITE);
    return idempotency.execute(
        actor.id(),
        communityId,
        "createExpense",
        key,
        command,
        Expense.class,
        () -> {
          notFuture(command.incurredOn());
          String id = UUID.randomUUID().toString();
          attachments.bind(
              actor.id(), communityId, command.documentId(), "EXPENSE_DOCUMENT", "EXPENSE", id);
          Expense expense =
              new Expense(
                  id,
                  communityId,
                  command.description(),
                  command.category(),
                  command.vendor(),
                  command.incurredOn(),
                  Money.positive(command.amountMinor()),
                  0,
                  command.documentId(),
                  0);
          store.expenses().insert(expense);
          if (command.immediatePayment() != null) {
            var immediate = command.immediatePayment();
            payExpenseInternal(
                communityId,
                expense,
                immediate.accountId(),
                immediate.paidOn(),
                expense.amount().minor());
            expense = store.expenses().find(id).orElseThrow();
          }
          activity.emit(
              actor, communityId, "EXPENSE_CREATED", id, "", List.of(), "SYSTEM", "", "OVERVIEW");
          return expense;
        });
  }

  public ExpensePayment payExpense(
      Actor actor, String communityId, String id, String key, FinanceCommands.PayExpense command) {
    manager(actor, communityId, Permission.EXPENSE_WRITE);
    return idempotency.execute(
        actor.id(),
        communityId,
        "payExpense:" + id,
        key,
        command,
        ExpensePayment.class,
        () -> {
          Expense expense = scoped(store.expenses().find(id), communityId);
          Checks.version(expense.version(), command.expectedVersion());
          ExpensePayment result =
              payExpenseInternal(
                  communityId,
                  expense,
                  command.accountId(),
                  command.paidOn(),
                  command.amountMinor());
          activity.emit(
              actor,
              communityId,
              "EXPENSE_PAID",
              result.id(),
              "",
              List.of(),
              "SYSTEM",
              "",
              "OVERVIEW");
          return result;
        });
  }

  private ExpensePayment payExpenseInternal(
      String communityId, Expense expense, String accountId, LocalDate paidOn, long amount) {
    notFuture(paidOn);
    FinancialAccount cash = scoped(store.accounts().find(accountId), communityId);
    ExpensePayment payment =
        new ExpensePayment(
            UUID.randomUUID().toString(),
            communityId,
            expense.id(),
            accountId,
            paidOn,
            Money.positive(amount),
            null,
            "",
            0);
    store.expenses().save(expense.pay(amount), expense.version());
    store.accounts().save(cash.move(-amount), cash.version());
    store.expensePayments().insert(payment);
    return payment;
  }

  public ExpensePayment reverseExpensePayment(
      Actor actor, String communityId, String id, String key, FinanceCommands.Reason command) {
    manager(actor, communityId, Permission.EXPENSE_WRITE);
    return idempotency.execute(
        actor.id(),
        communityId,
        "reverseExpensePayment:" + id,
        key,
        command,
        ExpensePayment.class,
        () -> {
          ExpensePayment original = scoped(store.expensePayments().find(id), communityId);
          Checks.version(original.version(), command.expectedVersion());
          ExpensePayment next = original.reverse(command.reason(), clock.instant());
          Expense expense = scoped(store.expenses().find(original.expenseId()), communityId);
          store.expenses().save(expense.reverse(original.amount().minor()), expense.version());
          FinancialAccount cash = scoped(store.accounts().find(original.accountId()), communityId);
          store.accounts().save(cash.move(original.amount().minor()), cash.version());
          store.expensePayments().save(next, original.version());
          activity.emit(
              actor,
              communityId,
              "EXPENSE_PAYMENT_REVERSED",
              id,
              command.reason(),
              List.of(),
              "SYSTEM",
              "",
              "OVERVIEW");
          return next;
        });
  }
}
