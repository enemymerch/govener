package com.govener.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

/** Transport contract generated from docs/openapi.json; domain models are separate. */
public record ReasonCommand(
    @Valid @NotNull @NotBlank @Size(min = 1, max = 500) String reason,
    @Valid @NotNull @Min(0L) @Max(9007199254740991L) Long expectedVersion) {}
