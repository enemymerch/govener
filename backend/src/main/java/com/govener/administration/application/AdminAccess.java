package com.govener.administration.application;

import com.govener.administration.domain.AdminAuthority;
import com.govener.community.application.CommunityPorts;
import com.govener.community.domain.Community;
import com.govener.shared.domain.*;

public final class AdminAccess {
  private final AdministrationPorts.Authorities authorities;
  private final CommunityPorts.Communities communities;

  public AdminAccess(
      AdministrationPorts.Authorities authorities, CommunityPorts.Communities communities) {
    this.authorities = authorities;
    this.communities = communities;
  }

  public AdminAuthority authority(String userId) {
    return authorities.find(userId).orElseThrow(RuleViolation::forbidden);
  }

  public AdminAuthority requireRoot(String userId) {
    var authority = authority(userId);
    if (!authority.root()) throw RuleViolation.forbidden();
    return authority;
  }

  public Community community(String userId, String id) {
    Checks.id(id);
    if (!authority(userId).allows(id)) throw RuleViolation.missing();
    return communities.find(id).orElseThrow(RuleViolation::missing);
  }
}
