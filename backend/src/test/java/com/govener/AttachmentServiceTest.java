package com.govener;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.govener.attachment.application.*;
import com.govener.attachment.domain.Attachment;
import com.govener.community.application.AccessService;
import com.govener.community.domain.*;
import com.govener.shared.domain.RuleViolation;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class AttachmentServiceTest {
  private final AttachmentPorts.Metadata metadata = mock(AttachmentPorts.Metadata.class);
  private final AttachmentPorts.Blobs blobs = mock(AttachmentPorts.Blobs.class);
  private final AttachmentPorts.ContentValidator validator =
      mock(AttachmentPorts.ContentValidator.class);
  private final AccessService access = mock(AccessService.class);
  private final Clock clock = Clock.fixed(Instant.parse("2026-11-08T09:00:00Z"), ZoneOffset.UTC);

  private AttachmentService service() {
    return new AttachmentService(metadata, blobs, validator, access, clock);
  }

  private Attachment orphan() {
    return new Attachment(
        "file",
        "site",
        "owner",
        "receipt.pdf",
        "application/pdf",
        20,
        "PAYMENT_RECEIPT",
        "blob",
        null,
        null,
        clock.instant().minusSeconds(86400),
        clock.instant(),
        0);
  }

  @Test
  void expiredUnboundFileCannotBeReadEvenByOwner() {
    when(access.membership("owner", "site"))
        .thenReturn(
            new Membership(
                "m", "site", "owner", "p", Set.of(Role.RESIDENT), Set.of(), List.of(), 0));
    when(metadata.find("file")).thenReturn(Optional.of(orphan()));
    assertThatThrownBy(() -> service().readable("owner", "site", "file"))
        .isInstanceOf(RuleViolation.class);
  }

  @Test
  void orphanCleanupDeletesContentOnlyAfterSuccessfulCompareAndSwap() {
    when(metadata.all()).thenReturn(List.of(orphan()));
    when(metadata.deleteOrphan("file", 0)).thenReturn(true);
    service().cleanOrphans();
    var order = inOrder(metadata, blobs);
    order.verify(metadata).deleteOrphan("file", 0);
    order.verify(blobs).delete("blob");
  }

  @Test
  void concurrentBindingPreventsCleanupFromDeletingContent() {
    when(metadata.all()).thenReturn(List.of(orphan()));
    when(metadata.deleteOrphan("file", 0)).thenReturn(false);
    service().cleanOrphans();
    verifyNoInteractions(blobs);
  }

  @Test
  void boundFilesAreNotOrphans() {
    when(metadata.all()).thenReturn(List.of(orphan().bind("PAYMENT_NOTIFICATION", "notice")));
    service().cleanOrphans();
    verify(metadata, never()).deleteOrphan(anyString(), anyLong());
    verifyNoInteractions(blobs);
  }

  @Test
  void failedMetadataInsertCompensatesBlobUpload() {
    when(blobs.store(any(), anyString(), anyString())).thenReturn("blob");
    doThrow(new IllegalStateException("database unavailable")).when(metadata).insert(any());
    assertThatThrownBy(
            () ->
                service()
                    .upload(
                        "owner",
                        "site",
                        "receipt.pdf",
                        "application/pdf",
                        "PAYMENT_RECEIPT",
                        new byte[] {1}))
        .isInstanceOf(IllegalStateException.class);
    verify(blobs).delete("blob");
  }

  @Test
  void boundAttachmentCannotBeReused() {
    assertThatThrownBy(() -> orphan().bind("REQUEST", "request").bind("REQUEST", "other"))
        .isInstanceOf(RuleViolation.class);
  }
}
