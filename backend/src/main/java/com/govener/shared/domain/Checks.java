package com.govener.shared.domain;

import java.util.Objects;

public final class Checks {
  private Checks() {}

  public static String id(String value) {
    if (value == null || !value.matches("[A-Za-z0-9][A-Za-z0-9_-]{0,99}"))
      throw RuleViolation.invalid("Invalid identifier.");
    return value;
  }

  public static String text(String value, int maximum) {
    if (value == null || value.isBlank() || value.length() > maximum)
      throw RuleViolation.invalid("Text is missing or too long.");
    return value.strip();
  }

  public static String optional(String value, int maximum) {
    if (value == null) return "";
    if (value.length() > maximum) throw RuleViolation.invalid("Text is too long.");
    return value.strip();
  }

  public static void version(long actual, long expected) {
    if (actual != expected)
      throw RuleViolation.conflict("The resource changed; reload before retrying.");
  }

  public static <T> T required(T value) {
    if (Objects.isNull(value)) throw RuleViolation.invalid("A required value is missing.");
    return value;
  }
}
