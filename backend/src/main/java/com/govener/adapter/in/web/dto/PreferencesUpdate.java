package com.govener.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

/** Transport contract generated from docs/openapi.json; domain models are separate. */
public record PreferencesUpdate(
    @Valid @NotNull @Pattern(regexp = "tr|en") String language,
    @Valid @NotNull Boolean inApp,
    @Valid @NotNull Boolean push,
    @Valid @NotNull Boolean emailNotifications,
    @Valid @NotNull @Min(0L) @Max(9007199254740991L) Long expectedVersion) {}
