package com.govener.finance.adapter.out.mongo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.govener.finance.application.FinancePorts;
import com.govener.finance.domain.Payment;
import com.govener.shared.adapter.out.mongo.MongoAggregateRepository;
import org.springframework.data.mongodb.core.MongoTemplate;

public final class MongoPaymentRepository extends MongoAggregateRepository<Payment>
    implements FinancePorts.Payments {
  public MongoPaymentRepository(MongoTemplate mongo, ObjectMapper mapper) {
    super(mongo, mapper, "payments", Payment.class);
  }

  @Override
  protected java.util.Map<String, Object> indexedFields(Payment payment) {
    java.util.Map<String, Object> fields = new java.util.HashMap<>();
    fields.put(
        "activeNotificationId", payment.reversedAt() == null ? payment.notificationId() : null);
    return fields;
  }
}
