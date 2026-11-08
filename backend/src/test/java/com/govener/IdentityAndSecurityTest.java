package com.govener;

import static org.assertj.core.api.Assertions.*;

import com.govener.identity.adapter.out.mail.ResetTokenCipher;
import com.govener.identity.adapter.out.security.Pbkdf2PasswordAdapter;
import com.govener.identity.domain.*;
import com.govener.shared.domain.RuleViolation;
import java.time.*;
import org.junit.jupiter.api.Test;

class IdentityAndSecurityTest {
  @Test
  void resetTokenExpiresAtExactBoundary() {
    Instant now = Instant.now();
    var reset = new PasswordReset("hash", "user", now, null, 0);
    assertThatThrownBy(() -> reset.consume(now)).isInstanceOf(RuleViolation.class);
  }

  @Test
  void resetTokenIsSingleUse() {
    Instant now = Instant.now();
    var reset = new PasswordReset("hash", "user", now.plusSeconds(60), null, 0).consume(now);
    assertThatThrownBy(() -> reset.consume(now)).isInstanceOf(RuleViolation.class);
  }

  @Test
  void resetRevokesExistingSessions() {
    var user =
        new UserAccount(
            "user",
            "person",
            "LOGIN@example.com",
            "hash",
            "Name",
            "contact@example.com",
            "",
            new Preferences("tr", true, true, false),
            2,
            4);
    assertThat(user.resetPassword("next-hash").securityVersion()).isEqualTo(3);
    assertThat(user.updateProfile("New", "new@example.com", "", 4).loginEmail())
        .isEqualTo("login@example.com");
  }

  @Test
  void passwordsUseSaltedHashes() {
    var passwords = new Pbkdf2PasswordAdapter();
    String one = passwords.encode("Password123!");
    String two = passwords.encode("Password123!");
    assertThat(one).isNotEqualTo(two);
    assertThat(passwords.matches("Password123!", one)).isTrue();
    assertThat(passwords.matches("wrong", one)).isFalse();
  }

  @Test
  void mailTokensAreEncryptedAndAuthenticated() {
    var cipher = new ResetTokenCipher("test-secret");
    String encrypted = cipher.encrypt("one-time-token");
    assertThat(encrypted).doesNotContain("one-time-token");
    assertThat(cipher.decrypt(encrypted)).isEqualTo("one-time-token");
    assertThatThrownBy(() -> new ResetTokenCipher("another-secret").decrypt(encrypted))
        .isInstanceOf(IllegalStateException.class);
  }
}
