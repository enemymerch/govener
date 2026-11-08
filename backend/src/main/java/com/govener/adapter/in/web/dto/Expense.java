package com.govener.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

/** Transport contract generated from docs/openapi.json; domain models are separate. */
public record Expense(
    @Valid @NotNull @NotBlank @Size(min = 1, max = 100) String id,
    @Valid @NotNull String description,
    @Valid @NotNull @Pattern(regexp = "MAINTENANCE|UTILITIES|CLEANING|OTHER") String category,
    @Valid @NotNull String vendor,
    @Valid @NotNull LocalDate incurredOn,
    @Valid @NotNull @Min(1L) @Max(999999999L) Long amountMinor,
    @Valid @NotNull @Pattern(regexp = "TRY") String currency,
    @Valid @NotNull @Min(-9007199254740991L) @Max(9007199254740991L) Long paidMinor,
    @Valid Attachment document,
    @Valid @NotNull @Min(0L) @Max(9007199254740991L) Long version) {}
