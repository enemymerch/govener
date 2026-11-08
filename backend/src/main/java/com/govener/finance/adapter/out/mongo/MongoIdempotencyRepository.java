package com.govener.finance.adapter.out.mongo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.govener.finance.application.FinancePorts;
import com.govener.finance.domain.IdempotentResult;
import com.govener.shared.adapter.out.mongo.MongoAggregateRepository;
import org.springframework.data.mongodb.core.MongoTemplate;

public final class MongoIdempotencyRepository extends MongoAggregateRepository<IdempotentResult>
    implements FinancePorts.Idempotency {
  public MongoIdempotencyRepository(MongoTemplate mongo, ObjectMapper mapper) {
    super(mongo, mapper, "idempotency_results", IdempotentResult.class);
  }
}
