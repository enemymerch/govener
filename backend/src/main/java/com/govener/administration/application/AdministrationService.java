package com.govener.administration.application;

import com.govener.administration.domain.*;
import com.govener.community.application.CommunityPorts;
import com.govener.community.domain.*;
import com.govener.finance.application.FinancePorts;
import com.govener.finance.domain.FinancialAccount;
import com.govener.identity.application.IdentityPorts;
import com.govener.identity.domain.*;
import com.govener.shared.application.*;
import com.govener.shared.domain.*;
import java.time.*;
import java.util.*;

public final class AdministrationService {
  public static final Set<Permission> RESIDENT_PERMISSIONS =
      Set.of(
          Permission.COMMUNITY_READ,
          Permission.UNIT_READ,
          Permission.DIRECTORY_READ,
          Permission.FINANCE_READ);
  private final AdminAccess access;
  private final AdministrationPorts.Authorities authorities;
  private final CommunityPorts.Communities communities;
  private final CommunityPorts.Units units;
  private final CommunityPorts.People people;
  private final CommunityPorts.Memberships memberships;
  private final CommunityPorts.Relationships relationships;
  private final CommunityPorts.Residences residences;
  private final IdentityPorts.Users users;
  private final IdentityPorts.Passwords passwords;
  private final FinancePorts.Accounts accounts;
  private final TransactionPort transactions;
  private final AuditPort audit;
  private final Clock clock;

  public AdministrationService(
      AdminAccess access,
      AdministrationPorts.Authorities authorities,
      CommunityPorts.Communities communities,
      CommunityPorts.Units units,
      CommunityPorts.People people,
      CommunityPorts.Memberships memberships,
      CommunityPorts.Relationships relationships,
      CommunityPorts.Residences residences,
      IdentityPorts.Users users,
      IdentityPorts.Passwords passwords,
      FinancePorts.Accounts accounts,
      TransactionPort transactions,
      AuditPort audit,
      Clock clock) {
    this.access = access;
    this.authorities = authorities;
    this.communities = communities;
    this.units = units;
    this.people = people;
    this.memberships = memberships;
    this.relationships = relationships;
    this.residences = residences;
    this.users = users;
    this.passwords = passwords;
    this.accounts = accounts;
    this.transactions = transactions;
    this.audit = audit;
    this.clock = clock;
  }

  public record CommunityView(String id, String name, String address, long version) {}

  public record UserView(
      String id,
      String name,
      String loginEmail,
      String email,
      String phone,
      Set<Role> roles,
      Set<Permission> permissions,
      List<UnitAccess> unitAccess,
      long version,
      long membershipVersion) {}

  public record AdministratorView(
      String id,
      String name,
      String loginEmail,
      String phone,
      Set<String> communityIds,
      long version) {}

  public List<CommunityView> communities(String actorId) {
    var authority = access.authority(actorId);
    return communities.all().stream()
        .filter(item -> authority.allows(item.id()))
        .map(this::view)
        .toList();
  }

  public List<Unit> units(String actorId, String communityId, String query) {
    access.community(actorId, communityId);
    String q = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
    return units.inCommunity(communityId).stream()
        .filter(
            unit -> (unit.label() + " " + unit.blockLabel()).toLowerCase(Locale.ROOT).contains(q))
        .toList();
  }

  public List<UserView> users(String actorId, String communityId, String query) {
    access.community(actorId, communityId);
    String q = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
    return memberships.inCommunity(communityId).stream()
        .map(
            member ->
                userView(users.find(member.userId()).orElseThrow(RuleViolation::missing), member))
        .filter(
            user -> (user.name() + " " + user.loginEmail()).toLowerCase(Locale.ROOT).contains(q))
        .toList();
  }

  public List<AdministratorView> administrators(String actorId) {
    access.requireRoot(actorId);
    return authorities.all().stream()
        .filter(item -> !item.root())
        .map(
            item ->
                administratorView(users.find(item.id()).orElseThrow(RuleViolation::missing), item))
        .toList();
  }

  public CommunityView createCommunity(Actor actor, AdminCommands.CommunityWrite command) {
    access.requireRoot(actor.id());
    return transactions.execute(
        () -> {
          access.requireRoot(actor.id());
          String id = id();
          Community community = new Community(id, command.name(), command.address(), 0);
          communities.insert(community);
          accounts.insert(new FinancialAccount(id(), id, "Bank account", "BANK", 0, 0, 0));
          accounts.insert(new FinancialAccount(id(), id, "Cash account", "CASH", 0, 0, 0));
          audit(actor, id, "COMMUNITY_CREATED", id);
          return view(community);
        });
  }

