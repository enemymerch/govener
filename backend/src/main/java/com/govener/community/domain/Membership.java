package com.govener.community.domain;

import com.govener.shared.domain.*;
import java.time.LocalDate;
import java.util.*;

public record Membership(
    String id,
    String communityId,
    String userId,
    String personId,
    Set<Role> roles,
    Set<Permission> permissions,
    List<UnitAccess> units,
    long version)
    implements ScopedAggregate {
  public Membership {
    roles = Set.copyOf(roles);
    permissions = Set.copyOf(permissions);
    units = List.copyOf(units);
  }

  public boolean manager() {
    return roles.contains(Role.MANAGER);
  }

  public boolean ownsAccess(String unitId, LocalDate on) {
    return units.stream().anyMatch(access -> access.unitId().equals(unitId) && access.active(on));
  }

  public void require(Permission permission) {
    if (!permissions.contains(permission)) throw RuleViolation.forbidden();
  }

  public void requireManager(Permission permission) {
    if (!manager()) throw RuleViolation.forbidden();
    require(permission);
  }
}
