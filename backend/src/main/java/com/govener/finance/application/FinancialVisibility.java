package com.govener.finance.application;

import com.govener.community.domain.*;
import com.govener.finance.domain.*;
import com.govener.shared.domain.*;
import java.time.LocalDate;

public interface FinancialVisibility {
  boolean chargeVisible(Membership membership, Charge charge, LocalDate today);

  static FinancialVisibility denyUnconfigured() {
    return (member, charge, today) -> {
      if (member.manager()) return true;
      throw new RuleViolation(
          RuleViolation.Kind.FORBIDDEN,
          "FINANCIAL_VISIBILITY_UNCONFIGURED",
          "Resident financial visibility has not been configured.");
    };
  }

  static FinancialVisibility relationshipPeriod() {
    return (member, charge, today) ->
        member.manager()
            || member.units().stream()
                .anyMatch(
                    access ->
                        access.unitId().equals(charge.unitId())
                            && access.active(today)
                            && access.covers(charge.period().start(), charge.period().end()));
  }
}
