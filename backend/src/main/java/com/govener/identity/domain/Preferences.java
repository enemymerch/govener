package com.govener.identity.domain;

import com.govener.shared.domain.RuleViolation;

public record Preferences(
    String language, boolean inApp, boolean push, boolean emailNotifications) {
  public Preferences {
    if (!"tr".equals(language) && !"en".equals(language))
      throw RuleViolation.invalid("Unsupported language.");
  }

  public static Preferences defaults() {
    return new Preferences("tr", true, true, false);
  }
}
