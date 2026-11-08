package com.govener.notification.application;

import com.govener.notification.domain.DomainEvent;
import com.govener.shared.application.AuditPort;
import com.govener.shared.domain.*;
import java.time.Clock;
import java.util.*;

public final class ActivityPublisher {
  private final AuditPort audit;
  private final NotificationPorts.Publisher publisher;
  private final Clock clock;

  public ActivityPublisher(AuditPort audit, NotificationPorts.Publisher publisher, Clock clock) {
    this.audit = audit;
    this.publisher = publisher;
    this.clock = clock;
  }

  public void emit(
      Actor actor,
      String communityId,
      String operation,
      String resourceId,
      String reason,
      List<String> recipients,
      String type,
      String title,
      String targetKind) {
    emit(
        actor,
        communityId,
        operation,
        resourceId,
        reason,
        recipients,
        type,
        title,
        targetKind,
        resourceId);
  }

  public void emit(
      Actor actor,
      String communityId,
      String operation,
      String resourceId,
      String reason,
      List<String> recipients,
      String type,
      String title,
      String targetKind,
      String targetId) {
    audit.append(
        new AuditRecord(
            UUID.randomUUID().toString(),
            communityId,
            actor.id(),
            operation,
            resourceId,
            reason == null ? "" : reason,
            clock.instant(),
            0));
    publisher.publish(
        new DomainEvent(
            UUID.randomUUID().toString(),
            communityId,
            recipients.stream().distinct().toList(),
            type,
            title,
            "",
            targetKind,
            targetId,
            clock.instant(),
            null,
            0));
  }
}
