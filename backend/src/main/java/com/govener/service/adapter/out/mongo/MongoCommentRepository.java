package com.govener.service.adapter.out.mongo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.govener.service.application.RequestPorts;
import com.govener.service.domain.RequestComment;
import com.govener.shared.adapter.out.mongo.MongoAggregateRepository;
import org.springframework.data.mongodb.core.MongoTemplate;

public final class MongoCommentRepository extends MongoAggregateRepository<RequestComment>
    implements RequestPorts.Comments {
  public MongoCommentRepository(MongoTemplate mongo, ObjectMapper mapper) {
    super(mongo, mapper, "request_comments", RequestComment.class);
  }
}
