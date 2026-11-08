package com.govener.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

/** Transport contract generated from docs/openapi.json; domain models are separate. */
public record RequestCreate(
    @Valid @Size(min = 1, max = 100) String unitId,
    @Valid @NotNull @NotBlank @Size(min = 1, max = 120) String title,
    @Valid @NotNull @NotBlank @Size(min = 1, max = 100) String location,
    @Valid @NotNull @Size(min = 0, max = 2000) String description,
    @Valid @NotNull List<String> photoIds) {}
