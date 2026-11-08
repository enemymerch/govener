package com.govener.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

/** Transport contract generated from docs/openapi.json; domain models are separate. */
public record Dashboard(
    @Valid @NotNull @NotBlank @Size(min = 1, max = 100) String communityId,
    @Valid @NotNull @Pattern(regexp = "TRY") String currency,
    @Valid @NotNull LocalDate period,
    @Valid LocalDate nextDueOn,
    @Valid @NotNull @Min(-9007199254740991L) @Max(9007199254740991L) Long collectedMinor,
    @Valid @NotNull @Min(-9007199254740991L) @Max(9007199254740991L) Long outstandingMinor,
    @Valid @NotNull @Min(-9007199254740991L) @Max(9007199254740991L) Long targetMinor,
    @Valid @NotNull @Min(-9007199254740991L) @Max(9007199254740991L) Long paidUnitCount,
    @Valid @NotNull @Min(-9007199254740991L) @Max(9007199254740991L) Long unitCount,
    @Valid @NotNull @Min(-9007199254740991L) @Max(9007199254740991L) Long openRequestCount,
    @Valid UnitAccount ownAccount) {}
