package com.govener.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

/** Transport contract generated from docs/openapi.json; domain models are separate. */
public record AccountEntry(
    @Valid @NotNull @NotBlank @Size(min = 1, max = 100) String id,
    @Valid @NotNull @NotBlank @Size(min = 1, max = 100) String unitId,
    @Valid @NotNull @Pattern(regexp = "CHARGE|PAYMENT|CHARGE_CANCELLATION|PAYMENT_REVERSAL")
        String type,
    @Valid @NotNull @Min(-9007199254740991L) @Max(9007199254740991L) Long amountMinor,
    @Valid @NotNull Instant occurredAt,
    @Valid @NotNull String description,
    @Valid @NotNull @NotBlank @Size(min = 1, max = 100) String sourceId,
    @Valid @NotNull @Min(-9007199254740991L) @Max(9007199254740991L) Long balanceAfterMinor) {}
