package com.govener.community.adapter.out.mongo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.govener.community.application.CommunityPorts;
import com.govener.community.domain.Unit;
import com.govener.shared.adapter.out.mongo.MongoAggregateRepository;
import org.springframework.data.mongodb.core.MongoTemplate;

public final class MongoUnitRepository extends MongoAggregateRepository<Unit>
    implements CommunityPorts.Units {
  public MongoUnitRepository(MongoTemplate mongo, ObjectMapper mapper) {
    super(mongo, mapper, "units", Unit.class);
  }
}
