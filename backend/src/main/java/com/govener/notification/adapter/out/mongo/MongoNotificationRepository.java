package com.govener.notification.adapter.out.mongo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.govener.notification.application.NotificationPorts;
import com.govener.notification.domain.AppNotification;
import com.govener.shared.adapter.out.mongo.MongoAggregateRepository;
import org.springframework.data.mongodb.core.MongoTemplate;

public final class MongoNotificationRepository extends MongoAggregateRepository<AppNotification>
    implements NotificationPorts.Notifications {
  public MongoNotificationRepository(MongoTemplate mongo, ObjectMapper mapper) {
    super(mongo, mapper, "notifications", AppNotification.class);
  }
}
