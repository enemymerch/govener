package com.govener.notification.application;

import com.govener.community.application.AccessService;
import com.govener.identity.application.IdentityPorts;
import com.govener.notification.domain.*;
import com.govener.shared.application.*;
import com.govener.shared.domain.*;
import java.time.Clock;

public final class OutboxProjector {
  private final NotificationPorts.Events events;
  private final NotificationPorts.Notifications notifications;
  private final IdentityPorts.Users users;
  private final AccessService access;
  private final TransactionPort transactions;
  private final SerializationPort serialization;
  private final Clock clock;

  public OutboxProjector(
      NotificationPorts.Events events,
      NotificationPorts.Notifications notifications,
      IdentityPorts.Users users,
      AccessService access,
      TransactionPort transactions,
      SerializationPort serialization,
      Clock clock) {
    this.events = events;
    this.notifications = notifications;
    this.users = users;
    this.access = access;
    this.transactions = transactions;
    this.serialization = serialization;
    this.clock = clock;
  }

  public void process() {
    for (DomainEvent candidate :
        events.all().stream().filter(item -> item.processedAt() == null).limit(100).toList()) {
      try {
        transactions.execute(
            () -> {
              DomainEvent event = events.find(candidate.id()).orElseThrow();
              if (event.processedAt() != null) return null;
              for (String id : event.recipientIds()) {
                var user = users.find(id);
                if (user.isEmpty() || !user.get().preferences().inApp()) continue;
                try {
                  access.membership(id, event.communityId());
                } catch (RuleViolation revoked) {
                  continue;
                }
                String notificationId = serialization.hash(event.id() + ":" + id);
                if (notifications.find(notificationId).isEmpty())
                  notifications.insert(
                      new AppNotification(
                          notificationId,
                          event.communityId(),
                          id,
                          event.type(),
                          event.title(),
                          event.body(),
                          event.targetKind(),
                          event.targetId(),
                          event.createdAt(),
                          null,
                          0));
              }
              events.save(event.processed(clock.instant()), event.version());
              return null;
            });
      } catch (RuleViolation conflict) {
        if (conflict.kind() != RuleViolation.Kind.CONFLICT) throw conflict;
      }
    }
  }
}
