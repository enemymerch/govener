package com.govener.service.adapter.out.mongo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.govener.service.application.RequestPorts;
import com.govener.service.domain.RequestEvent;
import com.govener.shared.adapter.out.mongo.MongoAggregateRepository;
import org.springframework.data.mongodb.core.MongoTemplate;

public final class MongoRequestEventRepository extends MongoAggregateRepository<RequestEvent>
    implements RequestPorts.Events {
  public MongoRequestEventRepository(MongoTemplate mongo, ObjectMapper mapper) {
    super(mongo, mapper, "request_events", RequestEvent.class);
  }
}
