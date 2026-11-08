package com.govener.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

/** Transport contract generated from docs/openapi.json; domain models are separate. */
public record NotificationReview(
    @Valid @NotNull @Pattern(regexp = "REVIEWED|REJECTED") String decision,
    @Valid @NotNull @Size(min = 0, max = 500) String note,
    @Valid @NotNull @Min(0L) @Max(9007199254740991L) Long expectedVersion) {}
