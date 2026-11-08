package com.govener.service.domain;

import com.govener.shared.domain.*;
import java.time.Instant;

public record RequestEvent(
    String id,
    String communityId,
    String requestId,
    String kind,
    Actor actor,
    Instant createdAt,
    String from,
    String to,
    long version)
    implements ScopedAggregate {}
