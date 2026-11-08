package com.govener.finance.domain;

import com.govener.shared.domain.*;
import java.time.Instant;
import java.util.List;

public record ChargeBatch(
    String id, String communityId, List<Charge> charges, Instant createdAt, long version)
    implements ScopedAggregate {
  public ChargeBatch {
    charges = List.copyOf(charges);
  }
}
