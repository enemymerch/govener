package com.govener.identity.domain;

import com.govener.shared.domain.*;
import java.time.Instant;

public record PasswordReset(
    String id, String userId, Instant expiresAt, Instant consumedAt, long version)
    implements Aggregate, Expiring {
  public PasswordReset consume(Instant now) {
    if (consumedAt != null || !expiresAt.isAfter(now))
      throw RuleViolation.invalid("Reset link is invalid or expired.");
    return new PasswordReset(id, userId, expiresAt, now, version + 1);
  }
}
