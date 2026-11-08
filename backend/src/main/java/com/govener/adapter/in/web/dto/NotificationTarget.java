package com.govener.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

/** Transport contract generated from docs/openapi.json; domain models are separate. */
public record NotificationTarget(
    @Valid @NotNull @Pattern(regexp = "OVERVIEW|ANNOUNCEMENT|ACCOUNT|REQUEST|PAYMENT_NOTIFICATION")
        String kind,
    @Valid @Size(min = 1, max = 100) String id) {}
