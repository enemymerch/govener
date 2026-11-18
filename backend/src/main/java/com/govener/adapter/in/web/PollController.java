package com.govener.adapter.in.web;

import com.govener.adapter.in.web.dto.*;
import com.govener.identity.adapter.in.security.CurrentActor;
import com.govener.poll.application.PollService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class PollController {
  private final PollService service;
  private final CurrentActor current;
  private final PageResponder pages;

  public PollController(PollService service, CurrentActor current, PageResponder pages) {
    this.service = service;
    this.current = current;
    this.pages = pages;
  }

  @GetMapping("/communities/{communityId}/polls")
  public Object list(
      @PathVariable String communityId,
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
    return pages.page(
        service.list(current.id(), communityId),
        communityId,
        "listPolls",
        Views.map(),
        cursor,
        limit,
        poll -> service.view(current.id(), poll));
  }

  @GetMapping("/communities/{communityId}/polls/{pollId}")
  public Object get(@PathVariable String communityId, @PathVariable String pollId) {
    return service.get(current.id(), communityId, pollId);
  }

  @PostMapping("/communities/{communityId}/polls")
  @ResponseStatus(HttpStatus.CREATED)
  public Object create(@PathVariable String communityId, @Valid @RequestBody PollCreate body) {
    return service.create(
        current.actor(),
        communityId,
        new PollService.Write(
            body.title(),
            body.description(),
            body.audience(),
            body.options(),
            body.closesAt(),
            body.status()));
  }

  @PostMapping("/communities/{communityId}/polls/{pollId}/votes")
  public Object vote(
      @PathVariable String communityId,
      @PathVariable String pollId,
      @Valid @RequestBody PollVote body) {
    return service.vote(
        current.actor(),
        communityId,
        pollId,
        body.optionId(),
        body.unitIds(),
        body.expectedVersion());
  }

  @PostMapping("/communities/{communityId}/polls/{pollId}/publication")
  public Object publish(
      @PathVariable String communityId,
      @PathVariable String pollId,
      @Valid @RequestBody VersionCommand body) {
    return service.publish(current.actor(), communityId, pollId, body.expectedVersion());
  }

  @PostMapping("/communities/{communityId}/polls/{pollId}/closure")
  public Object close(
      @PathVariable String communityId,
      @PathVariable String pollId,
      @Valid @RequestBody VersionCommand body) {
    return service.close(current.actor(), communityId, pollId, body.expectedVersion());
  }
}
