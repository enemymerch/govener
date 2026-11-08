package com.govener.attachment.adapter.out.mongo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.govener.attachment.application.AttachmentPorts;
import com.govener.attachment.domain.Attachment;
import com.govener.shared.adapter.out.mongo.MongoAggregateRepository;
import org.springframework.data.mongodb.core.MongoTemplate;

public final class MongoAttachmentRepository extends MongoAggregateRepository<Attachment>
    implements AttachmentPorts.Metadata {
  public MongoAttachmentRepository(MongoTemplate mongo, ObjectMapper mapper) {
    super(mongo, mapper, "attachments", Attachment.class);
  }

  public boolean deleteOrphan(String id, long expected) {
    return mongo
            .remove(
                org.springframework.data.mongodb.core.query.Query.query(
                    org.springframework.data.mongodb.core.query.Criteria.where("_id")
                        .is(id)
                        .and("version")
                        .is(expected)
                        .and("data.parentId")
                        .is(null)),
                collection)
            .getDeletedCount()
        == 1;
  }
}
