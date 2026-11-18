package com.govener.poll.domain;

import com.govener.shared.domain.*;
import java.time.Instant;
import java.util.*;

/** One aggregate serializes voting and closure, including competing votes for the same unit. */
public record Poll(
    String id,
    String communityId,
    String title,
    String description,
    String audience,
    List<Option> options,
    Instant closesAt,
    String createdBy,
    Instant createdAt,
    String status,
    List<Ballot> ballots,
    long version)
    implements ScopedAggregate {
  public record Option(String id, String label) {
    public Option {
      id = Checks.id(id);
      label = Checks.text(label, 200);
    }
  }

  public record Ballot(
      String unitId,
      String userId,
      String voterName,
      String unitLabel,
      String optionId,
      Instant votedAt) {}

  public Poll {
    Checks.id(id);
    Checks.id(communityId);
    title = Checks.text(title, 200);
    description = Checks.optional(description, 3000);
    Checks.id(createdBy);
    Checks.required(createdAt);
    Checks.required(closesAt);
    if (!Set.of("OWNERS", "RESIDENTS").contains(Checks.required(audience))
        || !Set.of("DRAFT", "OPEN", "CLOSED").contains(Checks.required(status)))
      throw RuleViolation.invalid("Invalid poll audience or status.");
    options = List.copyOf(Checks.required(options));
    ballots = List.copyOf(Checks.required(ballots));
    if (options.size() < 2
        || options.size() > 10
        || options.stream().map(Option::id).distinct().count() != options.size()
        || options.stream()
                .map(option -> option.label().toLowerCase(Locale.ROOT))
                .distinct()
                .count()
            != options.size()) throw RuleViolation.invalid("Provide two to ten distinct options.");
    if (ballots.stream().map(Ballot::unitId).distinct().count() != ballots.size())
      throw RuleViolation.invalid("A unit can vote only once.");
  }

  public boolean open(Instant now) {
    return status.equals("OPEN") && now.isBefore(closesAt);
  }

  public Poll publish(Instant now, long expected) {
    Checks.version(version, expected);
    if (!status.equals("DRAFT") || !now.isBefore(closesAt))
      throw RuleViolation.conflict("Only an unexpired draft can be published.");
    return copy("OPEN", ballots);
  }

  public Poll close(long expected) {
    Checks.version(version, expected);
    if (!status.equals("OPEN"))
      throw RuleViolation.conflict("Only a published poll can be closed.");
    return copy("CLOSED", ballots);
  }

  public Poll vote(List<Ballot> incoming, Instant now, long expected) {
    Checks.version(version, expected);
    if (!open(now)) throw RuleViolation.conflict("This poll is not open for voting.");
    if (incoming.isEmpty()
        || incoming.size() > 1000
        || incoming.stream().map(Ballot::unitId).distinct().count() != incoming.size())
      throw RuleViolation.invalid("Select distinct units.");
    for (Ballot vote : incoming) {
      if (options.stream().noneMatch(option -> option.id().equals(vote.optionId())))
        throw RuleViolation.invalid("Unknown poll option.");
      if (ballots.stream().anyMatch(existing -> existing.unitId().equals(vote.unitId())))
        throw new RuleViolation(
            RuleViolation.Kind.CONFLICT,
            "UNIT_ALREADY_VOTED",
            "The first vote for this unit is already recorded.");
    }
    var next = new ArrayList<>(ballots);
    next.addAll(incoming);
    return copy(status, next);
  }

  private Poll copy(String nextStatus, List<Ballot> votes) {
    return new Poll(
        id,
        communityId,
        title,
        description,
        audience,
        options,
        closesAt,
        createdBy,
        createdAt,
        nextStatus,
        votes,
        version + 1);
  }
}
