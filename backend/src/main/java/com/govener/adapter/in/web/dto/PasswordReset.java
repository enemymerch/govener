package com.govener.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

/** Transport contract generated from docs/openapi.json; domain models are separate. */
public record PasswordReset(
    @Valid @NotNull @NotBlank @Size(min = 1, max = 2147483647) String token,
    @Valid @NotNull @Size(min = 8, max = 128) String password) {
  @Override
  public String toString() {
    return "PasswordReset[REDACTED]";
  }
}
