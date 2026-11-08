package com.govener.community.application;

import com.govener.community.domain.*;
import com.govener.shared.domain.*;
import java.time.*;
import java.util.*;

public final class AccessService {
  private final CommunityPorts.Memberships memberships;
  private final CommunityPorts.Units units;
  private final Clock clock;

  public AccessService(
      CommunityPorts.Memberships memberships, CommunityPorts.Units units, Clock clock) {
    this.memberships = memberships;
    this.units = units;
    this.clock = clock;
  }

  public LocalDate today() {
    return LocalDate.now(clock.withZone(ZoneId.of("Europe/Istanbul")));
  }

  public Membership membership(String userId, String communityId) {
    Checks.id(communityId);
    Membership member =
        memberships.forUserInCommunity(userId, communityId).orElseThrow(RuleViolation::missing);
    if (!member.manager() && member.units().stream().noneMatch(unit -> unit.active(today())))
      throw RuleViolation.missing();
    return member;
  }

  public Unit unit(Membership member, String id) {
    return units
        .find(Checks.id(id))
        .filter(unit -> unit.communityId().equals(member.communityId()))
        .orElseThrow(RuleViolation::missing);
  }

  public void ownUnit(Membership member, String id) {
    unit(member, id);
    if (!member.ownsAccess(id, today())) throw RuleViolation.missing();
  }

  public List<Membership> memberships(String userId) {
    return memberships.forUser(userId);
  }

  public List<Membership> inCommunity(String id) {
    return memberships.inCommunity(id);
  }
}
