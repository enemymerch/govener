package com.govener.communication.application;

import com.govener.communication.domain.Announcement;
import com.govener.shared.application.AggregateRepository;

public final class AnnouncementPorts {
  private AnnouncementPorts() {}

  public interface Announcements extends AggregateRepository<Announcement> {}
}
