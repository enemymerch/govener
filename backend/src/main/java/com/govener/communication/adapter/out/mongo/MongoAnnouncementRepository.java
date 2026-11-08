package com.govener.communication.adapter.out.mongo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.govener.communication.application.AnnouncementPorts;
import com.govener.communication.domain.Announcement;
import com.govener.shared.adapter.out.mongo.MongoAggregateRepository;
import org.springframework.data.mongodb.core.MongoTemplate;

public final class MongoAnnouncementRepository extends MongoAggregateRepository<Announcement>
    implements AnnouncementPorts.Announcements {
  public MongoAnnouncementRepository(MongoTemplate mongo, ObjectMapper mapper) {
    super(mongo, mapper, "announcements", Announcement.class);
  }
}
