package com.govener.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

/** Transport contract generated from docs/openapi.json; domain models are separate. */
public record PaymentNotification(
    @Valid @NotNull @NotBlank @Size(min = 1, max = 100) String id,
    @Valid @NotNull @NotBlank @Size(min = 1, max = 100) String unitId,
    @Valid @NotNull @Min(1L) @Max(999999999L) Long amountMinor,
    @Valid @NotNull @Pattern(regexp = "TRY") String currency,
    @Valid @NotNull LocalDate paymentDate,
    @Valid @NotNull String reference,
    @Valid @NotNull String note,
    @Valid Attachment receipt,
    @Valid @NotNull @Pattern(regexp = "PENDING|REVIEWED|REJECTED|WITHDRAWN|RECORDED") String status,
    @Valid @NotNull String reviewNote,
    @Valid @Size(min = 1, max = 100) String paymentId,
    @Valid @NotNull @Min(0L) @Max(9007199254740991L) Long version) {}
