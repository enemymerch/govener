package com.govener.community.adapter.out.mongo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.govener.community.application.CommunityPorts;
import com.govener.community.domain.Community;
import com.govener.shared.adapter.out.mongo.MongoAggregateRepository;
import org.springframework.data.mongodb.core.MongoTemplate;

public final class MongoCommunityRepository extends MongoAggregateRepository<Community>
    implements CommunityPorts.Communities {
  public MongoCommunityRepository(MongoTemplate mongo, ObjectMapper mapper) {
    super(mongo, mapper, "communities", Community.class);
  }
}
