package com.govener.shared.adapter.out.mongo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.govener.shared.application.AuditPort;
import com.govener.shared.domain.AuditRecord;
import org.springframework.data.mongodb.core.MongoTemplate;

public final class MongoAuditAdapter extends MongoAggregateRepository<AuditRecord>
    implements AuditPort {
  public MongoAuditAdapter(MongoTemplate mongo, ObjectMapper mapper) {
    super(mongo, mapper, "audit_records", AuditRecord.class);
  }

  public void append(AuditRecord event) {
    insert(event);
  }
}
