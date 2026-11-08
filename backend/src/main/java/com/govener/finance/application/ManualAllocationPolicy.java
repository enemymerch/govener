package com.govener.finance.application;

import com.govener.finance.domain.*;
import com.govener.shared.domain.*;
import java.util.*;

public final class ManualAllocationPolicy {
  public List<Charge> allocate(
      Payment payment, java.util.function.Function<String, Charge> lookup) {
    List<Charge> changes = new ArrayList<>();
    for (Allocation allocation : payment.allocations()) {
      Charge charge = lookup.apply(allocation.chargeId());
      if (!charge.communityId().equals(payment.communityId())
          || !charge.unitId().equals(payment.unitId()))
        throw RuleViolation.invalid("Charges and payments must belong to the same unit.");
      changes.add(charge.allocate(allocation.amountMinor()));
    }
    return List.copyOf(changes);
  }
}
