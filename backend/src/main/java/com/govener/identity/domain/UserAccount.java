package com.govener.identity.domain;

import com.govener.shared.domain.*;

public record UserAccount(
    String id,
    String personId,
    String loginEmail,
    String passwordHash,
    String name,
    String email,
    String phone,
    Preferences preferences,
    long securityVersion,
    long version)
    implements Aggregate {
  public UserAccount {
    id = Checks.id(id);
    personId = Checks.id(personId);
    loginEmail = normalizeEmail(loginEmail);
    passwordHash = Checks.text(passwordHash, 512);
    name = Checks.text(name, 100);
    email = normalizeEmail(email);
    phone = Checks.optional(phone, 30);
    Checks.required(preferences);
  }

  public static String normalizeEmail(String value) {
    String email = Checks.text(value, 200).toLowerCase(java.util.Locale.ROOT);
    if (email.chars().anyMatch(Character::isWhitespace) || !email.matches("[^@]+@[^@]+[.][^@]+"))
      throw RuleViolation.invalid("Invalid email.");
    return email;
  }

  public UserAccount updateProfile(
      String newName, String contactEmail, String newPhone, long expected) {
    Checks.version(version, expected);
    return new UserAccount(
        id,
        personId,
        loginEmail,
        passwordHash,
        newName,
        contactEmail,
        newPhone,
        preferences,
        securityVersion,
        version + 1);
  }

  public UserAccount updatePreferences(Preferences next, long expected) {
    Checks.version(version, expected);
    return new UserAccount(
        id,
        personId,
        loginEmail,
        passwordHash,
        name,
        email,
        phone,
        next,
        securityVersion,
        version + 1);
  }

  public UserAccount resetPassword(String hash) {
    return new UserAccount(
        id,
        personId,
        loginEmail,
        hash,
        name,
        email,
        phone,
        preferences,
        securityVersion + 1,
        version + 1);
  }
}
