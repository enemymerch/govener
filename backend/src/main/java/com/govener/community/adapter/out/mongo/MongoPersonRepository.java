package com.govener.community.adapter.out.mongo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.govener.community.application.CommunityPorts;
import com.govener.community.domain.Person;
import com.govener.shared.adapter.out.mongo.MongoAggregateRepository;
import org.springframework.data.mongodb.core.MongoTemplate;

public final class MongoPersonRepository extends MongoAggregateRepository<Person>
    implements CommunityPorts.People {
  public MongoPersonRepository(MongoTemplate mongo, ObjectMapper mapper) {
    super(mongo, mapper, "people", Person.class);
  }
}
