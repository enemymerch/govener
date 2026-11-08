package com.govener.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

/** Transport contract generated from docs/openapi.json; domain models are separate. */
public record LoginRequest(
    @Valid @NotNull @Email String email,
    @Valid @NotNull @NotBlank @Size(min = 1, max = 128) String password) {
  @Override
  public String toString() {
    return "LoginRequest[REDACTED]";
  }
}
