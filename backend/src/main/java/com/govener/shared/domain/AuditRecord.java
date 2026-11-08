package com.govener.shared.domain;

import java.time.Instant;

public record AuditRecord(
    String id,
    String communityId,
    String actorId,
    String operation,
    String resourceId,
    String reason,
    Instant occurredAt,
    long version)
    implements ScopedAggregate {}
