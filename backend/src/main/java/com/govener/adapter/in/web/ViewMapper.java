package com.govener.adapter.in.web;

import static com.govener.adapter.in.web.Views.map;

import com.govener.attachment.application.AttachmentService;
import com.govener.communication.domain.Announcement;
import com.govener.community.application.*;
import com.govener.community.domain.*;
import com.govener.finance.application.FinanceQueries;
import com.govener.finance.domain.*;
import com.govener.identity.domain.*;
import com.govener.notification.domain.AppNotification;
import com.govener.service.application.RequestService;
import com.govener.service.domain.*;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public final class ViewMapper {
  private final CommunityQueries communities;
  private final AttachmentService attachments;
  private final RequestService requests;

  public ViewMapper(
      CommunityQueries communities, AttachmentService attachments, RequestService requests) {
    this.communities = communities;
    this.attachments = attachments;
    this.requests = requests;
  }

  public Map<String, Object> profile(UserAccount user) {
    return map(
        "id",
        user.id(),
        "name",
        user.name(),
        "email",
        user.email(),
        "phone",
        user.phone(),
        "version",
        user.version());
  }

  public Map<String, Object> preferences(UserAccount user) {
    var preferences = user.preferences();
    return map(
        "language",
        preferences.language(),
        "inApp",
        preferences.inApp(),
        "push",
        preferences.push(),
        "emailNotifications",
        preferences.emailNotifications(),
        "version",
        user.version());
  }

  public Map<String, Object> community(Community value) {
    return map(
        "id",
        value.id(),
        "name",
        value.name(),
        "address",
        value.address(),
        "timeZone",
        "Europe/Istanbul",
        "currency",
        "TRY");
  }

  public Map<String, Object> unit(Unit value) {
    return map(
        "id",
        value.id(),
        "communityId",
        value.communityId(),
        "label",
        value.label(),
        "blockId",
        value.blockId(),
        "blockLabel",
        value.blockLabel(),
        "floor",
        value.floor(),
        "kind",
        value.kind(),
        "areaSqm",
        value.areaSqm(),
        "version",
        value.version());
  }

  public Map<String, Object> person(Person value) {
    return map("id", value.id(), "name", value.name());
  }

  public Map<String, Object> actor(com.govener.shared.domain.Actor value) {
    return map("id", value.id(), "name", value.name());
  }

  public Map<String, Object> relationship(Relationship value) {
    return map(
        "id",
        value.id(),
        "unitId",
        value.unitId(),
        "person",
        person(communities.person(value.communityId(), value.personId())),
        "type",
        value.type(),
        "startsOn",
        value.startsOn(),
        "endsOn",
        value.endsOn());
  }

  public Map<String, Object> directory(CommunityQueries.DirectoryUnit value) {
    return map(
        "id",
        value.unit().id(),
        "label",
        value.unit().label(),
        "blockId",
        value.unit().blockId(),
        "blockLabel",
        value.unit().blockLabel(),
        "isMyUnit",
        value.isMyUnit(),
        "residents",
        value.residents().stream().map(this::person).toList());
  }

  public Map<String, Object> charge(Charge value) {
    return map(
        "id",
        value.id(),
        "unitId",
        value.unitId(),
        "description",
        value.description(),
        "periodStart",
        value.period().start(),
        "periodEnd",
        value.period().end(),
        "issuedOn",
        value.issuedOn(),
        "dueOn",
        value.dueOn(),
        "amountMinor",
        value.amount().minor(),
        "currency",
        "TRY",
        "allocatedMinor",
        value.allocatedMinor(),
        "outstandingMinor",
        value.outstandingMinor(),
        "status",
        value.status(),
        "cancelledAt",
        value.cancelledAt(),
        "cancellationReason",
        value.cancellationReason(),
        "version",
        value.version());
  }

  public Map<String, Object> batch(ChargeBatch value) {
    return map(
        "id",
        value.id(),
        "status",
        "COMPLETED",
        "charges",
        value.charges().stream().map(this::charge).toList(),
        "createdAt",
        value.createdAt());
  }

  public Map<String, Object> payment(Payment value) {
    return map(
        "id",
        value.id(),
        "unitId",
        value.unitId(),
        "accountId",
        value.accountId(),
        "amountMinor",
        value.amount().minor(),
        "currency",
        "TRY",
        "paidOn",
        value.paidOn(),
        "reference",
        value.reference(),
        "allocations",
        value.allocations().stream()
            .map(item -> map("chargeId", item.chargeId(), "amountMinor", item.amountMinor()))
            .toList(),
        "unallocatedMinor",
        value.unallocatedMinor(),
        "notificationId",
        value.notificationId(),
        "status",
        value.reversedAt() == null ? "RECORDED" : "REVERSED",
        "reversedAt",
        value.reversedAt(),
        "reversalReason",
        value.reversalReason(),
        "version",
        value.version());
  }

  public Map<String, Object> account(FinanceQueries.AccountProjection value) {
    return map(
        "unitId",
        value.unitId(),
        "currency",
        "TRY",
        "chargedMinor",
        value.chargedMinor(),
        "paidMinor",
        value.paidMinor(),
        "outstandingMinor",
        value.outstandingMinor(),
        "creditMinor",
        value.creditMinor(),
        "version",
        value.version());
  }

  public Map<String, Object> entry(AccountEntry value) {
    return map(
        "id",
        value.id(),
        "unitId",
        value.unitId(),
        "type",
        value.type(),
        "amountMinor",
        value.amountMinor(),
        "occurredAt",
        value.occurredAt(),
        "description",
        value.description(),
        "sourceId",
        value.sourceId(),
        "balanceAfterMinor",
        value.balanceAfterMinor());
  }

  public Map<String, Object> cash(FinancialAccount value) {
    return map(
        "id",
        value.id(),
        "name",
        value.name(),
        "kind",
        value.kind(),
        "currency",
        "TRY",
        "openingMinor",
        value.openingMinor(),
        "balanceMinor",
        value.balanceMinor(),
        "version",
        value.version());
  }

  public Map<String, Object> attachment(com.govener.attachment.domain.Attachment value) {
    return map(
        "id",
        value.id(),
        "fileName",
        value.fileName(),
        "mediaType",
        value.mediaType(),
        "sizeBytes",
        value.sizeBytes(),
        "purpose",
        value.purpose(),
        "contentUrl",
        "/api/v1/communities/" + value.communityId() + "/attachments/" + value.id() + "/content",
        "createdAt",
        value.createdAt());
  }

  public Map<String, Object> notification(PaymentNotification value) {
    var response =
        map(
            "id",
            value.id(),
            "unitId",
            value.unitId(),
            "amountMinor",
            value.amount().minor(),
            "currency",
            "TRY",
            "paymentDate",
            value.paymentDate(),
            "reference",
            value.reference(),
            "note",
            value.note(),
            "status",
            value.status(),
            "reviewNote",
            value.reviewNote(),
            "paymentId",
            value.paymentId(),
            "version",
            value.version());
    if (value.receiptId() != null)
      response.put("receipt", attachment(attachments.metadataForProjection(value.receiptId())));
    return response;
  }

  public Map<String, Object> expense(Expense value) {
    var response =
        map(
            "id",
            value.id(),
            "description",
            value.description(),
            "category",
            value.category(),
            "vendor",
            value.vendor(),
            "incurredOn",
            value.incurredOn(),
            "amountMinor",
            value.amount().minor(),
            "currency",
            "TRY",
            "paidMinor",
            value.paidMinor(),
            "version",
            value.version());
    if (value.documentId() != null)
      response.put("document", attachment(attachments.metadataForProjection(value.documentId())));
    return response;
  }

  public Map<String, Object> expensePayment(ExpensePayment value) {
    return map(
        "id",
        value.id(),
        "expenseId",
        value.expenseId(),
        "accountId",
        value.accountId(),
        "paidOn",
        value.paidOn(),
        "amountMinor",
        value.amount().minor(),
        "status",
        value.reversedAt() == null ? "RECORDED" : "REVERSED",
        "reversalReason",
        value.reversalReason(),
        "version",
        value.version());
  }

  public Map<String, Object> comment(RequestComment value) {
    return map(
        "id",
        value.id(),
        "author",
        actor(value.author()),
        "body",
        value.body(),
        "createdAt",
        value.createdAt());
  }

  public Map<String, Object> event(RequestEvent value) {
    var response =
        map(
            "id",
            value.id(),
            "kind",
            value.kind(),
            "actor",
            actor(value.actor()),
            "createdAt",
            value.createdAt());
    if (value.from() != null) response.put("from", value.from());
    if (value.to() != null) response.put("to", value.to());
    return response;
  }

  public Map<String, Object> request(String viewerId, ServiceRequest value) {
    var response =
        map(
            "id",
            value.id(),
            "displayNumber",
            value.displayNumber(),
            "communityId",
            value.communityId(),
            "unitId",
            value.unitId(),
            "title",
            value.title(),
            "location",
            value.location(),
            "description",
            value.description(),
            "status",
            value.status(),
            "creator",
            actor(value.creator()),
            "photos",
            value.photoIds().stream()
                .map(id -> attachment(attachments.metadataForProjection(id)))
                .toList(),
            "comments",
            requests.comments(viewerId, value.communityId(), value.id()).stream()
                .limit(50)
                .map(this::comment)
                .toList(),
            "history",
            requests.events(viewerId, value.communityId(), value.id()).stream()
                .limit(50)
                .map(this::event)
                .toList(),
            "createdAt",
            value.createdAt(),
            "version",
            value.version());
    if (value.assignee() != null) response.put("assignee", actor(value.assignee()));
    return response;
  }

  public Map<String, Object> announcement(Announcement value) {
    return map(
        "id",
        value.id(),
        "title",
        value.title(),
        "body",
        value.body(),
        "category",
        value.category(),
        "audience",
        map("type", value.audience().type(), "ids", value.audience().ids()),
        "status",
        value.status(),
        "author",
        actor(value.author()),
        "createdAt",
        value.createdAt(),
        "updatedAt",
        value.updatedAt(),
        "publishedAt",
        value.publishedAt(),
        "version",
        value.version());
  }

  public Map<String, Object> appNotification(AppNotification value) {
    return map(
        "id",
        value.id(),
        "type",
        value.type(),
        "title",
        value.title(),
        "body",
        value.body(),
        "target",
        map("kind", value.targetKind(), "id", value.targetId()),
        "createdAt",
        value.createdAt(),
        "readAt",
        value.readAt());
  }

  public Map<String, Object> dashboard(
      com.govener.community.application.DashboardQueries.Snapshot value) {
    var response =
        map(
            "communityId",
            value.communityId(),
            "currency",
            "TRY",
            "period",
            value.period(),
            "nextDueOn",
            value.nextDueOn(),
            "collectedMinor",
            value.collectedMinor(),
            "outstandingMinor",
            value.outstandingMinor(),
            "targetMinor",
            value.targetMinor(),
            "paidUnitCount",
            value.paidUnitCount(),
            "unitCount",
            value.unitCount(),
            "openRequestCount",
            value.openRequestCount());
    if (value.ownAccount() != null) response.put("ownAccount", account(value.ownAccount()));
    return response;
  }
}
