package com.govener.finance.adapter.out.mongo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.govener.finance.application.FinancePorts;
import com.govener.finance.domain.AccountEntry;
import com.govener.shared.adapter.out.mongo.MongoAggregateRepository;
import org.springframework.data.mongodb.core.MongoTemplate;

public final class MongoAccountEntryRepository extends MongoAggregateRepository<AccountEntry>
    implements FinancePorts.Entries {
  public MongoAccountEntryRepository(MongoTemplate mongo, ObjectMapper mapper) {
    super(mongo, mapper, "account_entries", AccountEntry.class);
  }
}
