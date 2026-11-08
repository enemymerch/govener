package com.govener.identity.adapter.in.security;

import java.io.Serializable;

public record SessionPrincipal(String id, long securityVersion) implements Serializable {
  private static final long serialVersionUID = 1L;
}
