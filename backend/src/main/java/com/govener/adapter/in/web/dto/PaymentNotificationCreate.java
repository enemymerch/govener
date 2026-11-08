package com.govener.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

/** Transport contract generated from docs/openapi.json; domain models are separate. */
public record PaymentNotificationCreate(
    @Valid @NotNull @NotBlank @Size(min = 1, max = 100) String unitId,
    @Valid @NotNull @Min(1L) @Max(999999999L) Long amountMinor,
    @Valid @NotNull @Pattern(regexp = "TRY") String currency,
    @Valid @NotNull LocalDate paymentDate,
    @Valid @NotNull @NotBlank @Size(min = 1, max = 100) String reference,
    @Valid @NotNull @Size(min = 0, max = 500) String note,
    @Valid @Size(min = 1, max = 100) String receiptId) {}
