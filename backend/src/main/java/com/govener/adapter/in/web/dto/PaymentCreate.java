package com.govener.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

/** Transport contract generated from docs/openapi.json; domain models are separate. */
public record PaymentCreate(
    @Valid @NotNull @NotBlank @Size(min = 1, max = 100) String unitId,
    @Valid @NotNull @NotBlank @Size(min = 1, max = 100) String accountId,
    @Valid @NotNull @Min(1L) @Max(999999999L) Long amountMinor,
    @Valid @NotNull @Pattern(regexp = "TRY") String currency,
    @Valid @NotNull LocalDate paidOn,
    @Valid @NotNull @NotBlank @Size(min = 1, max = 100) String reference,
    @Valid @NotNull List<Allocation> allocations,
    @Valid @Size(min = 1, max = 100) String notificationId,
    @Valid @NotNull @Min(0L) @Max(9007199254740991L) Long expectedAccountVersion) {}
