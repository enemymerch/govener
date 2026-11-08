package com.govener.attachment.domain;

import com.govener.shared.domain.*;
import java.time.Instant;

public record Attachment(
    String id,
    String communityId,
    String ownerId,
    String fileName,
    String mediaType,
    long sizeBytes,
    String purpose,
    String blobId,
    String parentType,
    String parentId,
    Instant createdAt,
    Instant expiresAt,
    long version)
    implements ScopedAggregate {
  public Attachment {
    Checks.id(id);
    Checks.id(communityId);
    Checks.id(ownerId);
    fileName = Checks.text(fileName, 180);
    if (fileName.contains("/")
        || fileName.contains("\\")
        || fileName.chars().anyMatch(c -> c < 32 || c == 127))
      throw RuleViolation.invalid("Invalid filename.");
    if (sizeBytes <= 0 || sizeBytes > 5L * 1024 * 1024)
      throw RuleViolation.invalid("Attachment size exceeds the limit.");
  }

  public Attachment bind(String type, String parent) {
    if (parentId != null) throw RuleViolation.conflict("Attachment already belongs to a resource.");
    return new Attachment(
        id,
        communityId,
        ownerId,
        fileName,
        mediaType,
        sizeBytes,
        purpose,
        blobId,
        type,
        parent,
        createdAt,
        null,
        version + 1);
  }
}
