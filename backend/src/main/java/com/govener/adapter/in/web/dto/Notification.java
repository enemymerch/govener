package com.govener.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

/** Transport contract generated from docs/openapi.json; domain models are separate. */
public record Notification(
    @Valid @NotNull @NotBlank @Size(min = 1, max = 100) String id,
    @Valid @NotNull @Pattern(regexp = "ANNOUNCEMENT|CHARGE|PAYMENT|REQUEST|SYSTEM") String type,
    @Valid @NotNull String title,
    @Valid @NotNull String body,
    @Valid @NotNull NotificationTarget target,
    @Valid @NotNull Instant createdAt,
    @Valid Instant readAt) {}
