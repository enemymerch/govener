package com.govener.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

/** Transport contract generated from docs/openapi.json; domain models are separate. */
public record PollCreate(
    @Valid @NotNull @NotBlank @Size(min = 1, max = 200) String title,
    @Valid @NotNull @Size(min = 0, max = 3000) String description,
    @Valid @NotNull @Pattern(regexp = "OWNERS|RESIDENTS") String audience,
    @Valid @NotNull @Size(min = 2, max = 10) List<String> options,
    @Valid @NotNull Instant closesAt,
    @Valid @NotNull @Pattern(regexp = "DRAFT|OPEN") String status) {}
