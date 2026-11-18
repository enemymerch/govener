package com.govener.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

/** Transport contract generated from docs/openapi.json; domain models are separate. */
public record PollBallot(
    @Valid @NotNull @NotBlank @Size(min = 1, max = 100) String unitId,
    @Valid @NotNull @NotBlank @Size(min = 1, max = 100) String userId,
    @Valid @NotNull String voterName,
    @Valid @NotNull String unitLabel,
    @Valid @NotNull @NotBlank @Size(min = 1, max = 100) String optionId,
    @Valid @NotNull Instant votedAt) {}
