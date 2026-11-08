package com.govener.adapter.in.web;

import static com.govener.adapter.in.web.Views.map;

import com.govener.adapter.in.web.dto.*;
import com.govener.community.application.*;
import com.govener.finance.application.*;
import com.govener.identity.adapter.in.security.CurrentActor;
import com.govener.notification.application.NotificationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class NotificationController {
  private final NotificationService notifications;
  private final CurrentActor current;
  private final ViewMapper views;
  private final PageResponder pages;

  public NotificationController(
      NotificationService notifications,
      CurrentActor current,
      ViewMapper views,
      PageResponder pages) {
    this.notifications = notifications;
    this.current = current;
    this.views = views;
    this.pages = pages;
  }

  @GetMapping("/communities/{communityId}/notifications")
  public Object listNotifications(
      @PathVariable String communityId,
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
      @RequestParam(defaultValue = "false") boolean unread) {
    return pages.page(
        notifications.list(current.id(), communityId, unread),
        communityId,
        "listNotifications",
        map("unread", unread),
        cursor,
        limit,
        views::appNotification);
  }

  @PutMapping("/communities/{communityId}/notifications/{notificationId}/read")
  @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
  public void markNotificationRead(
      @PathVariable String communityId, @PathVariable String notificationId) {
    notifications.read(current.id(), communityId, notificationId);
  }

  @PostMapping("/communities/{communityId}/notifications/read-all")
  @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
  public void markAllNotificationsRead(
      @PathVariable String communityId, @Valid @RequestBody ReadAll body) {
    notifications.readAll(current.id(), communityId, body.before());
  }
}
