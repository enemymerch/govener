package com.govener.poll.adapter.out.mongo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.govener.poll.application.PollPorts;
import com.govener.poll.domain.Poll;
import com.govener.shared.adapter.out.mongo.MongoAggregateRepository;
import org.springframework.data.mongodb.core.MongoTemplate;

public final class MongoPollRepository extends MongoAggregateRepository<Poll>
    implements PollPorts.Polls {
  public MongoPollRepository(MongoTemplate mongo, ObjectMapper mapper) {
    super(mongo, mapper, "polls", Poll.class);
  }
}
