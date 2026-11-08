package com.govener.adapter.in.web;

import static com.govener.adapter.in.web.Views.map;

import com.govener.adapter.in.web.dto.*;
import com.govener.community.application.*;
import com.govener.finance.application.*;
import com.govener.identity.adapter.in.security.CurrentActor;
import com.govener.service.application.RequestService;
import com.govener.service.domain.RequestStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class RequestController {
  private final RequestService requests;
  private final CurrentActor current;
  private final ViewMapper views;
  private final PageResponder pages;

  public RequestController(
      RequestService requests, CurrentActor current, ViewMapper views, PageResponder pages) {
    this.requests = requests;
    this.current = current;
    this.views = views;
    this.pages = pages;
  }

  @GetMapping("/communities/{communityId}/requests")
  public Object listRequests(
      @PathVariable String communityId,
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
      @RequestParam(required = false) String unitId,
      @RequestParam(required = false) @Pattern(regexp = "OPEN|IN_PROGRESS|RESOLVED") String status,
      @RequestParam(required = false) String q) {
    return pages.page(
        requests.list(current.id(), communityId, unitId, status, q),
        communityId,
        "listRequests",
        map("unitId", unitId, "status", status, "q", q),
        cursor,
        limit,
        item -> item.createdAt() + ":" + item.id(),
        item -> views.request(current.id(), item));
  }

  @PostMapping("/communities/{communityId}/requests")
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  public Object createRequest(
      @PathVariable String communityId, @Valid @RequestBody RequestCreate body) {
    return views.request(
        current.id(),
        requests.create(
            current.actor(),
            communityId,
            body.unitId(),
            body.title(),
            body.location(),
            body.description(),
            body.photoIds()));
  }

  @GetMapping("/communities/{communityId}/requests/{requestId}")
  public Object getRequest(@PathVariable String communityId, @PathVariable String requestId) {
    return views.request(current.id(), requests.get(current.id(), communityId, requestId));
  }

  @GetMapping("/communities/{communityId}/requests/{requestId}/comments")
  public Object listRequestComments(
      @PathVariable String communityId,
      @PathVariable String requestId,
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
    return pages.page(
        requests.comments(current.id(), communityId, requestId),
        communityId,
        "listRequestComments",
        map("requestId", requestId),
        cursor,
        limit,
        item -> item.createdAt() + ":" + item.id(),
        views::comment);
  }

  @PostMapping("/communities/{communityId}/requests/{requestId}/comments")
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  public Object addRequestComment(
      @PathVariable String communityId,
      @PathVariable String requestId,
      @Valid @RequestBody CommentCreate body) {
    return views.comment(requests.comment(current.actor(), communityId, requestId, body.body()));
  }

  @PostMapping("/communities/{communityId}/requests/{requestId}/assignments")
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  public Object assignRequest(
      @PathVariable String communityId,
      @PathVariable String requestId,
      @Valid @RequestBody RequestAssignment body) {
    return views.request(
        current.id(),
        requests.assign(
            current.actor(), communityId, requestId, body.assigneeId(), body.expectedVersion()));
  }

  @PostMapping("/communities/{communityId}/requests/{requestId}/status-changes")
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  public Object changeRequestStatus(
      @PathVariable String communityId,
      @PathVariable String requestId,
      @Valid @RequestBody RequestStatusChange body) {
    return views.request(
        current.id(),
        requests.changeStatus(
            current.actor(),
            communityId,
            requestId,
            RequestStatus.valueOf(body.status()),
            body.expectedVersion()));
  }

  @GetMapping("/communities/{communityId}/requests/{requestId}/events")
  public Object listRequestEvents(
      @PathVariable String communityId,
      @PathVariable String requestId,
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
    return pages.page(
        requests.events(current.id(), communityId, requestId),
        communityId,
        "listRequestEvents",
        map("requestId", requestId),
        cursor,
        limit,
        item -> String.format(java.util.Locale.ROOT, "%020d:%s", item.version(), item.id()),
        views::event);
  }
}
