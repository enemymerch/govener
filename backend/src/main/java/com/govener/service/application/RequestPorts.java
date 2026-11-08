package com.govener.service.application;

import com.govener.service.domain.*;
import com.govener.shared.application.AggregateRepository;

public final class RequestPorts {
  private RequestPorts() {}

  public interface Requests extends AggregateRepository<ServiceRequest> {}

  public interface Comments extends AggregateRepository<RequestComment> {}

  public interface Events extends AggregateRepository<RequestEvent> {}

  public interface Sequences {
    long next(String scope);
  }
}
