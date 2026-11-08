package com.govener.service.domain;

import com.govener.shared.domain.*;
import java.time.Instant;

public record RequestComment(
    String id,
    String communityId,
    String requestId,
    Actor author,
    String body,
    Instant createdAt,
    long version)
    implements ScopedAggregate {
  public RequestComment {
    body = Checks.text(body, 2000);
  }
}
