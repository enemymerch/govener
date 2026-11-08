package com.govener.community.application;

import com.govener.community.domain.*;
import com.govener.identity.application.IdentityPorts;
import com.govener.shared.domain.*;
import java.util.*;

public final class CommunityQueries {
  private final CommunityPorts.Communities communities;
  private final CommunityPorts.Units units;
  private final CommunityPorts.People people;
  private final CommunityPorts.Relationships relationships;
  private final CommunityPorts.Residences residences;
  private final IdentityPorts.Users users;
  private final AccessService access;

  public CommunityQueries(
      CommunityPorts.Communities communities,
      CommunityPorts.Units units,
      CommunityPorts.People people,
      CommunityPorts.Relationships relationships,
      CommunityPorts.Residences residences,
      IdentityPorts.Users users,
      AccessService access) {
    this.communities = communities;
    this.units = units;
    this.people = people;
    this.relationships = relationships;
    this.residences = residences;
    this.users = users;
    this.access = access;
  }

  public List<Community> communities(String userId) {
    return access.memberships(userId).stream()
        .filter(
            member ->
                member.manager()
                    || member.units().stream().anyMatch(unit -> unit.active(access.today())))
        .map(member -> communities.find(member.communityId()).orElseThrow(RuleViolation::missing))
        .toList();
  }

  public Community community(String userId, String communityId) {
    access.membership(userId, communityId);
    return communities.find(communityId).orElseThrow(RuleViolation::missing);
  }

  public List<Unit> units(String userId, String communityId, String query, String blockId) {
    Membership member = access.membership(userId, communityId);
    member.require(Permission.UNIT_READ);
    String q = query == null ? "" : query.toLowerCase(Locale.ROOT);
    return units.inCommunity(communityId).stream()
        .filter(
            unit ->
                (blockId == null || unit.blockId().equals(blockId))
                    && (unit.label().toLowerCase(Locale.ROOT).contains(q)))
        .toList();
  }

  public Unit unit(String userId, String communityId, String id) {
    Membership member = access.membership(userId, communityId);
    member.require(Permission.UNIT_READ);
    return access.unit(member, id);
  }

  public List<Unit> ownUnits(String userId, String communityId) {
    Membership member = access.membership(userId, communityId);
    return units.inCommunity(communityId).stream()
        .filter(unit -> member.ownsAccess(unit.id(), access.today()))
        .toList();
  }

  public List<Relationship> relationships(String userId, String communityId, String unitId) {
    Membership member = access.membership(userId, communityId);
    member.requireManager(Permission.FINANCE_READ);
    access.unit(member, unitId);
    return relationships.inCommunity(communityId).stream()
        .filter(item -> item.unitId().equals(unitId))
        .toList();
  }

  public Person person(String communityId, String id) {
    return people
        .find(id)
        .filter(person -> person.communityId().equals(communityId))
        .orElseThrow(RuleViolation::missing);
  }

  public record DirectoryUnit(Unit unit, boolean isMyUnit, List<Person> residents) {}

  public List<DirectoryUnit> directory(
      String userId, String communityId, String query, String blockId, boolean mine) {
    Membership member = access.membership(userId, communityId);
    member.require(Permission.DIRECTORY_READ);
    var current =
        residences.inCommunity(communityId).stream()
            .filter(item -> item.active(access.today()))
            .toList();
    String q = query == null ? "" : query.toLowerCase(Locale.ROOT);
    return units.inCommunity(communityId).stream()
        .filter(
            unit ->
                (blockId == null || unit.blockId().equals(blockId))
                    && (!mine || member.ownsAccess(unit.id(), access.today())))
        .map(
            unit ->
                new DirectoryUnit(
                    unit,
                    member.ownsAccess(unit.id(), access.today()),
                    current.stream()
                        .filter(item -> item.unitId().equals(unit.id()))
                        .map(item -> person(communityId, item.personId()))
                        .distinct()
                        .toList()))
        .filter(
            item ->
                (item.unit().label()
                        + " "
                        + item.residents().stream()
                            .map(Person::name)
                            .reduce("", (left, right) -> left + " " + right))
                    .toLowerCase(Locale.ROOT)
                    .contains(q))
        .toList();
  }

  public DirectoryUnit directoryUnit(String userId, String communityId, String id) {
    return directory(userId, communityId, null, null, false).stream()
        .filter(item -> item.unit().id().equals(id))
        .findFirst()
        .orElseThrow(RuleViolation::missing);
  }

  public List<Actor> staff(String userId, String communityId) {
    Membership member = access.membership(userId, communityId);
    member.requireManager(Permission.REQUEST_MANAGE);
    return access.inCommunity(communityId).stream()
        .filter(item -> item.manager() && item.permissions().contains(Permission.REQUEST_MANAGE))
        .map(
            item ->
                users
                    .find(item.userId())
                    .map(user -> new Actor(user.id(), user.name()))
                    .orElseThrow(RuleViolation::missing))
        .toList();
  }
}