  public CommunityView updateCommunity(
      Actor actor, String communityId, AdminCommands.CommunityWrite command, long expected) {
    access.requireRoot(actor.id());
    return transactions.execute(
        () -> {
          Community original = access.community(actor.id(), communityId);
          Checks.version(original.version(), expected);
          Community next =
              new Community(
                  original.id(), command.name(), command.address(), original.version() + 1);
          communities.save(next, original.version());
          audit(actor, communityId, "COMMUNITY_UPDATED", communityId);
          return view(next);
        });
  }

  public Unit createUnit(Actor actor, String communityId, AdminCommands.UnitWrite command) {
    access.community(actor.id(), communityId);
    return transactions.execute(
        () -> {
          access.community(actor.id(), communityId);
          Unit unit = unit(id(), communityId, command, 0);
          uniqueUnit(unit, null);
          units.insert(unit);
          audit(actor, communityId, "UNIT_CREATED", unit.id());
          return unit;
        });
  }

  public Unit updateUnit(
      Actor actor,
      String communityId,
      String unitId,
      AdminCommands.UnitWrite command,
      long expected) {
    access.community(actor.id(), communityId);
    return transactions.execute(
        () -> {
          access.community(actor.id(), communityId);
          Unit original =
              units
                  .find(Checks.id(unitId))
                  .filter(unit -> unit.communityId().equals(communityId))
                  .orElseThrow(RuleViolation::missing);
          Checks.version(original.version(), expected);
          Unit next = unit(original.id(), communityId, command, original.version() + 1);
          uniqueUnit(next, unitId);
          units.save(next, original.version());
          audit(actor, communityId, "UNIT_UPDATED", unitId);
          return next;
        });
  }

  public UserView createUser(Actor actor, String communityId, AdminCommands.UserCreate command) {
    access.community(actor.id(), communityId);
    validateUser(command);
    String hash = passwords.encode(command.password());
    return transactions.execute(
        () -> {
          access.community(actor.id(), communityId);
          uniqueEmail(command.loginEmail());
          String userId = id(), personId = id();
          UserAccount user =
              user(userId, personId, command.name(), command.loginEmail(), command.phone(), hash);
          List<UnitAccess> grants = List.of();
          if (command.role() == Role.RESIDENT) {
            Unit unit =
                units
                    .find(Checks.id(command.unitId()))
                    .filter(item -> item.communityId().equals(communityId))
                    .orElseThrow(RuleViolation::missing);
            var interval = new UnitAccess(unit.id(), command.startsOn(), command.endsOn());
            grants = List.of(interval);
            relationships.insert(
                new Relationship(
                    id(),
                    communityId,
                    unit.id(),
                    personId,
                    command.relationshipType(),
                    interval.startsOn(),
                    interval.endsOn(),
                    0));
            if (command.livesHere())
              residences.insert(
                  new Residence(
                      id(),
                      communityId,
                      unit.id(),
                      personId,
                      interval.startsOn(),
                      interval.endsOn(),
                      0));
          }
          users.insert(user);
          people.insert(new Person(personId, communityId, user.name(), 0));
          Membership membership =
              new Membership(
                  id(),
                  communityId,
                  userId,
                  personId,
                  Set.of(command.role()),
                  command.permissions(),
                  grants,
                  0);
          memberships.insert(membership);
          audit(actor, communityId, "USER_CREATED", userId);
          return userView(user, membership);
        });
  }

  public AdministratorView createAdministrator(
      Actor actor, AdminCommands.AdministratorCreate command) {
    access.requireRoot(actor.id());
    password(command.password());
    String hash = passwords.encode(command.password());
    return transactions.execute(
        () -> {
          access.requireRoot(actor.id());
          validateCommunities(command.communityIds());
          uniqueEmail(command.loginEmail());
          String userId = id();
          UserAccount user =
              user(userId, id(), command.name(), command.loginEmail(), command.phone(), hash);
          AdminAuthority authority =
              new AdminAuthority(userId, AdminRole.ADMIN, command.communityIds(), 0);
          users.insert(user);
          authorities.insert(authority);
          for (String cid : authority.communityIds())
            audit(actor, cid, "ADMINISTRATOR_CREATED", userId);
          return administratorView(user, authority);
        });
  }

  public AdministratorView assignAdministrator(
      Actor actor, String userId, Set<String> communityIds, long expected) {
    access.requireRoot(actor.id());
    return transactions.execute(
        () -> {
          access.requireRoot(actor.id());
          validateCommunities(communityIds);
          AdminAuthority original =
              authorities.find(Checks.id(userId)).orElseThrow(RuleViolation::missing);
          AdminAuthority next = original.assign(communityIds, expected);
          authorities.save(next, original.version());
          Set<String> affected = new HashSet<>(original.communityIds());
          affected.addAll(next.communityIds());
          for (String cid : affected)
            audit(actor, cid, "ADMINISTRATOR_COMMUNITIES_CHANGED", userId);
          return administratorView(users.find(userId).orElseThrow(RuleViolation::missing), next);
        });
  }

