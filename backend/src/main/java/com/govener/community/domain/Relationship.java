package com.govener.community.domain;

import com.govener.shared.domain.*;
import java.time.LocalDate;

public record Relationship(
    String id,
    String communityId,
    String unitId,
    String personId,
    String type,
    LocalDate startsOn,
    LocalDate endsOn,
    long version)
    implements ScopedAggregate {
  public Relationship {
    if (!java.util.Set.of("OWNER", "TENANT").contains(type))
      throw RuleViolation.invalid("Invalid relationship type.");
    new UnitAccess(unitId, startsOn, endsOn);
  }
}
