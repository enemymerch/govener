package com.govener.finance.adapter.out.mongo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.govener.finance.application.FinancePorts;
import com.govener.finance.domain.PaymentNotification;
import com.govener.shared.adapter.out.mongo.MongoAggregateRepository;
import org.springframework.data.mongodb.core.MongoTemplate;

public final class MongoPaymentNotificationRepository
    extends MongoAggregateRepository<PaymentNotification> implements FinancePorts.Notifications {
  public MongoPaymentNotificationRepository(MongoTemplate mongo, ObjectMapper mapper) {
    super(mongo, mapper, "payment_notifications", PaymentNotification.class);
  }
}
