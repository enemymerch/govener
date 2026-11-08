package com.govener.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

/** Transport contract generated from docs/openapi.json; domain models are separate. */
public record DirectoryUnit(
    @Valid @NotNull @NotBlank @Size(min = 1, max = 100) String id,
    @Valid @NotNull String label,
    @Valid @NotNull @NotBlank @Size(min = 1, max = 100) String blockId,
    @Valid @NotNull String blockLabel,
    @Valid @NotNull Boolean isMyUnit,
    @Valid @NotNull List<DirectoryPerson> residents) {}
