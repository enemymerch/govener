package com.govener.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

/** Transport contract generated from docs/openapi.json; domain models are separate. */
public record Unit(
    @Valid @NotNull @NotBlank @Size(min = 1, max = 100) String id,
    @Valid @NotNull @NotBlank @Size(min = 1, max = 100) String communityId,
    @Valid @NotNull String label,
    @Valid @NotNull @NotBlank @Size(min = 1, max = 100) String blockId,
    @Valid @NotNull String blockLabel,
    @Valid @NotNull @Min(-9007199254740991L) @Max(9007199254740991L) Long floor,
    @Valid @NotNull @Pattern(regexp = "APARTMENT|SHOP|OFFICE") String kind,
    @Valid @NotNull Double areaSqm,
    @Valid @NotNull @Min(0L) @Max(9007199254740991L) Long version) {}
