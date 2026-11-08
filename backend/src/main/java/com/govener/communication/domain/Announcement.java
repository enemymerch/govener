package com.govener.communication.domain;

import com.govener.shared.domain.*;
import java.time.Instant;

public record Announcement(
    String id,
    String communityId,
    String title,
    String body,
    String category,
    Audience audience,
    String status,
    Actor author,
    Instant createdAt,
    Instant updatedAt,
    Instant publishedAt,
    long version)
    implements ScopedAggregate {
  public Announcement {
    Checks.id(id);
    Checks.id(communityId);
    title = Checks.text(title, 160);
    body = Checks.text(body, 10000);
    Checks.required(audience);
    if (!java.util.Set.of("MAINTENANCE", "COMMUNITY").contains(category)
        || !java.util.Set.of("DRAFT", "PUBLISHED").contains(status))
      throw RuleViolation.invalid("Invalid announcement.");
  }

  public Announcement update(
      String nextTitle,
      String nextBody,
      String nextCategory,
      Audience nextAudience,
      String nextStatus,
      Instant now) {
    return new Announcement(
        id,
        communityId,
        nextTitle,
        nextBody,
        nextCategory,
        nextAudience,
        nextStatus,
        author,
        createdAt,
        now,
        nextStatus.equals("PUBLISHED") ? now : null,
        version + 1);
  }
}
