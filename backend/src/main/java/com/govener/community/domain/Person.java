package com.govener.community.domain;

import com.govener.shared.domain.*;

public record Person(String id, String communityId, String name, long version)
    implements ScopedAggregate {
  public Person {
    id = Checks.id(id);
    communityId = Checks.id(communityId);
    name = Checks.text(name, 100);
  }
}
