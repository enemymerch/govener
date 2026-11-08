package com.govener.communication.application;

import com.govener.communication.domain.*;
import com.govener.community.application.*;
import com.govener.community.domain.*;
import com.govener.notification.application.ActivityPublisher;
import com.govener.shared.application.TransactionPort;
import com.govener.shared.domain.*;
import java.time.Clock;
import java.util.*;

public final class AnnouncementService {
  private final AnnouncementPorts.Announcements announcements;
  private final AccessService access;
  private final CommunityPorts.Units units;
  private final TransactionPort transactions;
  private final ActivityPublisher activity;
  private final Clock clock;

  public AnnouncementService(
      AnnouncementPorts.Announcements announcements,
      AccessService access,
      CommunityPorts.Units units,
      TransactionPort transactions,
      ActivityPublisher activity,
      Clock clock) {
    this.announcements = announcements;
    this.access = access;
    this.units = units;
    this.transactions = transactions;
    this.activity = activity;
    this.clock = clock;
  }

  private boolean visible(Membership member, Announcement notice) {
    if (member.manager() && member.permissions().contains(Permission.ANNOUNCEMENT_WRITE))
      return true;
    if (!notice.status().equals("PUBLISHED")) return false;
    if (member.manager()) return true;
    return units.inCommunity(member.communityId()).stream()
        .anyMatch(
            unit ->
                member.ownsAccess(unit.id(), access.today())
                    && notice.audience().includes(unit.id(), unit.blockId()));
  }

  public List<Announcement> list(String userId, String communityId, String status, String query) {
    Membership member = access.membership(userId, communityId);
    String q = query == null ? "" : query.toLowerCase(Locale.ROOT);
    return announcements.inCommunity(communityId).stream()
        .filter(
            item ->
                visible(member, item)
                    && (status == null || item.status().equals(status))
                    && (item.title() + " " + item.body()).toLowerCase(Locale.ROOT).contains(q))
        .toList();
  }

  public Announcement get(String userId, String communityId, String id) {
    Membership member = access.membership(userId, communityId);
    return announcements
        .find(Checks.id(id))
        .filter(item -> item.communityId().equals(communityId) && visible(member, item))
        .orElseThrow(RuleViolation::missing);
  }

  public record Write(
      String title, String body, String category, Audience audience, String status) {}

  public Announcement create(Actor actor, String communityId, Write command) {
    access.membership(actor.id(), communityId).requireManager(Permission.ANNOUNCEMENT_WRITE);
    validateAudience(communityId, command.audience());
    return transactions.execute(
        () -> {
          var now = clock.instant();
          Announcement notice =
              new Announcement(
                  UUID.randomUUID().toString(),
                  communityId,
                  command.title(),
                  command.body(),
                  command.category(),
                  command.audience(),
                  command.status(),
                  actor,
                  now,
                  now,
                  command.status().equals("PUBLISHED") ? now : null,
                  0);
          announcements.insert(notice);
          publish(actor, notice);
          return notice;
        });
  }

  public Announcement update(
      Actor actor, String communityId, String id, Write command, long expected) {
    access.membership(actor.id(), communityId).requireManager(Permission.ANNOUNCEMENT_WRITE);
    validateAudience(communityId, command.audience());
    return transactions.execute(
        () -> {
          Announcement original = get(actor.id(), communityId, id);
          Checks.version(original.version(), expected);
          Announcement changed =
              original.update(
                  command.title(),
                  command.body(),
                  command.category(),
                  command.audience(),
                  command.status(),
                  clock.instant());
          announcements.save(changed, original.version());
          publish(actor, changed);
          return changed;
        });
  }

  private void validateAudience(String communityId, Audience audience) {
    var communityUnits = units.inCommunity(communityId);
    Set<String> valid =
        audience.type() == Audience.Type.BLOCKS
            ? new HashSet<>(communityUnits.stream().map(Unit::blockId).toList())
            : new HashSet<>(communityUnits.stream().map(Unit::id).toList());
    if (audience.type() != Audience.Type.ALL && !valid.containsAll(audience.ids()))
      throw RuleViolation.invalid("Audience references a different or missing community resource.");
  }

  private void publish(Actor actor, Announcement notice) {
    List<String> targets =
        notice.status().equals("PUBLISHED")
            ? access.inCommunity(notice.communityId()).stream()
                .filter(member -> visible(member, notice))
                .map(Membership::userId)
                .toList()
            : List.of();
    activity.emit(
        actor,
        notice.communityId(),
        notice.status().equals("PUBLISHED") ? "ANNOUNCEMENT_PUBLISHED" : "ANNOUNCEMENT_DRAFT_SAVED",
        notice.id(),
        "",
        targets,
        "ANNOUNCEMENT",
        "A community announcement is available.",
        "ANNOUNCEMENT");
  }
}
