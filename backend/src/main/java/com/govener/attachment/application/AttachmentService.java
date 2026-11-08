package com.govener.attachment.application;

import com.govener.attachment.domain.Attachment;
import com.govener.community.application.AccessService;
import com.govener.community.domain.*;
import com.govener.shared.domain.*;
import java.time.*;
import java.util.*;

public final class AttachmentService {
  private final AttachmentPorts.Metadata metadata;
  private final AttachmentPorts.Blobs blobs;
  private final AttachmentPorts.ContentValidator validator;
  private final AccessService access;
  private final Clock clock;

  public AttachmentService(
      AttachmentPorts.Metadata metadata,
      AttachmentPorts.Blobs blobs,
      AttachmentPorts.ContentValidator validator,
      AccessService access,
      Clock clock) {
    this.metadata = metadata;
    this.blobs = blobs;
    this.validator = validator;
    this.access = access;
    this.clock = clock;
  }

  public Attachment upload(
      String userId,
      String communityId,
      String name,
      String mediaType,
      String purpose,
      byte[] content) {
    var member = access.membership(userId, communityId);
    if (purpose.equals("EXPENSE_DOCUMENT")) member.requireManager(Permission.EXPENSE_WRITE);
    validator.validate(content, name, mediaType, purpose);
    String blob = blobs.store(content, name, mediaType);
    Attachment file =
        new Attachment(
            UUID.randomUUID().toString(),
            communityId,
            userId,
            name,
            mediaType,
            content.length,
            purpose,
            blob,
            null,
            null,
            clock.instant(),
            clock.instant().plus(Duration.ofHours(24)),
            0);
    try {
      metadata.insert(file);
      return file;
    } catch (RuntimeException failure) {
      blobs.delete(blob);
      throw failure;
    }
  }

  public Attachment owned(String userId, String communityId, String id, String purpose) {
    Attachment file =
        metadata
            .find(Checks.id(id))
            .filter(
                item ->
                    item.communityId().equals(communityId)
                        && item.ownerId().equals(userId)
                        && item.purpose().equals(purpose))
            .orElseThrow(RuleViolation::missing);
    if (file.expiresAt() != null && !file.expiresAt().isAfter(clock.instant()))
      throw RuleViolation.missing();
    return file;
  }

  public void bind(
      String userId,
      String communityId,
      String id,
      String purpose,
      String parentType,
      String parentId) {
    if (id == null) return;
    Attachment file = owned(userId, communityId, id, purpose);
    metadata.save(file.bind(parentType, parentId), file.version());
  }

  public Attachment readable(String userId, String communityId, String id) {
    Membership member = access.membership(userId, communityId);
    Attachment file =
        metadata
            .find(Checks.id(id))
            .filter(item -> item.communityId().equals(communityId))
            .orElseThrow(RuleViolation::missing);
    if (file.expiresAt() != null && !file.expiresAt().isAfter(clock.instant()))
      throw RuleViolation.missing();
    if (file.ownerId().equals(userId)) return file;
    if (file.parentId() == null || !member.manager()) throw RuleViolation.missing();
    Permission permission =
        switch (file.parentType()) {
          case "REQUEST" -> Permission.REQUEST_MANAGE;
          case "EXPENSE" -> Permission.EXPENSE_WRITE;
          default -> Permission.PAYMENT_RECORD;
        };
    member.requireManager(permission);
    return file;
  }

  public byte[] content(String userId, String communityId, String id) {
    return blobs.read(readable(userId, communityId, id).blobId());
  }

  public Attachment metadataForProjection(String id) {
    return id == null ? null : metadata.find(id).orElseThrow(RuleViolation::missing);
  }

  public void cleanOrphans() {
    for (Attachment file : metadata.all())
      if (file.parentId() == null
          && file.expiresAt() != null
          && !file.expiresAt().isAfter(clock.instant())) {
        if (metadata.deleteOrphan(file.id(), file.version())) blobs.delete(file.blobId());
      }
  }
}
