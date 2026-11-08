package com.govener.communication.domain;

import com.govener.shared.domain.*;
import java.util.*;

public record Audience(Type type, List<String> ids) {
  public enum Type {
    ALL,
    BLOCKS,
    UNITS
  }

  public Audience {
    Checks.required(type);
    ids = List.copyOf(ids);
    if (type == Type.ALL && !ids.isEmpty()
        || type != Type.ALL && ids.isEmpty()
        || ids.size() > 1000
        || ids.stream().distinct().count() != ids.size())
      throw RuleViolation.invalid("Invalid audience selection.");
    ids.forEach(Checks::id);
  }

  public boolean includes(String unitId, String blockId) {
    return type == Type.ALL || ids.contains(type == Type.UNITS ? unitId : blockId);
  }
}
