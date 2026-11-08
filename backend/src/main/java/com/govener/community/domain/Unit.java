package com.govener.community.domain;

import com.govener.shared.domain.*;

public record Unit(
    String id,
    String communityId,
    String label,
    String blockId,
    String blockLabel,
    int floor,
    String kind,
    double areaSqm,
    long version)
    implements ScopedAggregate {
  public Unit {
    id = Checks.id(id);
    communityId = Checks.id(communityId);
    label = Checks.text(label, 100);
    blockId = Checks.id(blockId);
    blockLabel = Checks.text(blockLabel, 100);
    if (!java.util.Set.of("APARTMENT", "SHOP", "OFFICE").contains(kind)
        || !Double.isFinite(areaSqm)
        || areaSqm < 0) throw RuleViolation.invalid("Invalid unit.");
  }
}
