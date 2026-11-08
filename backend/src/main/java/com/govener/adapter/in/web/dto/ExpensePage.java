package com.govener.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

/** Transport contract generated from docs/openapi.json; domain models are separate. */
public record ExpensePage(
    @Valid @NotNull List<Expense> items,
    @Valid String nextCursor,
    @Valid @NotNull Boolean hasMore) {}
