package com.govener.identity.adapter.out.mongo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.govener.identity.application.IdentityPorts;
import com.govener.identity.domain.UserAccount;
import com.govener.shared.adapter.out.mongo.MongoAggregateRepository;
import org.springframework.data.mongodb.core.MongoTemplate;

public final class MongoUserRepository extends MongoAggregateRepository<UserAccount>
    implements IdentityPorts.Users {
  public MongoUserRepository(MongoTemplate mongo, ObjectMapper mapper) {
    super(mongo, mapper, "users", UserAccount.class);
  }

  public java.util.Optional<UserAccount> byLoginEmail(String email) {
    return matching(
            org.springframework.data.mongodb.core.query.Query.query(
                org.springframework.data.mongodb.core.query.Criteria.where("data.loginEmail")
                    .is(email)))
        .stream()
        .findFirst();
  }
}
