package com.govener.shared.domain;

public final class RuleViolation extends RuntimeException {
  public enum Kind {
    VALIDATION,
    CONFLICT,
    NOT_FOUND,
    FORBIDDEN,
    UNAUTHORIZED,
    RATE_LIMITED
  }

  private final Kind kind;
  private final String code;

  public RuleViolation(Kind kind, String code, String message) {
    super(message);
    this.kind = kind;
    this.code = code;
  }

  public Kind kind() {
    return kind;
  }

  public String code() {
    return code;
  }

  public static RuleViolation invalid(String message) {
    return new RuleViolation(Kind.VALIDATION, "VALIDATION_FAILED", message);
  }

  public static RuleViolation conflict(String message) {
    return new RuleViolation(Kind.CONFLICT, "VERSION_CONFLICT", message);
  }

  public static RuleViolation missing() {
    return new RuleViolation(Kind.NOT_FOUND, "RESOURCE_NOT_FOUND", "Resource is unavailable.");
  }

  public static RuleViolation forbidden() {
    return new RuleViolation(Kind.FORBIDDEN, "ACCESS_DENIED", "The operation is not permitted.");
  }
}
