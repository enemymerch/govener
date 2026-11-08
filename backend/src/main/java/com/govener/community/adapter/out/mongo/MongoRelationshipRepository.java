package com.govener.community.adapter.out.mongo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.govener.community.application.CommunityPorts;
import com.govener.community.domain.Relationship;
import com.govener.shared.adapter.out.mongo.MongoAggregateRepository;
import org.springframework.data.mongodb.core.MongoTemplate;

public final class MongoRelationshipRepository extends MongoAggregateRepository<Relationship>
    implements CommunityPorts.Relationships {
  public MongoRelationshipRepository(MongoTemplate mongo, ObjectMapper mapper) {
    super(mongo, mapper, "relationships", Relationship.class);
  }
}
