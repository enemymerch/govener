package com.govener.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

/** Transport contract generated from docs/openapi.json; domain models are separate. */
public record RequestEvent(
    @Valid @NotNull @NotBlank @Size(min = 1, max = 100) String id,
    @Valid @NotNull @Pattern(regexp = "CREATED|STATUS_CHANGED|ASSIGNEE_CHANGED") String kind,
    @Valid @NotNull DirectoryPerson actor,
    @Valid @NotNull Instant createdAt,
    @Valid String from,
    @Valid String to) {}
