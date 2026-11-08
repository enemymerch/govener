package com.govener.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

/** Transport contract generated from docs/openapi.json; domain models are separate. */
public record FinancialAccount(
    @Valid @NotNull @NotBlank @Size(min = 1, max = 100) String id,
    @Valid @NotNull String name,
    @Valid @NotNull @Pattern(regexp = "BANK|CASH") String kind,
    @Valid @NotNull @Pattern(regexp = "TRY") String currency,
    @Valid @NotNull @Min(-9007199254740991L) @Max(9007199254740991L) Long openingMinor,
    @Valid @NotNull @Min(-9007199254740991L) @Max(9007199254740991L) Long balanceMinor,
    @Valid @NotNull @Min(0L) @Max(9007199254740991L) Long version) {}
