package com.govener.notification.application;

import com.govener.community.application.AccessService;
import com.govener.notification.domain.*;
import com.govener.shared.application.TransactionPort;
import com.govener.shared.domain.*;
import java.time.*;
import java.util.*;

public final class NotificationService {
  private final NotificationPorts.Notifications store;
  private final AccessService access;
  private final TransactionPort transactions;
  private final Clock clock;

  public NotificationService(
      NotificationPorts.Notifications store,
      AccessService access,
      TransactionPort transactions,
      Clock clock) {
    this.store = store;
    this.access = access;
    this.transactions = transactions;
    this.clock = clock;
  }

  public List<AppNotification> list(String userId, String communityId, boolean unread) {
    access.membership(userId, communityId);
    return store.inCommunity(communityId).stream()
        .filter(item -> item.userId().equals(userId) && (!unread || item.readAt() == null))
        .toList();
  }

  public void read(String userId, String communityId, String id) {
    access.membership(userId, communityId);
    transactions.execute(
        () -> {
          AppNotification notification =
              store
                  .find(Checks.id(id))
                  .filter(
                      item ->
                          item.communityId().equals(communityId) && item.userId().equals(userId))
                  .orElseThrow(RuleViolation::missing);
          if (notification.readAt() == null)
            store.save(notification.read(clock.instant()), notification.version());
          return null;
        });
  }

  public void readAll(String userId, String communityId, Instant before) {
    Checks.required(before);
    for (AppNotification notification : list(userId, communityId, true))
      if (!notification.createdAt().isAfter(before)) {
        try {
          read(userId, communityId, notification.id());
        } catch (RuleViolation failure) {
          if (failure.kind() != RuleViolation.Kind.CONFLICT) throw failure;
        }
      }
  }
}
