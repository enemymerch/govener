package com.govener.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

/** Transport contract generated from docs/openapi.json; domain models are separate. */
public record Community(
    @Valid @NotNull @NotBlank @Size(min = 1, max = 100) String id,
    @Valid @NotNull String name,
    @Valid @NotNull String address,
    @Valid @NotNull @Pattern(regexp = "Europe/Istanbul") String timeZone,
    @Valid @NotNull @Pattern(regexp = "TRY") String currency) {}
