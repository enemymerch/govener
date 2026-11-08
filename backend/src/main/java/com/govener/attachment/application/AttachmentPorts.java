package com.govener.attachment.application;

import com.govener.attachment.domain.Attachment;
import com.govener.shared.application.AggregateRepository;

public final class AttachmentPorts {
  private AttachmentPorts() {}

  public interface Metadata extends AggregateRepository<Attachment> {
    boolean deleteOrphan(String id, long expectedVersion);
  }

  public interface Blobs {
    String store(byte[] bytes, String name, String mediaType);

    byte[] read(String id);

    void delete(String id);
  }

  public interface ContentValidator {
    void validate(byte[] bytes, String fileName, String mediaType, String purpose);
  }
}
