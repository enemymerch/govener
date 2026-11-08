package com.govener.finance.domain;

import com.govener.shared.domain.*;

public record Allocation(String chargeId, long amountMinor) {
  public Allocation {
    chargeId = Checks.id(chargeId);
    Money.positive(amountMinor);
  }
}
