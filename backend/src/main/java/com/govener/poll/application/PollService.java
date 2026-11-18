package com.govener.poll.application;

import com.govener.community.application.*;
import com.govener.community.domain.*;
import com.govener.identity.application.IdentityPorts;
import com.govener.poll.domain.Poll;
import com.govener.shared.application.*;
import com.govener.shared.domain.*;
import java.time.*;
import java.util.*;

public final class PollService {
  private final PollPorts.Polls polls;
  private final AccessService access;
  private final CommunityPorts.Units units;
  private final CommunityPorts.Relationships relationships;
  private final CommunityPorts.Residences residences;
  private final IdentityPorts.Users users;
  private final TransactionPort transactions;
  private final AuditPort audit;
  private final Clock clock;

  public PollService(
      PollPorts.Polls polls,
      AccessService access,
      CommunityPorts.Units units,
      CommunityPorts.Relationships relationships,
      CommunityPorts.Residences residences,
      IdentityPorts.Users users,
      TransactionPort transactions,
      AuditPort audit,
      Clock clock) {
    this.polls = polls;
    this.access = access;
    this.units = units;
    this.relationships = relationships;
    this.residences = residences;
    this.users = users;
    this.transactions = transactions;
    this.audit = audit;
    this.clock = clock;
  }

  public record Write(
      String title,
      String description,
      String audience,
      List<String> options,
      Instant closesAt,
      String status) {}

  public record EligibleUnit(String id, String label, boolean voted) {}

  public record Result(String optionId, String label, long votes) {}

  public record View(
      String id,
      String title,
      String description,
      String audience,
      List<Poll.Option> options,
      Instant closesAt,
      Instant createdAt,
      String status,
      List<Poll.Ballot> ballots,
      List<Result> results,
      List<EligibleUnit> eligibleUnits,
      long totalVotes,
      long version) {}

  private Membership member(String user, String cid) {
    var member = access.membership(user, cid);
    member.require(Permission.COMMUNITY_READ);
    return member;
  }

  private void manager(String user, String cid) {
    var member = member(user, cid);
    if (!member.manager()) throw RuleViolation.forbidden();
    member.require(Permission.POLL_MANAGE);
  }

  private Set<String> eligible(Membership member, Poll poll) {
    var result = new HashSet<String>();
    if (poll.audience().equals("OWNERS")) {
      relationships.inCommunity(poll.communityId()).stream()
          .filter(
              item ->
                  item.personId().equals(member.personId())
                      && item.type().equals("OWNER")
                      && new UnitAccess(item.unitId(), item.startsOn(), item.endsOn())
                          .active(access.today()))
          .forEach(item -> result.add(item.unitId()));
    } else {
      residences.inCommunity(poll.communityId()).stream()
          .filter(item -> item.personId().equals(member.personId()) && item.active(access.today()))
          .forEach(item -> result.add(item.unitId()));
    }
    return result;
  }

  private Poll visible(String user, String cid, String id) {
    var member = member(user, cid);
    var poll =
        polls
            .find(Checks.id(id))
            .filter(item -> item.communityId().equals(cid))
            .orElseThrow(RuleViolation::missing);
    if (!member.manager() && (poll.status().equals("DRAFT") || eligible(member, poll).isEmpty()))
      throw RuleViolation.missing();
    return poll;
  }

  public List<Poll> list(String user, String cid) {
    var member = member(user, cid);
    return polls.inCommunity(cid).stream()
        .filter(
            poll ->
                member.manager()
                    || !poll.status().equals("DRAFT") && !eligible(member, poll).isEmpty())
        .toList();
  }

  public View get(String user, String cid, String id) {
    return view(user, visible(user, cid, id));
  }

