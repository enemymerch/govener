package com.govener.service.adapter.out.mongo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.govener.service.application.RequestPorts;
import com.govener.service.domain.ServiceRequest;
import com.govener.shared.adapter.out.mongo.MongoAggregateRepository;
import org.springframework.data.mongodb.core.MongoTemplate;

public final class MongoRequestRepository extends MongoAggregateRepository<ServiceRequest>
    implements RequestPorts.Requests {
  public MongoRequestRepository(MongoTemplate mongo, ObjectMapper mapper) {
    super(mongo, mapper, "requests", ServiceRequest.class);
  }
}
