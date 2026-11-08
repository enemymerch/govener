package com.govener.shared.adapter.out.mongo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.govener.shared.application.AggregateRepository;
import com.govener.shared.domain.*;
import java.util.*;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;

public abstract class MongoAggregateRepository<T extends Aggregate>
    implements AggregateRepository<T> {
  protected final MongoTemplate mongo;
  protected final ObjectMapper mapper;
  protected final String collection;
  private final Class<T> type;

  protected MongoAggregateRepository(
      MongoTemplate mongo, ObjectMapper mapper, String collection, Class<T> type) {
    this.mongo = mongo;
    this.mapper = mapper;
    this.collection = collection;
    this.type = type;
  }

  protected Map<String, Object> indexedFields(T aggregate) {
    return Map.of();
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> data(T aggregate) {
    return mapper.convertValue(aggregate, Map.class);
  }

  protected T decode(Document doc) {
    return mapper.convertValue(doc.get("data"), type);
  }

  protected List<T> matching(Query query) {
    return mongo.find(query, Document.class, collection).stream().map(this::decode).toList();
  }

  public Optional<T> find(String id) {
    return Optional.ofNullable(mongo.findById(id, Document.class, collection)).map(this::decode);
  }

  public List<T> all() {
    return matching(new Query());
  }

  public List<T> inCommunity(String communityId) {
    return matching(Query.query(Criteria.where("communityId").is(communityId)));
  }

  public void insert(T aggregate) {
    Document doc =
        new Document("_id", aggregate.id())
            .append("version", aggregate.version())
            .append("data", data(aggregate));
    if (aggregate instanceof ScopedAggregate scoped) doc.put("communityId", scoped.communityId());
    if (aggregate instanceof Expiring expiring)
      doc.put("expiresAt", Date.from(expiring.expiresAt()));
    indexedFields(aggregate)
        .forEach(
            (key, value) -> {
              if (value != null) doc.put(key, value);
            });
    mongo.insert(doc, collection);
  }

  public void save(T aggregate, long expectedVersion) {
    if (aggregate.version() != expectedVersion + 1)
      throw RuleViolation.conflict("Invalid aggregate version transition.");
    Criteria filter = Criteria.where("_id").is(aggregate.id()).and("version").is(expectedVersion);
    if (aggregate instanceof ScopedAggregate scoped)
      filter = filter.and("communityId").is(scoped.communityId());
    Update update = new Update().set("version", aggregate.version()).set("data", data(aggregate));
    indexedFields(aggregate)
        .forEach(
            (key, value) -> {
              if (value == null) update.unset(key);
              else update.set(key, value);
            });
    if (mongo.updateFirst(Query.query(filter), update, collection).getMatchedCount() != 1)
      throw RuleViolation.conflict("The resource changed; reload before retrying.");
  }
}
