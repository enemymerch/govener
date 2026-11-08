package com.govener.service.adapter.out.mongo;

import com.govener.service.application.RequestPorts;
import org.bson.Document;
import org.springframework.data.mongodb.core.*;
import org.springframework.data.mongodb.core.query.*;

public final class MongoSequenceAdapter implements RequestPorts.Sequences {
  private final MongoTemplate mongo;

  public MongoSequenceAdapter(MongoTemplate mongo) {
    this.mongo = mongo;
  }

  public long next(String scope) {
    Document next =
        mongo.findAndModify(
            Query.query(Criteria.where("_id").is(scope)),
            new Update().inc("value", 1),
            FindAndModifyOptions.options().upsert(true).returnNew(true),
            Document.class,
            "sequences");
    return ((Number) java.util.Objects.requireNonNull(next).get("value")).longValue();
  }
}
