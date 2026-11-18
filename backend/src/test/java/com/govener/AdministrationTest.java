package com.govener;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.govener.administration.application.*;
import com.govener.administration.domain.*;
import com.govener.community.application.CommunityPorts;
import com.govener.community.domain.Community;
import com.govener.shared.domain.RuleViolation;
import java.util.*;
import org.junit.jupiter.api.Test;

class AdministrationTest {
  @Test
  void rootHasGlobalScopeAndCannotBeReassigned() {
    var root = new AdminAuthority("root", AdminRole.ROOT, Set.of(), 0);
    assertThat(root.allows("any-site")).isTrue();
    assertThatThrownBy(() -> root.assign(Set.of("site-a"), 0)).isInstanceOf(RuleViolation.class);
  }

  @Test
  void onlyReservedAccountMayBeRoot() {
    assertThatThrownBy(() -> new AdminAuthority("other", AdminRole.ROOT, Set.of(), 0))
        .isInstanceOf(RuleViolation.class);
    assertThatThrownBy(() -> new AdminAuthority("root", AdminRole.ADMIN, Set.of(), 0))
        .isInstanceOf(RuleViolation.class);
  }

  @Test
  void removingAllAssignmentsImmediatelyDeniesCommunityAccess() {
    var admin = new AdminAuthority("admin", AdminRole.ADMIN, Set.of("site-a"), 0);
    var revoked = admin.assign(Set.of(), 0);
    assertThat(revoked.allows("site-a")).isFalse();
    assertThat(revoked.version()).isEqualTo(1);
  }

  @Test
  void staleAssignmentCannotOverwriteNewerAuthority() {
    var admin = new AdminAuthority("admin", AdminRole.ADMIN, Set.of("site-a"), 2);
    assertThatThrownBy(() -> admin.assign(Set.of("site-b"), 1)).isInstanceOf(RuleViolation.class);
  }

  @Test
  void scopeIsImmutable() {
    var scopes = new HashSet<>(Set.of("site-a"));
    var admin = new AdminAuthority("admin", AdminRole.ADMIN, scopes, 0);
    scopes.add("site-b");
    assertThat(admin.allows("site-b")).isFalse();
  }

  @Test
  void accessDoesNotUseManagerMembership() {
    var authorities = mock(AdministrationPorts.Authorities.class);
    var communities = mock(CommunityPorts.Communities.class);
    when(authorities.find("manager")).thenReturn(Optional.empty());
    var access = new AdminAccess(authorities, communities);
    assertThatThrownBy(() -> access.authority("manager")).isInstanceOf(RuleViolation.class);
    verifyNoInteractions(communities);
  }

  @Test
  void forbiddenCommunityIsNotLookedUp() {
    var authorities = mock(AdministrationPorts.Authorities.class);
    var communities = mock(CommunityPorts.Communities.class);
    when(authorities.find("admin"))
        .thenReturn(Optional.of(new AdminAuthority("admin", AdminRole.ADMIN, Set.of("site-a"), 0)));
    var access = new AdminAccess(authorities, communities);
    assertThatThrownBy(() -> access.community("admin", "site-b")).isInstanceOf(RuleViolation.class);
    assertThatThrownBy(() -> access.requireRoot("admin")).isInstanceOf(RuleViolation.class);
    verifyNoInteractions(communities);
  }

  @Test
  void rootCanOpenUnassignedCommunity() {
    var authorities = mock(AdministrationPorts.Authorities.class);
    var communities = mock(CommunityPorts.Communities.class);
    when(authorities.find("root"))
        .thenReturn(Optional.of(new AdminAuthority("root", AdminRole.ROOT, Set.of(), 0)));
    var community = new Community("site-b", "Site B", "Istanbul", 0);
    when(communities.find("site-b")).thenReturn(Optional.of(community));
    assertThat(new AdminAccess(authorities, communities).community("root", "site-b"))
        .isEqualTo(community);
  }
}
