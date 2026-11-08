package com.govener.community.domain;

import com.govener.shared.domain.*;
import java.time.LocalDate;

public record Residence(
    String id,
    String communityId,
    String unitId,
    String personId,
    LocalDate startsOn,
    LocalDate endsOn,
    long version)
    implements ScopedAggregate {
  public Residence {
    new UnitAccess(unitId, startsOn, endsOn);
  }

  public boolean active(LocalDate on) {
    return new UnitAccess(unitId, startsOn, endsOn).active(on);
  }
}
