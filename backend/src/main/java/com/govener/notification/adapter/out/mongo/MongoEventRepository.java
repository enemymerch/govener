package com.govener.notification.adapter.out.mongo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.govener.notification.application.NotificationPorts;
import com.govener.notification.domain.DomainEvent;
import com.govener.shared.adapter.out.mongo.MongoAggregateRepository;
import org.springframework.data.mongodb.core.MongoTemplate;

public final class MongoEventRepository extends MongoAggregateRepository<DomainEvent>
    implements NotificationPorts.Events {
  public MongoEventRepository(MongoTemplate mongo, ObjectMapper mapper) {
    super(mongo, mapper, "outbox_events", DomainEvent.class);
  }
}
