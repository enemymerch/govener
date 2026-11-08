package com.govener.adapter.in.web;

import static com.govener.adapter.in.web.Views.map;

import com.govener.adapter.in.web.dto.*;
import com.govener.community.application.*;
import com.govener.community.domain.Permission;
import com.govener.finance.application.*;
import com.govener.identity.adapter.in.security.CurrentActor;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class CommunityController {
  private final CommunityQueries communities;
  private final AccessService access;
  private final DashboardQueries dashboard;
  private final CurrentActor current;
  private final ViewMapper views;
  private final PageResponder pages;

  public CommunityController(
      CommunityQueries communities,
      AccessService access,
      DashboardQueries dashboard,
      CurrentActor current,
      ViewMapper views,
      PageResponder pages) {
    this.communities = communities;
    this.access = access;
    this.dashboard = dashboard;
    this.current = current;
    this.views = views;
    this.pages = pages;
  }

  @GetMapping("/communities")
  public Object listCommunities(
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
    return pages.page(
        communities.communities(current.id()),
        null,
        "listCommunities",
        map(),
        cursor,
        limit,
        views::community);
  }

  @GetMapping("/communities/{communityId}/context")
  public Object getContext(@PathVariable String communityId) {
    return context(communityId);
  }

  @GetMapping("/communities/{communityId}/dashboard")
  public Object getDashboard(
      @PathVariable String communityId,
      @RequestParam(required = false) String unitId,
      @RequestParam(required = false) @Pattern(regexp = "^\\d{4}-\\d{2}$") String period) {
    return views.dashboard(dashboard.get(current.id(), communityId, unitId, period));
  }

  @GetMapping("/communities/{communityId}/units")
  public Object listUnits(
      @PathVariable String communityId,
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String blockId) {
    return pages.page(
        communities.units(current.id(), communityId, q, blockId),
        communityId,
        "listUnits",
        map("q", q, "blockId", blockId),
        cursor,
        limit,
        views::unit);
  }

  @GetMapping("/communities/{communityId}/units/{unitId}")
  public Object getUnit(@PathVariable String communityId, @PathVariable String unitId) {
    return views.unit(communities.unit(current.id(), communityId, unitId));
  }

  @GetMapping("/communities/{communityId}/units/{unitId}/relationships")
  public Object listRelationships(
      @PathVariable String communityId,
      @PathVariable String unitId,
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
    return pages.page(
        communities.relationships(current.id(), communityId, unitId),
        communityId,
        "listRelationships",
        map("unitId", unitId),
        cursor,
        limit,
        views::relationship);
  }

  @GetMapping("/communities/{communityId}/directory")
  public Object listDirectory(
      @PathVariable String communityId,
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String blockId,
      @RequestParam(defaultValue = "false") boolean mine) {
    return pages.page(
        communities.directory(current.id(), communityId, q, blockId, mine),
        communityId,
        "listDirectory",
        map("q", q, "blockId", blockId, "mine", mine),
        cursor,
        limit,
        item -> item.unit().id(),
        views::directory);
  }

  @GetMapping("/communities/{communityId}/directory/units/{unitId}")
  public Object getDirectoryUnit(@PathVariable String communityId, @PathVariable String unitId) {
    return views.directory(communities.directoryUnit(current.id(), communityId, unitId));
  }

  @GetMapping("/communities/{communityId}/staff")
  public Object listAssignableStaff(
      @PathVariable String communityId,
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
    return pages.page(
        communities.staff(current.id(), communityId),
        communityId,
        "listAssignableStaff",
        map(),
        cursor,
        limit,
        item -> item.id(),
        views::actor);
  }

  private Object context(String communityId) {
    var member = access.membership(current.id(), communityId);
    member.require(Permission.COMMUNITY_READ);
    return map(
        "community",
        views.community(communities.community(current.id(), communityId)),
        "roles",
        member.roles(),
        "ownUnits",
        communities.ownUnits(current.id(), communityId).stream().map(views::unit).toList(),
        "permissions",
        member.permissions(),
        "profile",
        views.profile(current.user()));
  }
}
