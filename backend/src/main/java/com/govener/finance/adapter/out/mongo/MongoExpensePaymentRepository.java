package com.govener.finance.adapter.out.mongo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.govener.finance.application.FinancePorts;
import com.govener.finance.domain.ExpensePayment;
import com.govener.shared.adapter.out.mongo.MongoAggregateRepository;
import org.springframework.data.mongodb.core.MongoTemplate;

public final class MongoExpensePaymentRepository extends MongoAggregateRepository<ExpensePayment>
    implements FinancePorts.ExpensePayments {
  public MongoExpensePaymentRepository(MongoTemplate mongo, ObjectMapper mapper) {
    super(mongo, mapper, "expense_payments", ExpensePayment.class);
  }
}
