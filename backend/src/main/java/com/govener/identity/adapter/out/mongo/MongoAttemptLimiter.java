package com.govener.identity.adapter.out.mongo;

import com.govener.identity.application.IdentityPorts;
import com.govener.shared.application.SerializationPort;
import com.govener.shared.domain.RuleViolation;
import java.time.Clock;
import java.util.Date;
import org.bson.Document;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;

public final class MongoAttemptLimiter implements IdentityPorts.Attempts {
  private final MongoTemplate mongo;
  private final SerializationPort serialization;
  private final Clock clock;

  public MongoAttemptLimiter(MongoTemplate mongo, SerializationPort serialization, Clock clock) {
    this.mongo = mongo;
    this.serialization = serialization;
    this.clock = clock;
  }

  public void consume(String key, int maximum) {
    long window = clock.instant().getEpochSecond() / 60;
    String id = serialization.hash(key + ":" + window);
    Document entry =
        mongo.findAndModify(
            Query.query(Criteria.where("_id").is(id)),
            new Update()
                .inc("attempts", 1)
                .setOnInsert("expiresAt", Date.from(clock.instant().plusSeconds(120))),
            FindAndModifyOptions.options().upsert(true).returnNew(true),
            Document.class,
            "auth_attempts");
    if (entry != null && ((Number) entry.get("attempts")).intValue() > maximum)
      throw new RuleViolation(
          RuleViolation.Kind.RATE_LIMITED, "RATE_LIMITED", "Too many attempts. Try again later.");
  }
}
