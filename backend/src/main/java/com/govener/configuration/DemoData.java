package com.govener.configuration;

import com.govener.community.application.CommunityPorts;
import com.govener.community.domain.*;
import com.govener.finance.application.FinancePorts;
import com.govener.finance.domain.FinancialAccount;
import com.govener.identity.application.IdentityPorts;
import com.govener.identity.domain.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Explicit development fixtures. This component is excluded from production. */
@Component
@Profile({"dev", "test"})
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DemoData implements ApplicationRunner {
  public static final String PASSWORD = "DemoPass123!";
  private final CommunityPorts.Communities communities;
  private final CommunityPorts.Units units;
  private final CommunityPorts.People people;
  private final CommunityPorts.Memberships memberships;
  private final CommunityPorts.Relationships relationships;
  private final CommunityPorts.Residences residences;
  private final IdentityPorts.Users users;
  private final IdentityPorts.Passwords passwords;
  private final FinancePorts.Accounts accounts;

  public DemoData(
      CommunityPorts.Communities communities,
      CommunityPorts.Units units,
      CommunityPorts.People people,
      CommunityPorts.Memberships memberships,
      CommunityPorts.Relationships relationships,
      CommunityPorts.Residences residences,
      IdentityPorts.Users users,
      IdentityPorts.Passwords passwords,
      FinancePorts.Accounts accounts) {
    this.communities = communities;
    this.units = units;
    this.people = people;
    this.memberships = memberships;
    this.relationships = relationships;
    this.residences = residences;
    this.users = users;
    this.passwords = passwords;
    this.accounts = accounts;
  }

  @Override
  public void run(ApplicationArguments args) {
    seed();
  }

  public void seed() {
    if (communities.find("olive-gardens").isPresent()) return;
    communities.insert(new Community("olive-gardens", "Olive Gardens", "Istanbul", 0));
    communities.insert(new Community("bosphorus-homes", "Bosphorus Homes", "Istanbul", 0));
    units.insert(
        new Unit("a12", "olive-gardens", "A-12", "block-a", "Block A", 3, "APARTMENT", 110, 0));
    units.insert(
        new Unit("a03", "olive-gardens", "A-03", "block-a", "Block A", 1, "APARTMENT", 95, 0));
    units.insert(
        new Unit("b06", "bosphorus-homes", "B-06", "block-b", "Block B", 2, "APARTMENT", 100, 0));
    String hash = passwords.encode(PASSWORD);
    user("manager", "Olive Manager", hash);
    user("resident", "Deniz Resident", hash);
    user("neighbor", "Ece Neighbor", hash);
    user("outsider", "Bosphorus Resident", hash);
    user("staff", "Maintenance Manager", hash);
    var residentPermissions =
        Set.of(
            Permission.COMMUNITY_READ,
            Permission.UNIT_READ,
            Permission.DIRECTORY_READ,
            Permission.FINANCE_READ);
    membership(
        "manager",
        "olive-gardens",
        Set.of(Role.MANAGER),
        EnumSet.allOf(Permission.class),
        List.of());
    membership(
        "staff",
        "olive-gardens",
        Set.of(Role.MANAGER),
        Set.of(
            Permission.COMMUNITY_READ,
            Permission.UNIT_READ,
            Permission.DIRECTORY_READ,
            Permission.REQUEST_MANAGE),
        List.of());
    var start = LocalDate.of(2026, 11, 1);
    membership(
        "resident",
        "olive-gardens",
        Set.of(Role.RESIDENT),
        residentPermissions,
        List.of(new UnitAccess("a12", start, null)));
    membership(
        "neighbor",
        "olive-gardens",
        Set.of(Role.RESIDENT),
        residentPermissions,
        List.of(new UnitAccess("a03", start, null)));
    membership(
        "outsider",
        "bosphorus-homes",
        Set.of(Role.RESIDENT),
        residentPermissions,
        List.of(new UnitAccess("b06", start, null)));
    people.insert(new Person("family", "olive-gardens", "Household Member", 0));
    relationships.insert(
        new Relationship(
            "resident-tenancy",
            "olive-gardens",
            "a12",
            "resident-person",
            "TENANT",
            start,
            null,
            0));
    residences.insert(
        new Residence("resident-home", "olive-gardens", "a12", "resident-person", start, null, 0));
    residences.insert(
        new Residence("family-home", "olive-gardens", "a12", "family", start, null, 0));
    residences.insert(
        new Residence("neighbor-home", "olive-gardens", "a03", "neighbor-person", start, null, 0));
    accounts.insert(
        new FinancialAccount("olive-bank", "olive-gardens", "Community Bank", "BANK", 0, 0, 0));
    accounts.insert(
        new FinancialAccount(
            "bosphorus-bank", "bosphorus-homes", "Community Bank", "BANK", 0, 0, 0));
  }

  private void user(String id, String name, String hash) {
    users.insert(
        new UserAccount(
            id,
            id + "-person",
            id + "@govener.test",
            hash,
            name,
            id + "@govener.test",
            "",
            new Preferences("tr", true, true, false),
            0,
            0));
  }

  private void membership(
      String user,
      String community,
      Set<Role> roles,
      Set<Permission> permissions,
      List<UnitAccess> access) {
    people.insert(
        new Person(user + "-person", community, users.find(user).orElseThrow().name(), 0));
    memberships.insert(
        new Membership(
            user + "-membership",
            community,
            user,
            user + "-person",
            roles,
            permissions,
            access,
            0));
  }
}
