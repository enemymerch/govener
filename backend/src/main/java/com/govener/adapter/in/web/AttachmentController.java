package com.govener.adapter.in.web;

import com.govener.adapter.in.web.dto.*;
import com.govener.attachment.application.AttachmentService;
import com.govener.community.application.*;
import com.govener.finance.application.*;
import com.govener.identity.adapter.in.security.CurrentActor;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class AttachmentController {
  private final AttachmentService attachments;
  private final CurrentActor current;
  private final ViewMapper views;
  private final PageResponder pages;

  public AttachmentController(
      AttachmentService attachments, CurrentActor current, ViewMapper views, PageResponder pages) {
    this.attachments = attachments;
    this.current = current;
    this.views = views;
    this.pages = pages;
  }

  @PostMapping("/communities/{communityId}/attachments")
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  public Object uploadAttachment(
      @PathVariable String communityId,
      @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
      @RequestParam("purpose") String purpose)
      throws java.io.IOException {
    return views.attachment(
        attachments.upload(
            current.id(),
            communityId,
            file.getOriginalFilename(),
            file.getContentType(),
            purpose,
            file.getBytes()));
  }

  @GetMapping("/communities/{communityId}/attachments/{attachmentId}")
  public Object getAttachment(@PathVariable String communityId, @PathVariable String attachmentId) {
    return views.attachment(attachments.readable(current.id(), communityId, attachmentId));
  }

  @GetMapping("/communities/{communityId}/attachments/{attachmentId}/content")
  public Object downloadAttachment(
      @PathVariable String communityId, @PathVariable String attachmentId) {
    return download(communityId, attachmentId);
  }

  private Object download(String communityId, String attachmentId) {
    var file = attachments.readable(current.id(), communityId, attachmentId);
    return org.springframework.http.ResponseEntity.ok()
        .contentType(org.springframework.http.MediaType.parseMediaType(file.mediaType()))
        .header("X-Content-Type-Options", "nosniff")
        .header("Cache-Control", "private, no-store")
        .header(
            "Content-Disposition",
            org.springframework.http.ContentDisposition.attachment()
                .filename(file.fileName(), java.nio.charset.StandardCharsets.UTF_8)
                .build()
                .toString())
        .body(attachments.content(current.id(), communityId, attachmentId));
  }
}
