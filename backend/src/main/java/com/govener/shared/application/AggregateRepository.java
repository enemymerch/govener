package com.govener.shared.application;

import com.govener.shared.domain.Aggregate;
import java.util.*;

public interface AggregateRepository<T extends Aggregate> {
  Optional<T> find(String id);

  List<T> inCommunity(String communityId);

  List<T> all();

  void insert(T aggregate);

  void save(T aggregate, long expectedVersion);
}
