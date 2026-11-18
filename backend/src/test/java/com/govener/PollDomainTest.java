package com.govener;

import static org.assertj.core.api.Assertions.*;

import com.govener.poll.domain.Poll;
import com.govener.shared.domain.RuleViolation;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

class PollDomainTest {
  private final Instant now = Instant.parse("2026-11-08T09:00:00Z");

  private Poll poll(String status) {
    return new Poll(
        "poll",
        "community",
        "Garden decision",
        "Description",
        "RESIDENTS",
        List.of(new Poll.Option("yes", "Yes"), new Poll.Option("no", "No")),
        now.plusSeconds(3600),
        "manager",
        now,
        status,
        List.of(),
        0);
  }

  private Poll.Ballot ballot(String unit, String option) {
    return new Poll.Ballot(unit, "user", "Voter", unit, option, now);
  }

  @Test
  void multipleUnitsHaveSeparateEqualVotes() {
    var result =
        poll("OPEN")
            .vote(List.of(ballot("a1", "yes"), ballot("a2", "yes"), ballot("a3", "yes")), now, 0);
    assertThat(result.ballots()).hasSize(3);
    assertThat(result.version()).isEqualTo(1);
  }

  @Test
  void firstVoteCannotBeOverwritten() {
    var original = poll("OPEN").vote(List.of(ballot("a1", "yes")), now, 0);
    assertThatThrownBy(() -> original.vote(List.of(ballot("a1", "no")), now, 1))
        .isInstanceOf(RuleViolation.class);
    assertThat(original.ballots().getFirst().optionId()).isEqualTo("yes");
  }

  @Test
  void batchCannotPartiallyVoteWhenOneUnitAlreadyVoted() {
    var original = poll("OPEN").vote(List.of(ballot("a1", "yes")), now, 0);
    assertThatThrownBy(() -> original.vote(List.of(ballot("a2", "no"), ballot("a1", "no")), now, 1))
        .isInstanceOf(RuleViolation.class);
    assertThat(original.ballots()).hasSize(1);
  }

  @Test
  void draftsCannotReceiveVotes() {
    assertThatThrownBy(() -> poll("DRAFT").vote(List.of(ballot("a1", "yes")), now, 0))
        .isInstanceOf(RuleViolation.class);
  }

  @Test
  void closingBoundaryIsExclusive() {
    var poll = poll("OPEN");
    assertThat(poll.open(poll.closesAt())).isFalse();
    assertThatThrownBy(() -> poll.vote(List.of(ballot("a1", "yes")), poll.closesAt(), 0))
        .isInstanceOf(RuleViolation.class);
  }

  @Test
  void closedPollRejectsVotes() {
    var closed = poll("OPEN").close(0);
    assertThatThrownBy(() -> closed.vote(List.of(ballot("a1", "yes")), now, 1))
        .isInstanceOf(RuleViolation.class);
  }

  @Test
  void unknownOptionsAndDuplicateUnitsAreRejected() {
    assertThatThrownBy(() -> poll("OPEN").vote(List.of(ballot("a1", "unknown")), now, 0))
        .isInstanceOf(RuleViolation.class);
    assertThatThrownBy(
            () -> poll("OPEN").vote(List.of(ballot("a1", "yes"), ballot("a1", "yes")), now, 0))
        .isInstanceOf(RuleViolation.class);
  }

  @Test
  void staleVersionCannotChangeState() {
    assertThatThrownBy(() -> poll("OPEN").vote(List.of(ballot("a1", "yes")), now, 1))
        .isInstanceOf(RuleViolation.class);
  }

  @Test
  void duplicateLabelsAreRejectedAfterTrimming() {
    assertThatThrownBy(
            () ->
                new Poll(
                    "poll",
                    "community",
                    "Question",
                    "",
                    "OWNERS",
                    List.of(new Poll.Option("one", "Yes"), new Poll.Option("two", " yes ")),
                    now.plusSeconds(60),
                    "manager",
                    now,
                    "OPEN",
                    List.of(),
                    0))
        .isInstanceOf(RuleViolation.class);
  }

  @Test
  void expiredDraftCannotBePublished() {
    var draft = poll("DRAFT");
    assertThatThrownBy(() -> draft.publish(draft.closesAt(), 0)).isInstanceOf(RuleViolation.class);
  }
}
