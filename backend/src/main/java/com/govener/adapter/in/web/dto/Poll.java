package com.govener.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

/** Transport contract generated from docs/openapi.json; domain models are separate. */
public record Poll(
    @Valid @NotNull @NotBlank @Size(min = 1, max = 100) String id,
    @Valid @NotNull String title,
    @Valid @NotNull String description,
    @Valid @NotNull @Pattern(regexp = "OWNERS|RESIDENTS") String audience,
    @Valid @NotNull List<PollOption> options,
    @Valid @NotNull Instant closesAt,
    @Valid @NotNull Instant createdAt,
    @Valid @NotNull @Pattern(regexp = "DRAFT|OPEN|CLOSED") String status,
    @Valid @NotNull List<PollBallot> ballots,
    @Valid @NotNull List<PollResult> results,
    @Valid @NotNull List<PollEligibleUnit> eligibleUnits,
    @Valid @NotNull @Min(0L) @Max(9007199254740991L) Long totalVotes,
    @Valid @NotNull @Min(0L) @Max(9007199254740991L) Long version) {}
