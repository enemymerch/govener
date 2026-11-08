package com.govener.adapter.in.web;

import com.govener.adapter.in.web.dto.*;
import com.govener.identity.adapter.in.security.CurrentActor;
import com.govener.identity.application.IdentityService;
import com.govener.identity.domain.Preferences;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/me")
public class ProfileController {
  private final IdentityService identity;
  private final CurrentActor current;
  private final ViewMapper views;

  public ProfileController(IdentityService identity, CurrentActor current, ViewMapper views) {
    this.identity = identity;
    this.current = current;
    this.views = views;
  }

  @GetMapping
  public Object get() {
    return views.profile(current.user());
  }

  @PatchMapping
  public Object update(@Valid @RequestBody ProfileUpdate body) {
    return views.profile(
        identity.updateProfile(
            current.id(), body.name(), body.email(), body.phone(), body.expectedVersion()));
  }

  @GetMapping("/preferences")
  public Object preferences() {
    return views.preferences(current.user());
  }

  @PatchMapping("/preferences")
  public Object preferences(@Valid @RequestBody PreferencesUpdate body) {
    return views.preferences(
        identity.updatePreferences(
            current.id(),
            new Preferences(body.language(), body.inApp(), body.push(), body.emailNotifications()),
            body.expectedVersion()));
  }
}
