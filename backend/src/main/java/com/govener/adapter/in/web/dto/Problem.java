package com.govener.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

/** Transport contract generated from docs/openapi.json; domain models are separate. */
public record Problem(
    @Valid @NotNull String type,
    @Valid @NotNull String title,
    @Valid @NotNull @Min(400L) @Max(599L) Long status,
    @Valid String detail,
    @Valid String instance,
    @Valid @NotNull String code,
    @Valid String traceId,
    @Valid List<ProblemFieldErrorsItem> fieldErrors) {}
