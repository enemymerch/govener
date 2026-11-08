package com.govener.identity.application;

import com.govener.identity.domain.*;
import com.govener.shared.application.AggregateRepository;
import java.util.Optional;

public final class IdentityPorts {
  private IdentityPorts() {}

  public interface Users extends AggregateRepository<UserAccount> {
    Optional<UserAccount> byLoginEmail(String email);
  }

  public interface Resets extends AggregateRepository<PasswordReset> {}

  public interface Passwords {
    String encode(String raw);

    boolean matches(String raw, String encoded);
  }

  public interface ResetDelivery {
    void deliver(String email, String rawToken);
  }

  public interface Attempts {
    void consume(String key, int maximum);
  }
}
