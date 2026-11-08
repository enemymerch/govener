package com.govener.identity.adapter.out.mongo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.govener.identity.application.IdentityPorts;
import com.govener.identity.domain.PasswordReset;
import com.govener.shared.adapter.out.mongo.MongoAggregateRepository;
import org.springframework.data.mongodb.core.MongoTemplate;

public final class MongoResetRepository extends MongoAggregateRepository<PasswordReset>
    implements IdentityPorts.Resets {
  public MongoResetRepository(MongoTemplate mongo, ObjectMapper mapper) {
    super(mongo, mapper, "password_resets", PasswordReset.class);
  }
}
