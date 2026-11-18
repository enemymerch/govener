package com.govener.administration.adapter.out.mongo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.govener.administration.application.AdministrationPorts;
import com.govener.administration.domain.AdminAuthority;
import com.govener.shared.adapter.out.mongo.MongoAggregateRepository;
import org.springframework.data.mongodb.core.MongoTemplate;

public final class MongoAdminAuthorityRepository extends MongoAggregateRepository<AdminAuthority>
    implements AdministrationPorts.Authorities {
  public MongoAdminAuthorityRepository(MongoTemplate mongo, ObjectMapper mapper) {
    super(mongo, mapper, "admin_authorities", AdminAuthority.class);
  }
}