  /** Called only by explicit deployment bootstrap, never by an HTTP endpoint. */
  public void bootstrapRoot(String name, String loginEmail, String rawPassword) {
    if (authorities.find("root").isPresent()) return;
    password(rawPassword);
    String hash = passwords.encode(rawPassword);
    transactions.execute(
        () -> {
          if (authorities.find("root").isPresent()) return null;
          if (users.find("root").isPresent())
            throw RuleViolation.conflict(
                "The reserved root account already exists without authority.");
          uniqueEmail(loginEmail);
          users.insert(user("root", id(), name, loginEmail, "", hash));
          authorities.insert(new AdminAuthority("root", AdminRole.ROOT, Set.of(), 0));
          return null;
        });
  }

  private void validateUser(AdminCommands.UserCreate command) {
    Checks.required(command.role());
    Checks.required(command.permissions());
    password(command.password());
    if (command.role() == Role.RESIDENT) {
      if (!RESIDENT_PERMISSIONS.equals(command.permissions()))
        throw RuleViolation.invalid("Resident permissions cannot include management capabilities.");
      Checks.id(command.unitId());
      new UnitAccess(command.unitId(), command.startsOn(), command.endsOn());
      if (!("OWNER".equals(command.relationshipType())
          || "TENANT".equals(command.relationshipType())))
        throw RuleViolation.invalid("Select an owner or tenant relationship.");
    } else {
      if (!command.permissions().contains(Permission.COMMUNITY_READ))
        throw RuleViolation.invalid("Managers require community read access.");
      if (command.unitId() != null
          || command.startsOn() != null
          || command.endsOn() != null
          || command.relationshipType() != null
          || command.livesHere())
        throw RuleViolation.invalid(
            "Manager creation does not implicitly create a residence or unit relationship.");
    }
  }

  private void validateCommunities(Set<String> ids) {
    Checks.required(ids);
    if (ids.size() > 1000) throw RuleViolation.invalid("Too many communities.");
    for (String id : ids) communities.find(Checks.id(id)).orElseThrow(RuleViolation::missing);
  }

  private void uniqueEmail(String email) {
    if (users.byLoginEmail(UserAccount.normalizeEmail(email)).isPresent())
      throw new RuleViolation(
          RuleViolation.Kind.CONFLICT,
          "EMAIL_ALREADY_REGISTERED",
          "This login email cannot be registered again.");
  }

  private void uniqueUnit(Unit unit, String exclude) {
    if (units.inCommunity(unit.communityId()).stream()
        .anyMatch(
            item ->
                !item.id().equals(exclude)
                    && item.blockId().equals(unit.blockId())
                    && item.label().equals(unit.label())))
      throw new RuleViolation(
          RuleViolation.Kind.CONFLICT,
          "UNIT_ALREADY_EXISTS",
          "This unit label already exists in the selected block.");
  }

  private Unit unit(String id, String cid, AdminCommands.UnitWrite value, long version) {
    return new Unit(
        id,
        cid,
        value.label(),
        value.blockId(),
        value.blockLabel(),
        value.floor(),
        value.kind(),
        value.areaSqm(),
        version);
  }

  private UserAccount user(
      String id, String personId, String name, String email, String phone, String hash) {
    return new UserAccount(
        id, personId, email, hash, name, email, phone, Preferences.defaults(), 0, 0);
  }

  private CommunityView view(Community value) {
    return new CommunityView(value.id(), value.name(), value.address(), value.version());
  }

  private UserView userView(UserAccount user, Membership membership) {
    return new UserView(
        user.id(),
        user.name(),
        user.loginEmail(),
        user.email(),
        user.phone(),
        membership.roles(),
        membership.permissions(),
        membership.units(),
        user.version(),
        membership.version());
  }

  private AdministratorView administratorView(UserAccount user, AdminAuthority authority) {
    return new AdministratorView(
        user.id(),
        user.name(),
        user.loginEmail(),
        user.phone(),
        authority.communityIds(),
        authority.version());
  }

  private static void password(String value) {
    if (value == null || value.length() < 12 || value.length() > 128)
      throw RuleViolation.invalid("Initial passwords must contain 12 to 128 characters.");
  }

  private void audit(Actor actor, String cid, String operation, String resource) {
    audit.append(
        new AuditRecord(id(), cid, actor.id(), operation, resource, "", clock.instant(), 0));
  }

  private static String id() {
    return UUID.randomUUID().toString();
  }
}
