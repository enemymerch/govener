package com.govener.identity.adapter.out.security;

import com.govener.identity.application.IdentityPorts;
import java.util.Map;
import org.springframework.security.crypto.password.*;

public final class Pbkdf2PasswordAdapter implements IdentityPorts.Passwords {
  private final PasswordEncoder encoder =
      new DelegatingPasswordEncoder(
          "pbkdf2", Map.of("pbkdf2", Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8()));

  public String encode(String raw) {
    return encoder.encode(raw);
  }

  public boolean matches(String raw, String encoded) {
    return encoder.matches(raw, encoded);
  }
}
