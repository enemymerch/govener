package com.govener.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

/** Transport contract generated from docs/openapi.json; domain models are separate. */
public record AnnouncementWrite(
    @Valid @NotNull @NotBlank @Size(min = 1, max = 160) String title,
    @Valid @NotNull @NotBlank @Size(min = 1, max = 10000) String body,
    @Valid @NotNull @Pattern(regexp = "MAINTENANCE|COMMUNITY") String category,
    @Valid @NotNull Audience audience,
    @Valid @NotNull @Pattern(regexp = "DRAFT|PUBLISHED") String status) {}
