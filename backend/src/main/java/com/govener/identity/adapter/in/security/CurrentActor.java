package com.govener.identity.adapter.in.security;

import com.govener.identity.application.IdentityService;
import com.govener.identity.domain.UserAccount;
import com.govener.shared.domain.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public final class CurrentActor {
  private final IdentityService identity;

  public CurrentActor(IdentityService identity) {
    this.identity = identity;
  }

  public UserAccount user() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !(authentication.getPrincipal() instanceof SessionPrincipal principal))
      throw new RuleViolation(
          RuleViolation.Kind.UNAUTHORIZED,
          "AUTHENTICATION_REQUIRED",
          "Authentication is required.");
    return identity.user(principal.id());
  }

  public String id() {
    return user().id();
  }

  public Actor actor() {
    var user = user();
    return new Actor(user.id(), user.name());
  }
}
