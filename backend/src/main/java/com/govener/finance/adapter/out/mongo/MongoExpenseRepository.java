package com.govener.finance.adapter.out.mongo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.govener.finance.application.FinancePorts;
import com.govener.finance.domain.Expense;
import com.govener.shared.adapter.out.mongo.MongoAggregateRepository;
import org.springframework.data.mongodb.core.MongoTemplate;

public final class MongoExpenseRepository extends MongoAggregateRepository<Expense>
    implements FinancePorts.Expenses {
  public MongoExpenseRepository(MongoTemplate mongo, ObjectMapper mapper) {
    super(mongo, mapper, "expenses", Expense.class);
  }
}
