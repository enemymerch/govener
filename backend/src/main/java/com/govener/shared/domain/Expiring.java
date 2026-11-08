package com.govener.shared.domain;

public interface Expiring {
  java.time.Instant expiresAt();
}
