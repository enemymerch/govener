package com.govener.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

/** Transport contract generated from docs/openapi.json; domain models are separate. */
public record PollVote(
    @Valid @NotNull @NotBlank @Size(min = 1, max = 100) String optionId,
    @Valid @NotNull @Size(min = 1, max = 1000) List<String> unitIds,
    @Valid @NotNull @Min(0L) @Max(9007199254740991L) Long expectedVersion) {}
