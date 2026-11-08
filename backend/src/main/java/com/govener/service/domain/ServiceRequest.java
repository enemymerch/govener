package com.govener.service.domain;

import com.govener.shared.domain.*;
import java.time.Instant;
import java.util.List;

public record ServiceRequest(
    String id,
    String communityId,
    long displayNumber,
    String unitId,
    String title,
    String location,
    String description,
    RequestStatus status,
    Actor creator,
    Actor assignee,
    List<String> photoIds,
    Instant createdAt,
    long version)
    implements ScopedAggregate {
  public ServiceRequest {
    id = Checks.id(id);
    communityId = Checks.id(communityId);
    title = Checks.text(title, 120);
    location = Checks.text(location, 100);
    description = Checks.optional(description, 2000);
    Checks.required(status);
    Checks.required(creator);
    photoIds = List.copyOf(photoIds);
    if (photoIds.size() > 3 || photoIds.stream().distinct().count() != photoIds.size())
      throw RuleViolation.invalid("At most three distinct photos are permitted.");
  }

  public ServiceRequest changeStatus(RequestStatus next) {
    return new ServiceRequest(
        id,
        communityId,
        displayNumber,
        unitId,
        title,
        location,
        description,
        Checks.required(next),
        creator,
        assignee,
        photoIds,
        createdAt,
        version + 1);
  }

  public ServiceRequest assign(Actor next) {
    return new ServiceRequest(
        id,
        communityId,
        displayNumber,
        unitId,
        title,
        location,
        description,
        status,
        creator,
        next,
        photoIds,
        createdAt,
        version + 1);
  }

  public boolean visibleTo(String userId, boolean manager) {
    return manager || creator.id().equals(userId);
  }
}
