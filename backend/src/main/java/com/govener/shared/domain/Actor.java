package com.govener.shared.domain;

public record Actor(String id, String name) {
  public Actor {
    id = Checks.id(id);
    name = Checks.text(name, 100);
  }
}
