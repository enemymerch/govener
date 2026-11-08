package com.govener.finance.application;

import com.govener.community.application.AccessService;
import com.govener.community.application.CommunityPorts;
import com.govener.community.domain.*;
import com.govener.finance.domain.*;
import com.govener.shared.domain.*;
import java.util.*;

public final class FinanceQueries {
  private final FinanceRepositories store;
  private final AccessService access;
  private final FinanceService finance;
  private final FinancialVisibility visibility;
  private final CommunityPorts.Units units;

  public FinanceQueries(
      FinanceRepositories store,
      AccessService access,
      FinanceService finance,
      FinancialVisibility visibility,
      CommunityPorts.Units units) {
    this.store = store;
    this.access = access;
    this.finance = finance;
    this.visibility = visibility;
    this.units = units;
  }

  private Membership member(String userId, String communityId) {
    Membership member = access.membership(userId, communityId);
    member.require(Permission.FINANCE_READ);
    return member;
  }

  private void unitScope(Membership member, String unitId) {
    if (unitId != null) {
      access.unit(member, unitId);
      if (!member.manager()) access.ownUnit(member, unitId);
    }
  }

  public List<Charge> charges(String userId, String communityId, String unitId, String status) {
    Membership member = member(userId, communityId);
    unitScope(member, unitId);
    return store.charges().inCommunity(communityId).stream()
        .filter(
            charge ->
                (unitId == null || charge.unitId().equals(unitId))
                    && (member.manager() || member.ownsAccess(charge.unitId(), access.today()))
                    && visibility.chargeVisible(member, charge, access.today())
                    && (status == null || charge.status().equals(status)))
        .toList();
  }

  public List<Payment> payments(String userId, String communityId, String unitId) {
    Membership member = member(userId, communityId);
    unitScope(member, unitId);
    Set<String> visibleCharges =
        new HashSet<>(charges(userId, communityId, unitId, null).stream().map(Charge::id).toList());
    return store.payments().inCommunity(communityId).stream()
        .filter(
            payment ->
                (unitId == null || payment.unitId().equals(unitId))
                    && (member.manager()
                        || member.units().stream()
                                .anyMatch(
                                    unit ->
                                        unit.unitId().equals(payment.unitId())
                                            && unit.active(access.today())
                                            && unit.covers(payment.paidOn(), payment.paidOn()))
                            && payment.allocations().stream()
                                .allMatch(item -> visibleCharges.contains(item.chargeId()))))
        .toList();
  }

  public record AccountProjection(
      String unitId,
      long chargedMinor,
      long paidMinor,
      long outstandingMinor,
      long creditMinor,
      long version) {}

  public AccountProjection account(String userId, String communityId, String unitId) {
    Membership member = member(userId, communityId);
    unitScope(member, unitId);
    UnitAccount account = finance.unitAccount(communityId, unitId);
    if (member.manager())
      return new AccountProjection(
          unitId,
          account.chargedMinor(),
          account.paidMinor(),
          account.outstandingMinor(),
          account.creditMinor(),
          account.version());
    var visibleCharges = charges(userId, communityId, unitId, null);
    var visiblePayments = payments(userId, communityId, unitId);
    return new AccountProjection(
        unitId,
        visibleCharges.stream()
            .filter(item -> item.cancelledAt() == null)
            .mapToLong(item -> item.amount().minor())
            .sum(),
        visiblePayments.stream()
            .filter(item -> item.reversedAt() == null)
            .mapToLong(item -> item.amount().minor())
            .sum(),
        visibleCharges.stream().mapToLong(Charge::outstandingMinor).sum(),
        visiblePayments.stream()
            .filter(item -> item.reversedAt() == null)
            .mapToLong(Payment::unallocatedMinor)
            .sum(),
        account.version());
  }

  public List<AccountProjection> accounts(String userId, String communityId) {
    Membership member = member(userId, communityId);
    return units.inCommunity(communityId).stream()
        .filter(unit -> member.manager() || member.ownsAccess(unit.id(), access.today()))
        .map(unit -> account(userId, communityId, unit.id()))
        .toList();
  }

  public List<AccountEntry> entries(String userId, String communityId, String unitId) {
    Membership member = member(userId, communityId);
    unitScope(member, unitId);
    List<AccountEntry> entries =
        store.entries().inCommunity(communityId).stream()
            .filter(item -> item.unitId().equals(unitId))
            .toList();
    if (member.manager()) return entries;
    Set<String> sources =
        new HashSet<>(charges(userId, communityId, unitId, null).stream().map(Charge::id).toList());
    sources.addAll(payments(userId, communityId, unitId).stream().map(Payment::id).toList());
    long balance = 0;
    List<AccountEntry> projected = new ArrayList<>();
    for (AccountEntry entry :
        entries.stream()
            .filter(item -> sources.contains(item.sourceId()))
            .sorted(Comparator.comparingLong(AccountEntry::version).thenComparing(AccountEntry::id))
            .toList()) {
      balance = new Money(balance).plus(new Money(entry.amountMinor())).minor();
      projected.add(
          new AccountEntry(
              entry.id(),
              entry.communityId(),
              entry.unitId(),
              entry.type(),
              entry.amountMinor(),
              entry.occurredAt(),
              entry.description(),
              entry.sourceId(),
              balance,
              entry.version()));
    }
    return projected;
  }

  public List<FinancialAccount> financialAccounts(String userId, String communityId) {
    member(userId, communityId).requireManager(Permission.FINANCE_READ);
    return store.accounts().inCommunity(communityId);
  }

  public List<Expense> expenses(String userId, String communityId) {
    member(userId, communityId).requireManager(Permission.FINANCE_READ);
    return store.expenses().inCommunity(communityId);
  }

  public Expense expense(String userId, String communityId, String id) {
    return expenses(userId, communityId).stream()
        .filter(item -> item.id().equals(id))
        .findFirst()
        .orElseThrow(RuleViolation::missing);
  }

  public List<ExpensePayment> expensePayments(String userId, String communityId, String expenseId) {
    member(userId, communityId).requireManager(Permission.FINANCE_READ);
    if (expenseId != null) expense(userId, communityId, expenseId);
    return store.expensePayments().inCommunity(communityId).stream()
        .filter(item -> expenseId == null || item.expenseId().equals(expenseId))
        .toList();
  }

  public ChargeBatch batch(String userId, String communityId, String id) {
    member(userId, communityId).requireManager(Permission.FINANCE_READ);
    return store
        .batches()
        .find(id)
        .filter(item -> item.communityId().equals(communityId))
        .orElseThrow(RuleViolation::missing);
  }

  public List<PaymentNotification> notifications(
      String userId, String communityId, String unitId, String status) {
    Membership member = access.membership(userId, communityId);
    if (member.manager()) member.require(Permission.PAYMENT_RECORD);
    unitScope(member, unitId);
    return store.notifications().inCommunity(communityId).stream()
        .filter(
            item ->
                (member.manager()
                        || item.userId().equals(userId)
                            && member.ownsAccess(item.unitId(), access.today()))
                    && (unitId == null || item.unitId().equals(unitId))
                    && (status == null || item.status().equals(status)))
        .toList();
  }
}
