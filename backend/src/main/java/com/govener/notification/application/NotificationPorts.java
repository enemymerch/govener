package com.govener.notification.application;

import com.govener.notification.domain.*;
import com.govener.shared.application.AggregateRepository;

public final class NotificationPorts {
  private NotificationPorts() {}

  public interface Notifications extends AggregateRepository<AppNotification> {}

  public interface Events extends AggregateRepository<DomainEvent> {}

  public interface Publisher {
    void publish(DomainEvent event);
  }
}
