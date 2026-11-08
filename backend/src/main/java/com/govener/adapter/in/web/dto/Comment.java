package com.govener.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

/** Transport contract generated from docs/openapi.json; domain models are separate. */
public record Comment(
    @Valid @NotNull @NotBlank @Size(min = 1, max = 100) String id,
    @Valid @NotNull DirectoryPerson author,
    @Valid @NotNull String body,
    @Valid @NotNull Instant createdAt) {}
