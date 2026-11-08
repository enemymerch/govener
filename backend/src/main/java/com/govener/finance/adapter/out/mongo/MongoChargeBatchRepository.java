package com.govener.finance.adapter.out.mongo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.govener.finance.application.FinancePorts;
import com.govener.finance.domain.ChargeBatch;
import com.govener.shared.adapter.out.mongo.MongoAggregateRepository;
import org.springframework.data.mongodb.core.MongoTemplate;

public final class MongoChargeBatchRepository extends MongoAggregateRepository<ChargeBatch>
    implements FinancePorts.Batches {
  public MongoChargeBatchRepository(MongoTemplate mongo, ObjectMapper mapper) {
    super(mongo, mapper, "charge_batches", ChargeBatch.class);
  }
}
