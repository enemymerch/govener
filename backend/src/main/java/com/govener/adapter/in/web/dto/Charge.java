package com.govener.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

/** Transport contract generated from docs/openapi.json; domain models are separate. */
public record Charge(
    @Valid @NotNull @NotBlank @Size(min = 1, max = 100) String id,
    @Valid @NotNull @NotBlank @Size(min = 1, max = 100) String unitId,
    @Valid @NotNull @NotBlank @Size(min = 1, max = 300) String description,
    @Valid @NotNull LocalDate periodStart,
    @Valid @NotNull LocalDate periodEnd,
    @Valid @NotNull LocalDate issuedOn,
    @Valid @NotNull LocalDate dueOn,
    @Valid @NotNull @Min(1L) @Max(999999999L) Long amountMinor,
    @Valid @NotNull @Pattern(regexp = "TRY") String currency,
    @Valid @NotNull @Min(0L) @Max(9007199254740991L) Long allocatedMinor,
    @Valid @NotNull @Min(0L) @Max(9007199254740991L) Long outstandingMinor,
    @Valid @NotNull @Pattern(regexp = "OUTSTANDING|PARTIALLY_PAID|PAID|CANCELLED") String status,
    @Valid Instant cancelledAt,
    @Valid String cancellationReason,
    @Valid @NotNull @Min(0L) @Max(9007199254740991L) Long version) {}
