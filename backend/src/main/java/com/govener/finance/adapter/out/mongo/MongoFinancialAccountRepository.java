package com.govener.finance.adapter.out.mongo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.govener.finance.application.FinancePorts;
import com.govener.finance.domain.FinancialAccount;
import com.govener.shared.adapter.out.mongo.MongoAggregateRepository;
import org.springframework.data.mongodb.core.MongoTemplate;

public final class MongoFinancialAccountRepository
    extends MongoAggregateRepository<FinancialAccount> implements FinancePorts.Accounts {
  public MongoFinancialAccountRepository(MongoTemplate mongo, ObjectMapper mapper) {
    super(mongo, mapper, "financial_accounts", FinancialAccount.class);
  }
}
