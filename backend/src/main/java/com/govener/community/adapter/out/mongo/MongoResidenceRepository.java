package com.govener.community.adapter.out.mongo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.govener.community.application.CommunityPorts;
import com.govener.community.domain.Residence;
import com.govener.shared.adapter.out.mongo.MongoAggregateRepository;
import org.springframework.data.mongodb.core.MongoTemplate;

public final class MongoResidenceRepository extends MongoAggregateRepository<Residence>
    implements CommunityPorts.Residences {
  public MongoResidenceRepository(MongoTemplate mongo, ObjectMapper mapper) {
    super(mongo, mapper, "residences", Residence.class);
  }
}
