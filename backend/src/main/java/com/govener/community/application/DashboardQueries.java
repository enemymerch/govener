package com.govener.community.application;

import com.govener.community.domain.Permission;
import com.govener.finance.application.FinanceQueries;
import com.govener.finance.domain.Charge;
import com.govener.service.application.RequestService;
import com.govener.service.domain.RequestStatus;
import com.govener.shared.domain.RuleViolation;
import java.time.*;
import java.util.Comparator;
import java.util.stream.Collectors;

/** Server-authoritative monthly summary, using the same visibility policy as account queries. */
public final class DashboardQueries {
  private final AccessService access;
  private final CommunityQueries communities;
  private final FinanceQueries finance;
  private final RequestService requests;

  public DashboardQueries(
      AccessService access,
      CommunityQueries communities,
      FinanceQueries finance,
      RequestService requests) {
    this.access = access;
    this.communities = communities;
    this.finance = finance;
    this.requests = requests;
  }

  public record Snapshot(
      String communityId,
      LocalDate period,
      LocalDate nextDueOn,
      long collectedMinor,
      long outstandingMinor,
      long targetMinor,
      long paidUnitCount,
      long unitCount,
      long openRequestCount,
      FinanceQueries.AccountProjection ownAccount) {}

  public Snapshot get(String userId, String communityId, String unitId, String period) {
    var member = access.membership(userId, communityId);
    member.require(Permission.COMMUNITY_READ);
    YearMonth month;
    try {
      month = period == null ? YearMonth.from(access.today()) : YearMonth.parse(period);
    } catch (DateTimeException failure) {
      throw RuleViolation.invalid("Invalid dashboard period.");
    }
    var charges =
        finance.charges(userId, communityId, unitId, null).stream()
            .filter(
                charge ->
                    charge.cancelledAt() == null
                        && !charge.period().start().isAfter(month.atEndOfMonth())
                        && !charge.period().end().isBefore(month.atDay(1)))
            .toList();
    FinanceQueries.AccountProjection ownAccount = null;
    if (!member.manager()) {
      String selected =
          unitId != null
              ? unitId
              : communities.ownUnits(userId, communityId).stream()
                  .map(unit -> unit.id())
                  .findFirst()
                  .orElse(null);
      if (selected != null) ownAccount = finance.account(userId, communityId, selected);
    }
    return new Snapshot(
        communityId,
        month.atDay(1),
        charges.stream()
            .filter(charge -> charge.outstandingMinor() > 0)
            .map(Charge::dueOn)
            .min(Comparator.naturalOrder())
            .orElse(null),
        charges.stream().mapToLong(Charge::allocatedMinor).sum(),
        charges.stream().mapToLong(Charge::outstandingMinor).sum(),
        charges.stream().mapToLong(charge -> charge.amount().minor()).sum(),
        charges.stream().collect(Collectors.groupingBy(Charge::unitId)).values().stream()
            .filter(items -> items.stream().allMatch(charge -> charge.outstandingMinor() == 0))
            .count(),
        unitId != null
            ? 1
            : member.manager()
                ? communities.units(userId, communityId, null, null).size()
                : communities.ownUnits(userId, communityId).size(),
        requests.list(userId, communityId, unitId, null, null).stream()
            .filter(request -> request.status() != RequestStatus.RESOLVED)
            .count(),
        ownAccount);
  }
}
