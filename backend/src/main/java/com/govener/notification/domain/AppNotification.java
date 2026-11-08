package com.govener.notification.domain;

import com.govener.shared.domain.*;
import java.time.Instant;

public record AppNotification(
    String id,
    String communityId,
    String userId,
    String type,
    String title,
    String body,
    String targetKind,
    String targetId,
    Instant createdAt,
    Instant readAt,
    long version)
    implements ScopedAggregate {
  public AppNotification read(Instant now) {
    return readAt == null
        ? new AppNotification(
            id,
            communityId,
            userId,
            type,
            title,
            body,
            targetKind,
            targetId,
            createdAt,
            now,
            version + 1)
        : this;
  }
}
