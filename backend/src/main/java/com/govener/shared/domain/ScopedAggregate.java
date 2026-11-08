package com.govener.shared.domain;

public interface ScopedAggregate extends Aggregate {
  String communityId();
}
