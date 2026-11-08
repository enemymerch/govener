package com.govener.finance.domain;

import com.govener.shared.domain.*;
import java.time.Instant;

public record AccountEntry(
    String id,
    String communityId,
    String unitId,
    String type,
    long amountMinor,
    Instant occurredAt,
    String description,
    String sourceId,
    long balanceAfterMinor,
    long version)
    implements ScopedAggregate {}
