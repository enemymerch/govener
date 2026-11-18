package com.govener.administration.application;

import com.govener.administration.domain.AdminAuthority;
import com.govener.shared.application.AggregateRepository;

public final class AdministrationPorts {
  private AdministrationPorts() {}

  public interface Authorities extends AggregateRepository<AdminAuthority> {}
}
