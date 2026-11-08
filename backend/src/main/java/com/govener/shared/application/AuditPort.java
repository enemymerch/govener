package com.govener.shared.application;

import com.govener.shared.domain.AuditRecord;

public interface AuditPort {
  void append(AuditRecord event);
}
