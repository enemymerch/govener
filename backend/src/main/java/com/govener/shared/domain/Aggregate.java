package com.govener.shared.domain;

public interface Aggregate {
  String id();

  long version();
}
