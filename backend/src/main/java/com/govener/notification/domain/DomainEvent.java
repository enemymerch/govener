package com.govener.notification.domain;

import com.govener.shared.domain.*;
import java.time.Instant;
import java.util.List;

public record DomainEvent(
    String id,
    String communityId,
    List<String> recipientIds,
    String type,
    String title,
    String body,
    String targetKind,
    String targetId,
    Instant createdAt,
    Instant processedAt,
    long version)
    implements ScopedAggregate {
  public DomainEvent {
    recipientIds = List.copyOf(recipientIds);
  }

  public DomainEvent processed(Instant now) {
    return new DomainEvent(
        id,
        communityId,
        recipientIds,
        type,
        title,
        body,
        targetKind,
        targetId,
        createdAt,
        now,
        version + 1);
  }
}
