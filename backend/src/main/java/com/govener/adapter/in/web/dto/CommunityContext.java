package com.govener.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

/** Transport contract generated from docs/openapi.json; domain models are separate. */
public record CommunityContext(
    @Valid @NotNull Community community,
    @Valid @NotNull List<String> roles,
    @Valid @NotNull List<Unit> ownUnits,
    @Valid @NotNull List<String> permissions,
    @Valid @NotNull Profile profile) {}
