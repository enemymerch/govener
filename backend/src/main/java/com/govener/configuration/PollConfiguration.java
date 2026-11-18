package com.govener.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.govener.community.application.*;
import com.govener.identity.application.IdentityPorts;
import com.govener.poll.adapter.out.mongo.MongoPollRepository;
import com.govener.poll.application.*;
import com.govener.shared.application.*;
import java.time.Clock;
import org.springframework.context.annotation.*;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
public class PollConfiguration {
  @Bean
  PollPorts.Polls polls(MongoTemplate mongo, ObjectMapper mapper) {
    return new MongoPollRepository(mongo, mapper);
  }

  @Bean
  PollService pollService(
      PollPorts.Polls polls,
      AccessService access,
      CommunityPorts.Units units,
      CommunityPorts.Relationships relationships,
      CommunityPorts.Residences residences,
      IdentityPorts.Users users,
      TransactionPort transactions,
      AuditPort audit,
      Clock clock) {
    return new PollService(
        polls, access, units, relationships, residences, users, transactions, audit, clock);
  }
}