  public View view(String user, Poll poll) {
    var member = member(user, poll.communityId());
    var eligible = eligible(member, poll);
    var granted =
        units.inCommunity(poll.communityId()).stream()
            .filter(unit -> eligible.contains(unit.id()))
            .map(
                unit ->
                    new EligibleUnit(
                        unit.id(),
                        unit.label(),
                        poll.ballots().stream()
                            .anyMatch(ballot -> ballot.unitId().equals(unit.id()))))
            .sorted(Comparator.comparing(EligibleUnit::label))
            .toList();
    return new View(
        poll.id(),
        poll.title(),
        poll.description(),
        poll.audience(),
        poll.options(),
        poll.closesAt(),
        poll.createdAt(),
        poll.status().equals("OPEN") && !poll.open(clock.instant()) ? "CLOSED" : poll.status(),
        poll.ballots(),
        poll.options().stream()
            .map(
                option ->
                    new Result(
                        option.id(),
                        option.label(),
                        poll.ballots().stream()
                            .filter(vote -> vote.optionId().equals(option.id()))
                            .count()))
            .toList(),
        granted,
        poll.ballots().size(),
        poll.version());
  }

  public View create(Actor actor, String cid, Write command) {
    manager(actor.id(), cid);
    if (command.closesAt() == null || !command.closesAt().isAfter(clock.instant()))
      throw RuleViolation.invalid("Choose a future closing time.");
    if (!Set.of("DRAFT", "OPEN").contains(Checks.required(command.status())))
      throw RuleViolation.invalid("Invalid initial poll status.");
    var poll =
        new Poll(
            UUID.randomUUID().toString(),
            cid,
            command.title(),
            command.description(),
            command.audience(),
            Checks.required(command.options()).stream()
                .map(label -> new Poll.Option(UUID.randomUUID().toString(), label))
                .toList(),
            command.closesAt(),
            actor.id(),
            clock.instant(),
            command.status(),
            List.of(),
            0);
    transactions.execute(
        () -> {
          manager(actor.id(), cid);
          polls.insert(poll);
          audit(actor, cid, "POLL_CREATED", poll.id());
          return null;
        });
    return view(actor.id(), poll);
  }

  public View publish(Actor actor, String cid, String id, long expected) {
    return transition(actor, cid, id, expected, true);
  }

  public View close(Actor actor, String cid, String id, long expected) {
    return transition(actor, cid, id, expected, false);
  }

  private View transition(Actor actor, String cid, String id, long expected, boolean publish) {
    manager(actor.id(), cid);
    var next =
        transactions.execute(
            () -> {
              manager(actor.id(), cid);
              var old = visible(actor.id(), cid, id);
              var updated = publish ? old.publish(clock.instant(), expected) : old.close(expected);
              polls.save(updated, old.version());
              audit(actor, cid, publish ? "POLL_PUBLISHED" : "POLL_CLOSED", id);
              return updated;
            });
    return view(actor.id(), next);
  }

  public View vote(
      Actor actor, String cid, String id, String optionId, List<String> unitIds, long expected) {
    member(actor.id(), cid);
    var next =
        transactions.execute(
            () -> {
              var member = member(actor.id(), cid);
              var original = visible(actor.id(), cid, id);
              var eligible = eligible(member, original);
              Checks.required(unitIds);
              if (unitIds.stream().anyMatch(unit -> !eligible.contains(unit)))
                throw RuleViolation.forbidden();
              var user = users.find(actor.id()).orElseThrow(RuleViolation::missing);
              var votes =
                  unitIds.stream()
                      .map(
                          unitId -> {
                            var unit = access.unit(member, unitId);
                            return new Poll.Ballot(
                                unitId,
                                user.id(),
                                user.name(),
                                unit.label(),
                                optionId,
                                clock.instant());
                          })
                      .toList();
              var updated = original.vote(votes, clock.instant(), expected);
              polls.save(updated, original.version());
              audit(actor, cid, "POLL_VOTED", id);
              return updated;
            });
    return view(actor.id(), next);
  }

  private void audit(Actor actor, String cid, String operation, String id) {
    audit.append(
        new AuditRecord(
            UUID.randomUUID().toString(), cid, actor.id(), operation, id, "", clock.instant(), 0));
  }
}
