package com.govener.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

/** Transport contract generated from docs/openapi.json; domain models are separate. */
public record Upload(
    @Valid @NotNull @Pattern(regexp = "REQUEST_PHOTO|PAYMENT_RECEIPT|EXPENSE_DOCUMENT")
        String purpose,
    @Valid @NotNull byte[] file) {}
