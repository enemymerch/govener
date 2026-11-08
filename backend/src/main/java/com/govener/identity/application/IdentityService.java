package com.govener.identity.application;

import com.govener.identity.domain.*;
import com.govener.shared.application.*;
import com.govener.shared.domain.*;
import java.time.*;
import java.util.function.Supplier;

public final class IdentityService {
  private final IdentityPorts.Users users;
  private final IdentityPorts.Resets resets;
  private final IdentityPorts.Passwords passwords;
  private final IdentityPorts.ResetDelivery delivery;
  private final IdentityPorts.Attempts attempts;
  private final TransactionPort transactions;
  private final SerializationPort serialization;
  private final Supplier<String> tokens;
  private final Clock clock;
  private final String dummyHash;

  public IdentityService(
      IdentityPorts.Users users,
      IdentityPorts.Resets resets,
      IdentityPorts.Passwords passwords,
      IdentityPorts.ResetDelivery delivery,
      IdentityPorts.Attempts attempts,
      TransactionPort transactions,
      SerializationPort serialization,
      Supplier<String> tokens,
      Clock clock) {
    this.users = users;
    this.resets = resets;
    this.passwords = passwords;
    this.delivery = delivery;
    this.attempts = attempts;
    this.transactions = transactions;
    this.serialization = serialization;
    this.tokens = tokens;
    this.clock = clock;
    dummyHash = passwords.encode(tokens.get());
  }

  public UserAccount user(String id) {
    return users
        .find(id)
        .orElseThrow(
            () ->
                new RuleViolation(
                    RuleViolation.Kind.UNAUTHORIZED,
                    "AUTHENTICATION_REQUIRED",
                    "Authentication is required."));
  }

  public void csrfRequested(String clientKey) {
    attempts.consume("csrf:" + clientKey, 60);
  }

  public UserAccount login(String email, String password, String clientKey) {
    attempts.consume("login-ip:" + clientKey, 60);
    attempts.consume(
        "login:" + clientKey + ":" + serialization.hash(email.toLowerCase(java.util.Locale.ROOT)),
        10);
    var found = users.byLoginEmail(UserAccount.normalizeEmail(email));
    boolean matches =
        passwords.matches(password, found.map(UserAccount::passwordHash).orElse(dummyHash));
    if (found.isEmpty() || !matches)
      throw new RuleViolation(
          RuleViolation.Kind.UNAUTHORIZED,
          "INVALID_CREDENTIALS",
          "Email or password is incorrect.");
    return found.get();
  }

  public UserAccount updateProfile(
      String id, String name, String email, String phone, long expected) {
    return transactions.execute(
        () -> {
          var current = user(id);
          var next = current.updateProfile(name, email, phone, expected);
          users.save(next, current.version());
          return next;
        });
  }

  public UserAccount updatePreferences(String id, Preferences preferences, long expected) {
    return transactions.execute(
        () -> {
          var current = user(id);
          var next = current.updatePreferences(preferences, expected);
          users.save(next, current.version());
          return next;
        });
  }

  public void requestReset(String email, String clientKey) {
    attempts.consume("reset:" + clientKey, 5);
    users
        .byLoginEmail(UserAccount.normalizeEmail(email))
        .ifPresent(
            user -> {
              String raw = tokens.get();
              transactions.execute(
                  () -> {
                    resets.insert(
                        new PasswordReset(
                            serialization.hash(raw),
                            user.id(),
                            clock.instant().plus(Duration.ofMinutes(15)),
                            null,
                            0));
                    delivery.deliver(user.loginEmail(), raw);
                    return null;
                  });
            });
  }

  public void resetPassword(String raw, String password, String clientKey) {
    attempts.consume("password-reset:" + clientKey, 10);
    if (password == null || password.length() < 8 || password.length() > 128)
      throw RuleViolation.invalid("Password must contain 8 to 128 characters.");
    String hash = passwords.encode(password);
    transactions.execute(
        () -> {
          var token =
              resets
                  .find(serialization.hash(Checks.text(raw, 512)))
                  .orElseThrow(() -> RuleViolation.invalid("Reset link is invalid or expired."));
          var consumed = token.consume(clock.instant());
          var current = user(token.userId());
          users.save(current.resetPassword(hash), current.version());
          resets.save(consumed, token.version());
          return null;
        });
  }
}
