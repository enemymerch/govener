package com.govener.community.application;

import com.govener.community.domain.*;
import com.govener.shared.application.AggregateRepository;
import java.util.*;

public final class CommunityPorts {
  private CommunityPorts() {}

  public interface Communities extends AggregateRepository<Community> {}

  public interface Units extends AggregateRepository<Unit> {}

  public interface People extends AggregateRepository<Person> {}

  public interface Memberships extends AggregateRepository<Membership> {
    List<Membership> forUser(String userId);

    Optional<Membership> forUserInCommunity(String userId, String communityId);
  }

  public interface Relationships extends AggregateRepository<Relationship> {}

  public interface Residences extends AggregateRepository<Residence> {}
}
