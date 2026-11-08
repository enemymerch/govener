package com.govener.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

/** Transport contract generated from docs/openapi.json; domain models are separate. */
public record ProblemFieldErrorsItem(
    @Valid @NotNull String field, @Valid @NotNull String code, @Valid @NotNull String message) {}
