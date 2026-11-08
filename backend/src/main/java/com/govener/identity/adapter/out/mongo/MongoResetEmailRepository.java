package com.govener.identity.adapter.out.mongo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.govener.identity.domain.ResetEmail;
import com.govener.shared.adapter.out.mongo.MongoAggregateRepository;
import org.springframework.data.mongodb.core.MongoTemplate;

public final class MongoResetEmailRepository extends MongoAggregateRepository<ResetEmail> {
  public MongoResetEmailRepository(MongoTemplate mongo, ObjectMapper mapper) {
    super(mongo, mapper, "reset_email_outbox", ResetEmail.class);
  }
}
