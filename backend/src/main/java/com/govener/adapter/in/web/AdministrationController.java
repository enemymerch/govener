package com.govener.adapter.in.web;

import com.govener.administration.application.*;
import com.govener.community.domain.*;
import com.govener.identity.adapter.in.security.CurrentActor;
import com.govener.shared.application.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
public class AdministrationController {
  private final AdministrationService service;
  private final AdminAccess access;
  private final CurrentActor current;
  private final Pagination pagination;
  private final ViewMapper views;

  public AdministrationController(
      AdministrationService service,
      AdminAccess access,
      CurrentActor current,
      Pagination pagination,
      ViewMapper views) {
    this.service = service;
    this.access = access;
    this.current = current;
    this.pagination = pagination;
    this.views = views;
  }

  @GetMapping("/session")
  public Object session() {
    var authority = access.authority(current.id());
    return Views.map(
        "user",
        views.profile(current.user()),
        "role",
        authority.role(),
        "communityIds",
        authority.communityIds(),
        "authorityVersion",
        authority.version());
  }

  private <T> Page<T> page(
      List<T> values, String query, String cursor, int limit, Function<T, String> key) {
    var a = access.authority(current.id());
    return pagination.page(
        values,
        current.id() + ":" + a.role() + ":" + a.version() + ":" + query,
        cursor,
        limit,
        key);
  }

  @GetMapping("/communities")
  public Object communities(
      @RequestParam(required = false) String cursor, @RequestParam(defaultValue = "20") int limit) {
    return page(
        service.communities(current.id()),
        "communities",
        cursor,
        limit,
        AdministrationService.CommunityView::id);
  }

  @PostMapping("/communities")
  @ResponseStatus(HttpStatus.CREATED)
  public Object createCommunity(@Valid @RequestBody CommunityBody body) {
    return service.createCommunity(
        current.actor(), new AdminCommands.CommunityWrite(body.name(), body.address()));
  }

  @PutMapping("/communities/{cid}")
  public Object updateCommunity(
      @PathVariable String cid, @Valid @RequestBody CommunityUpdate body) {
    return service.updateCommunity(
        current.actor(),
        cid,
        new AdminCommands.CommunityWrite(body.name(), body.address()),
        body.expectedVersion());
  }

  @GetMapping("/communities/{cid}/units")
  public Object units(
      @PathVariable String cid,
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "20") int limit,
      @RequestParam(required = false) String q) {
    return page(
        service.units(current.id(), cid, q), "units:" + cid + ":" + q, cursor, limit, Unit::id);
  }

  @PostMapping("/communities/{cid}/units")
  @ResponseStatus(HttpStatus.CREATED)
  public Object createUnit(@PathVariable String cid, @Valid @RequestBody UnitBody body) {
    return service.createUnit(current.actor(), cid, body.command());
  }

  @PutMapping("/communities/{cid}/units/{id}")
  public Object updateUnit(
      @PathVariable String cid, @PathVariable String id, @Valid @RequestBody UnitUpdate body) {
    return service.updateUnit(
        current.actor(),
        cid,
        id,
        new AdminCommands.UnitWrite(
            body.label(),
            body.blockId(),
            body.blockLabel(),
            body.floor(),
            body.kind(),
            body.areaSqm()),
        body.expectedVersion());
  }

  @GetMapping("/communities/{cid}/users")
  public Object users(
      @PathVariable String cid,
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "20") int limit,
      @RequestParam(required = false) String q) {
    return page(
        service.users(current.id(), cid, q),
        "users:" + cid + ":" + q,
        cursor,
        limit,
        AdministrationService.UserView::id);
  }

  @PostMapping("/communities/{cid}/users")
  @ResponseStatus(HttpStatus.CREATED)
  public Object createUser(@PathVariable String cid, @Valid @RequestBody UserBody body) {
    return service.createUser(
        current.actor(),
        cid,
        new AdminCommands.UserCreate(
            body.name(),
            body.loginEmail(),
            body.phone(),
            body.password(),
            body.role(),
            body.permissions(),
            body.unitId(),
            body.relationshipType(),
            body.startsOn(),
            body.endsOn(),
            body.livesHere()));
  }

  @GetMapping("/administrators")
  public Object administrators(
      @RequestParam(required = false) String cursor, @RequestParam(defaultValue = "20") int limit) {
    return page(
        service.administrators(current.id()),
        "administrators",
        cursor,
        limit,
        AdministrationService.AdministratorView::id);
  }

  @PostMapping("/administrators")
  @ResponseStatus(HttpStatus.CREATED)
  public Object createAdministrator(@Valid @RequestBody AdministratorBody body) {
    return service.createAdministrator(
        current.actor(),
        new AdminCommands.AdministratorCreate(
            body.name(), body.loginEmail(), body.phone(), body.password(), body.communityIds()));
  }

  @PutMapping("/administrators/{id}/communities")
  public Object assign(@PathVariable String id, @Valid @RequestBody Assignment body) {
    return service.assignAdministrator(
        current.actor(), id, body.communityIds(), body.expectedVersion());
  }

  public record CommunityBody(
      @NotBlank @Size(max = 200) String name, @NotBlank @Size(max = 1000) String address) {}

  public record CommunityUpdate(
      @NotBlank @Size(max = 200) String name,
      @NotBlank @Size(max = 1000) String address,
      @Min(0) long expectedVersion) {}

  public record UnitBody(
      @NotBlank String label,
      @NotBlank String blockId,
      @NotBlank String blockLabel,
      int floor,
      @NotBlank String kind,
      @PositiveOrZero double areaSqm) {
    AdminCommands.UnitWrite command() {
      return new AdminCommands.UnitWrite(label, blockId, blockLabel, floor, kind, areaSqm);
    }
  }

  public record UnitUpdate(
      @NotBlank String label,
      @NotBlank String blockId,
      @NotBlank String blockLabel,
      int floor,
      @NotBlank String kind,
      @PositiveOrZero double areaSqm,
      @Min(0) long expectedVersion) {}

  public record UserBody(
      @NotBlank String name,
      @NotBlank @Email String loginEmail,
      @NotNull String phone,
      @NotNull @Size(min = 12, max = 128) String password,
      @NotNull Role role,
      @NotNull Set<Permission> permissions,
      String unitId,
      String relationshipType,
      LocalDate startsOn,
      LocalDate endsOn,
      boolean livesHere) {
    @Override
    public String toString() {
      return "UserBody[REDACTED]";
    }
  }

  public record AdministratorBody(
      @NotBlank String name,
      @NotBlank @Email String loginEmail,
      @NotNull String phone,
      @NotNull @Size(min = 12, max = 128) String password,
      @NotNull @Size(max = 1000) Set<String> communityIds) {
    @Override
    public String toString() {
      return "AdministratorBody[REDACTED]";
    }
  }

  public record Assignment(
      @NotNull @Size(max = 1000) Set<String> communityIds, @Min(0) long expectedVersion) {}
}
