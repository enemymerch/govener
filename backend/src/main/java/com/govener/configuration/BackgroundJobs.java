package com.govener.configuration;

import com.govener.attachment.application.AttachmentService;
import com.govener.notification.application.OutboxProjector;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.jobs.enabled", havingValue = "true", matchIfMissing = true)
public final class BackgroundJobs {
  private final OutboxProjector projector;
  private final AttachmentService attachments;

  public BackgroundJobs(OutboxProjector projector, AttachmentService attachments) {
    this.projector = projector;
    this.attachments = attachments;
  }

  @Scheduled(fixedDelayString = "${app.jobs.outbox-delay:1000}")
  public void project() {
    projector.process();
  }

  @Scheduled(fixedDelayString = "${app.jobs.cleanup-delay:60000}")
  public void clean() {
    attachments.cleanOrphans();
  }
}
