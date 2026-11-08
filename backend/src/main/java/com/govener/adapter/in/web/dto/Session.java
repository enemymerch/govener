package com.govener.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

/** Transport contract generated from docs/openapi.json; domain models are separate. */
public record Session(
    @Valid @NotNull Profile user,
    @Valid @NotNull List<String> roles,
    @Valid @NotNull Instant expiresAt,
    @Valid @NotNull String csrfToken,
    @Valid @NotNull List<String> permissions) {}
