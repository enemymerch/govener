package com.govener.community.domain;

import com.govener.shared.domain.*;
import java.time.LocalDate;

public record UnitAccess(String unitId, LocalDate startsOn, LocalDate endsOn) {
  public UnitAccess {
    unitId = Checks.id(unitId);
    Checks.required(startsOn);
    if (endsOn != null && endsOn.isBefore(startsOn))
      throw RuleViolation.invalid("Invalid access interval.");
  }

  public boolean active(LocalDate on) {
    return !on.isBefore(startsOn) && (endsOn == null || !on.isAfter(endsOn));
  }

  public boolean covers(LocalDate start, LocalDate end) {
    return !start.isBefore(startsOn) && (endsOn == null || !end.isAfter(endsOn));
  }
}
