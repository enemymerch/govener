package com.govener.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

/** Transport contract generated from docs/openapi.json; domain models are separate. */
public record Attachment(
    @Valid @NotNull @NotBlank @Size(min = 1, max = 100) String id,
    @Valid @NotNull String fileName,
    @Valid @NotNull @Pattern(regexp = "application/pdf|image/png|image/jpeg|image/webp")
        String mediaType,
    @Valid @NotNull @Min(1L) @Max(5242880L) Long sizeBytes,
    @Valid @NotNull @Pattern(regexp = "REQUEST_PHOTO|PAYMENT_RECEIPT|EXPENSE_DOCUMENT")
        String purpose,
    @Valid @NotNull String contentUrl,
    @Valid @NotNull Instant createdAt) {}
