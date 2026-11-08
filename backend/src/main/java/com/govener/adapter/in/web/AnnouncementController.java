package com.govener.adapter.in.web;

import static com.govener.adapter.in.web.Views.map;

import com.govener.adapter.in.web.dto.*;
import com.govener.communication.application.AnnouncementService;
import com.govener.community.application.*;
import com.govener.finance.application.*;
import com.govener.identity.adapter.in.security.CurrentActor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class AnnouncementController {
  private final AnnouncementService announcements;
  private final CurrentActor current;
  private final ViewMapper views;
  private final PageResponder pages;

  public AnnouncementController(
      AnnouncementService announcements,
      CurrentActor current,
      ViewMapper views,
      PageResponder pages) {
    this.announcements = announcements;
    this.current = current;
    this.views = views;
    this.pages = pages;
  }

  @GetMapping("/communities/{communityId}/announcements")
  public Object listAnnouncements(
      @PathVariable String communityId,
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
      @RequestParam(required = false) @Pattern(regexp = "DRAFT|PUBLISHED") String status,
      @RequestParam(required = false) String q) {
    return pages.page(
        announcements.list(current.id(), communityId, status, q),
        communityId,
        "listAnnouncements",
        map("status", status, "q", q),
        cursor,
        limit,
        views::announcement);
  }

  @PostMapping("/communities/{communityId}/announcements")
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  public Object createAnnouncement(
      @PathVariable String communityId, @Valid @RequestBody AnnouncementWrite body) {
    return views.announcement(
        announcements.create(
            current.actor(),
            communityId,
            new AnnouncementService.Write(
                body.title(),
                body.body(),
                body.category(),
                new com.govener.communication.domain.Audience(
                    com.govener.communication.domain.Audience.Type.valueOf(body.audience().type()),
                    body.audience().ids()),
                body.status())));
  }

  @GetMapping("/communities/{communityId}/announcements/{announcementId}")
  public Object getAnnouncement(
      @PathVariable String communityId, @PathVariable String announcementId) {
    return views.announcement(announcements.get(current.id(), communityId, announcementId));
  }

  @PutMapping("/communities/{communityId}/announcements/{announcementId}")
  public Object updateAnnouncement(
      @PathVariable String communityId,
      @PathVariable String announcementId,
      @Valid @RequestBody AnnouncementUpdate body) {
    return views.announcement(
        announcements.update(
            current.actor(),
            communityId,
            announcementId,
            new AnnouncementService.Write(
                body.title(),
                body.body(),
                body.category(),
                new com.govener.communication.domain.Audience(
                    com.govener.communication.domain.Audience.Type.valueOf(body.audience().type()),
                    body.audience().ids()),
                body.status()),
            body.expectedVersion()));
  }
}
