package com.govener.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

/** Transport contract generated from docs/openapi.json; domain models are separate. */
public record RequestStatusChange(
    @Valid @NotNull @Pattern(regexp = "OPEN|IN_PROGRESS|RESOLVED") String status,
    @Valid @NotNull @Min(0L) @Max(9007199254740991L) Long expectedVersion) {}
