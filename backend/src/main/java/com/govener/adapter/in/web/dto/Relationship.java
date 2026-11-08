package com.govener.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

/** Transport contract generated from docs/openapi.json; domain models are separate. */
public record Relationship(
    @Valid @NotNull @NotBlank @Size(min = 1, max = 100) String id,
    @Valid @NotNull @NotBlank @Size(min = 1, max = 100) String unitId,
    @Valid @NotNull DirectoryPerson person,
    @Valid @NotNull @Pattern(regexp = "OWNER|TENANT") String type,
    @Valid @NotNull LocalDate startsOn,
    @Valid LocalDate endsOn) {}
