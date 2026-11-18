package com.govener.poll.application;

import com.govener.poll.domain.Poll;
import com.govener.shared.application.AggregateRepository;

public final class PollPorts {
  private PollPorts() {}

  public interface Polls extends AggregateRepository<Poll> {}
}
