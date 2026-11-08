package com.govener.finance.domain;

import com.govener.shared.domain.*;
import java.time.Instant;

public record IdempotentResult(
    String id, String communityId, String fingerprint, String json, Instant expiresAt, long version)
    implements ScopedAggregate, Expiring {}
