package com.govener.adapter.in.web;

import com.govener.shared.application.Page;
import java.util.*;
import java.util.function.Function;

public final class Views {
  private Views() {}

  public static Map<String, Object> map(Object... pairs) {
    Map<String, Object> result = new LinkedHashMap<>();
    for (int i = 0; i < pairs.length; i += 2) result.put((String) pairs[i], pairs[i + 1]);
    return result;
  }

  public static <T> Map<String, Object> page(Page<T> page, Function<T, ?> mapper) {
    return map(
        "items",
        page.items().stream().map(mapper).toList(),
        "nextCursor",
        page.nextCursor(),
        "hasMore",
        page.hasMore());
  }
}
