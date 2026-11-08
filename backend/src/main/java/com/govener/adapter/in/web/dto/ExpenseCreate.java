package com.govener.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

/** Transport contract generated from docs/openapi.json; domain models are separate. */
public record ExpenseCreate(
    @Valid @NotNull @NotBlank @Size(min = 1, max = 300) String description,
    @Valid @NotNull @Pattern(regexp = "MAINTENANCE|UTILITIES|CLEANING|OTHER") String category,
    @Valid @NotNull @Size(min = 0, max = 150) String vendor,
    @Valid @NotNull LocalDate incurredOn,
    @Valid @NotNull @Min(1L) @Max(999999999L) Long amountMinor,
    @Valid @NotNull @Pattern(regexp = "TRY") String currency,
    @Valid @Size(min = 1, max = 100) String documentId,
    @Valid ExpenseCreateImmediatePayment immediatePayment) {}
