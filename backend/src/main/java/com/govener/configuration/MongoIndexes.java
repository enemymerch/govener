package com.govener.configuration;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;

@Component
public final class MongoIndexes {
  private final MongoTemplate mongo;

  public MongoIndexes(MongoTemplate mongo) {
    this.mongo = mongo;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void create() {
    for (String name :
        List.of(
            "users",
            "password_resets",
            "communities",
            "units",
            "people",
            "memberships",
            "relationships",
            "residences",
            "charges",
            "payments",
            "financial_accounts",
            "unit_accounts",
            "account_entries",
            "expenses",
            "expense_payments",
            "payment_notifications",
            "charge_batches",
            "idempotency_results",
            "requests",
            "request_comments",
            "request_events",
            "announcements",
            "notifications",
            "outbox_events",
            "attachments",
            "audit_records",
            "reset_email_outbox")) {
      if (!mongo.collectionExists(name)) mongo.createCollection(name);
      mongo
          .indexOps(name)
          .createIndex(
              new Index().on("communityId", Sort.Direction.ASC).on("_id", Sort.Direction.ASC));
    }
    mongo
        .indexOps("users")
        .createIndex(new Index().on("data.loginEmail", Sort.Direction.ASC).unique());
    mongo
        .indexOps("memberships")
        .createIndex(
            new Index()
                .on("communityId", Sort.Direction.ASC)
                .on("data.userId", Sort.Direction.ASC)
                .unique());
    mongo
        .indexOps("unit_accounts")
        .createIndex(
            new Index()
                .on("communityId", Sort.Direction.ASC)
                .on("data.unitId", Sort.Direction.ASC)
                .unique());
    mongo
        .indexOps("payments")
        .createIndex(
            new Index()
                .on("communityId", Sort.Direction.ASC)
                .on("activeNotificationId", Sort.Direction.ASC)
                .unique()
                .partial(
                    PartialIndexFilter.of(Criteria.where("activeNotificationId").exists(true))));
    for (String name :
        List.of("password_resets", "idempotency_results", "auth_attempts", "reset_email_outbox"))
      mongo
          .indexOps(name)
          .createIndex(new Index().on("expiresAt", Sort.Direction.ASC).expire(Duration.ZERO));
    mongo
        .indexOps("notifications")
        .createIndex(
            new Index()
                .on("communityId", Sort.Direction.ASC)
                .on("data.userId", Sort.Direction.ASC));
  }
}
