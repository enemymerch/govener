package com.govener.community.domain;

import com.govener.shared.domain.*;

public record Community(String id, String name, String address, long version)
    implements ScopedAggregate {
  public Community {
    id = Checks.id(id);
    name = Checks.text(name, 200);
    address = Checks.optional(address, 300);
  }

  public String communityId() {
    return id;
  }
}
