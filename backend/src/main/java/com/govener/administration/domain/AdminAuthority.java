package com.govener.administration.domain;

import com.govener.shared.domain.*;
import java.util.Set;

/** Administrative authority is independent of manager/resident memberships. */
public record AdminAuthority(String id, AdminRole role, Set<String> communityIds, long version)
    implements Aggregate {
  public AdminAuthority {
    id = Checks.id(id);
    Checks.required(role);
    communityIds = Set.copyOf(communityIds);
    communityIds.forEach(Checks::id);
    if (role == AdminRole.ROOT && (!id.equals("root") || !communityIds.isEmpty())
        || role == AdminRole.ADMIN && id.equals("root")
        || communityIds.size() > 1000)
      throw RuleViolation.invalid("Invalid administrative authority.");
  }

  public boolean root() {
    return role == AdminRole.ROOT;
  }

  public boolean allows(String communityId) {
    return root() || communityIds.contains(communityId);
  }

  public AdminAuthority assign(Set<String> ids, long expected) {
    if (root()) throw RuleViolation.forbidden();
    Checks.version(version, expected);
    return new AdminAuthority(id, role, ids, version + 1);
  }
}
