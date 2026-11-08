package com.govener.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

/** Transport contract generated from docs/openapi.json; domain models are separate. */
public record DirectoryPerson(
    @Valid @NotNull @NotBlank @Size(min = 1, max = 100) String id, @Valid @NotNull String name) {}
