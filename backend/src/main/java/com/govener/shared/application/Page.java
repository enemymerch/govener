package com.govener.shared.application;

import java.util.List;

public record Page<T>(List<T> items, String nextCursor, boolean hasMore) {
  public Page {
    items = List.copyOf(items);
  }
}
