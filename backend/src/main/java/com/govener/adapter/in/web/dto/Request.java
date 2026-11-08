package com.govener.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

/** Transport contract generated from docs/openapi.json; domain models are separate. */
public record Request(
    @Valid @NotNull @NotBlank @Size(min = 1, max = 100) String id,
    @Valid @NotNull @Min(1L) @Max(9007199254740991L) Long displayNumber,
    @Valid @NotNull @NotBlank @Size(min = 1, max = 100) String communityId,
    @Valid @Size(min = 1, max = 100) String unitId,
    @Valid @NotNull String title,
    @Valid @NotNull String location,
    @Valid @NotNull String description,
    @Valid @NotNull @Pattern(regexp = "OPEN|IN_PROGRESS|RESOLVED") String status,
    @Valid @NotNull DirectoryPerson creator,
    @Valid DirectoryPerson assignee,
    @Valid @NotNull List<Attachment> photos,
    @Valid @NotNull List<Comment> comments,
    @Valid @NotNull List<RequestEvent> history,
    @Valid @NotNull Instant createdAt,
    @Valid @NotNull @Min(0L) @Max(9007199254740991L) Long version) {}
