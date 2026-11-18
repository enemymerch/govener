package com.govener.administration.application;

import com.govener.community.domain.*;
import java.time.LocalDate;
import java.util.Set;

public final class AdminCommands {
  private AdminCommands() {}

  public record CommunityWrite(String name, String address) {}

  public record UnitWrite(
      String label, String blockId, String blockLabel, int floor, String kind, double areaSqm) {}

  public record UserCreate(
      String name,
      String loginEmail,
      String phone,
      String password,
      Role role,
      Set<Permission> permissions,
      String unitId,
      String relationshipType,
      LocalDate startsOn,
      LocalDate endsOn,
      boolean livesHere) {
    @Override
    public String toString() {
      return "UserCreate[REDACTED]";
    }
  }

  public record AdministratorCreate(
      String name, String loginEmail, String phone, String password, Set<String> communityIds) {
    @Override
    public String toString() {
      return "AdministratorCreate[REDACTED]";
    }
  }
}
