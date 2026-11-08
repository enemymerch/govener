package com.govener.finance.adapter.out.mongo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.govener.finance.application.FinancePorts;
import com.govener.finance.domain.Charge;
import com.govener.shared.adapter.out.mongo.MongoAggregateRepository;
import org.springframework.data.mongodb.core.MongoTemplate;

public final class MongoChargeRepository extends MongoAggregateRepository<Charge>
    implements FinancePorts.Charges {
  public MongoChargeRepository(MongoTemplate mongo, ObjectMapper mapper) {
    super(mongo, mapper, "charges", Charge.class);
  }
}
