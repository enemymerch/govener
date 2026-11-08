package com.govener.identity.domain;

import com.govener.shared.domain.*;
import java.time.Instant;

public record ResetEmail(
    String id,
    String email,
    String encryptedToken,
    Instant createdAt,
    Instant expiresAt,
    Instant claimedUntil,
    Instant sentAt,
    long version)
    implements Aggregate, Expiring {
  public ResetEmail claim(Instant until) {
    return new ResetEmail(
        id, email, encryptedToken, createdAt, expiresAt, until, sentAt, version + 1);
  }

  public ResetEmail sent(Instant now) {
    return new ResetEmail(id, email, encryptedToken, createdAt, expiresAt, null, now, version + 1);
  }
}
