package com.govener.service.application;

import com.govener.attachment.application.AttachmentService;
import com.govener.community.application.AccessService;
import com.govener.community.domain.*;
import com.govener.identity.application.IdentityPorts;
import com.govener.notification.application.ActivityPublisher;
import com.govener.service.domain.*;
import com.govener.shared.application.TransactionPort;
import com.govener.shared.domain.*;
import java.time.*;
import java.util.*;

public final class RequestService {
  private final RequestPorts.Requests requests;
  private final RequestPorts.Comments comments;
  private final RequestPorts.Events events;
  private final RequestPorts.Sequences sequences;
  private final AccessService access;
  private final IdentityPorts.Users users;
  private final TransactionPort transactions;
  private final AttachmentService attachments;
  private final ActivityPublisher activity;
  private final Clock clock;

  public RequestService(
      RequestPorts.Requests requests,
      RequestPorts.Comments comments,
      RequestPorts.Events events,
      RequestPorts.Sequences sequences,
      AccessService access,
      IdentityPorts.Users users,
      TransactionPort transactions,
      AttachmentService attachments,
      ActivityPublisher activity,
      Clock clock) {
    this.requests = requests;
    this.comments = comments;
    this.events = events;
    this.sequences = sequences;
    this.access = access;
    this.users = users;
    this.transactions = transactions;
    this.attachments = attachments;
    this.activity = activity;
    this.clock = clock;
  }

  private boolean manages(Membership member) {
    return member.manager() && member.permissions().contains(Permission.REQUEST_MANAGE);
  }

  public List<ServiceRequest> list(
      String userId, String communityId, String unitId, String status, String query) {
    Membership member = access.membership(userId, communityId);
    String q = query == null ? "" : query.toLowerCase(Locale.ROOT);
    return requests.inCommunity(communityId).stream()
        .filter(
            item ->
                item.visibleTo(userId, manages(member))
                    && (unitId == null || unitId.equals(item.unitId()))
                    && (status == null || item.status().name().equals(status))
                    && (item.title() + " " + item.location()).toLowerCase(Locale.ROOT).contains(q))
        .toList();
  }

  public ServiceRequest get(String userId, String communityId, String id) {
    Membership member = access.membership(userId, communityId);
    return requests
        .find(Checks.id(id))
        .filter(
            item ->
                item.communityId().equals(communityId) && item.visibleTo(userId, manages(member)))
        .orElseThrow(RuleViolation::missing);
  }

  public ServiceRequest create(
      Actor actor,
      String communityId,
      String unitId,
      String title,
      String location,
      String description,
      List<String> photos) {
    Membership member = access.membership(actor.id(), communityId);
    if (unitId != null) {
      access.unit(member, unitId);
      if (!member.manager()) access.ownUnit(member, unitId);
    }
    long number = sequences.next("request:" + communityId);
    return transactions.execute(
        () -> {
          String id = UUID.randomUUID().toString();
          ServiceRequest created =
              new ServiceRequest(
                  id,
                  communityId,
                  number,
                  unitId,
                  title,
                  location,
                  description,
                  RequestStatus.OPEN,
                  actor,
                  null,
                  photos,
                  clock.instant(),
                  0);
          for (String photo : photos)
            attachments.bind(actor.id(), communityId, photo, "REQUEST_PHOTO", "REQUEST", id);
          requests.insert(created);
          event(created, actor, "CREATED", null, null);
          activity.emit(
              actor,
              communityId,
              "REQUEST_CREATED",
              id,
              "",
              access.inCommunity(communityId).stream()
                  .filter(this::manages)
                  .map(Membership::userId)
                  .toList(),
              "REQUEST",
              "A new service request needs attention.",
              "REQUEST");
          return created;
        });
  }

  public ServiceRequest assign(
      Actor actor, String communityId, String id, String assigneeId, long expected) {
    Membership member = access.membership(actor.id(), communityId);
    member.requireManager(Permission.REQUEST_MANAGE);
    return transactions.execute(
        () -> {
          ServiceRequest original = get(actor.id(), communityId, id);
          Checks.version(original.version(), expected);
          Actor assignee = null;
          if (assigneeId != null) {
            Membership target = access.membership(assigneeId, communityId);
            target.requireManager(Permission.REQUEST_MANAGE);
            var user = users.find(assigneeId).orElseThrow(RuleViolation::missing);
            assignee = new Actor(user.id(), user.name());
          }
          if (Objects.equals(original.assignee(), assignee)) return original;
          ServiceRequest changed = original.assign(assignee);
          requests.save(changed, original.version());
          event(
              changed,
              actor,
              "ASSIGNEE_CHANGED",
              original.assignee() == null ? null : original.assignee().id(),
              assigneeId);
          activity.emit(
              actor,
              communityId,
              "REQUEST_ASSIGNED",
              id,
              "",
              List.of(original.creator().id()),
              "REQUEST",
              "Your request assignment was updated.",
              "REQUEST");
          return changed;
        });
  }

  public ServiceRequest changeStatus(
      Actor actor, String communityId, String id, RequestStatus status, long expected) {
    access.membership(actor.id(), communityId).requireManager(Permission.REQUEST_MANAGE);
    return transactions.execute(
        () -> {
          ServiceRequest original = get(actor.id(), communityId, id);
          Checks.version(original.version(), expected);
          if (original.status() == status) return original;
          ServiceRequest changed = original.changeStatus(status);
          requests.save(changed, original.version());
          event(changed, actor, "STATUS_CHANGED", original.status().name(), status.name());
          activity.emit(
              actor,
              communityId,
              "REQUEST_STATUS_CHANGED",
              id,
              "",
              List.of(original.creator().id()),
              "REQUEST",
              "Your request status was updated.",
              "REQUEST");
          return changed;
        });
  }

  public RequestComment comment(Actor actor, String communityId, String id, String body) {
    ServiceRequest request = get(actor.id(), communityId, id);
    return transactions.execute(
        () -> {
          RequestComment comment =
              new RequestComment(
                  UUID.randomUUID().toString(), communityId, id, actor, body, clock.instant(), 0);
          comments.insert(comment);
          List<String> targets =
              actor.id().equals(request.creator().id())
                  ? access.inCommunity(communityId).stream()
                      .filter(this::manages)
                      .map(Membership::userId)
                      .toList()
                  : List.of(request.creator().id());
          activity.emit(
              actor,
              communityId,
              "REQUEST_COMMENTED",
              id,
              "",
              targets,
              "REQUEST",
              "There is a new message on a request.",
              "REQUEST");
          return comment;
        });
  }

  public List<RequestComment> comments(String userId, String communityId, String id) {
    get(userId, communityId, id);
    return comments.inCommunity(communityId).stream()
        .filter(item -> item.requestId().equals(id))
        .sorted(Comparator.comparing(RequestComment::createdAt).thenComparing(RequestComment::id))
        .toList();
  }

  public List<RequestEvent> events(String userId, String communityId, String id) {
    get(userId, communityId, id);
    return events.inCommunity(communityId).stream()
        .filter(item -> item.requestId().equals(id))
        .sorted(Comparator.comparingLong(RequestEvent::version).thenComparing(RequestEvent::id))
        .toList();
  }

  private void event(ServiceRequest request, Actor actor, String kind, String from, String to) {
    events.insert(
        new RequestEvent(
            UUID.randomUUID().toString(),
            request.communityId(),
            request.id(),
            kind,
            actor,
            clock.instant(),
            from,
            to,
            request.version()));
  }
}
