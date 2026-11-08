package com.govener;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.govener.community.application.*;
import com.govener.community.domain.*;
import com.govener.finance.application.*;
import com.govener.finance.domain.IdempotentResult;
import com.govener.identity.application.*;
import com.govener.identity.domain.*;
import com.govener.shared.adapter.out.json.JacksonSerializationAdapter;
import com.govener.shared.application.*;
import com.govener.shared.domain.*;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import org.junit.jupiter.api.*;

class ApplicationServicesTest {
  private final Clock clock = Clock.fixed(Instant.parse("2026-11-08T09:00:00Z"), ZoneOffset.UTC);
  private final SerializationPort serialization =
      new JacksonSerializationAdapter(JsonMapper.builder().findAndAddModules().build());
  private final TransactionPort transactions =
      new TransactionPort() {
        public <T> T execute(Supplier<T> work) {
          return work.get();
        }
      };
  private final IdentityPorts.Users users = mock(IdentityPorts.Users.class);
  private final IdentityPorts.Resets resets = mock(IdentityPorts.Resets.class);
  private final IdentityPorts.Passwords passwords = mock(IdentityPorts.Passwords.class);
  private final IdentityPorts.ResetDelivery delivery = mock(IdentityPorts.ResetDelivery.class);
  private final IdentityPorts.Attempts attempts = mock(IdentityPorts.Attempts.class);

  private IdentityService identity() {
    return new IdentityService(
        users,
        resets,
        passwords,
        delivery,
        attempts,
        transactions,
        serialization,
        () -> "raw-token",
        clock);
  }

  private UserAccount user() {
    return new UserAccount(
        "user",
        "person",
        "login@example.com",
        "hash",
        "Name",
        "contact@example.com",
        "",
        new Preferences("tr", true, true, false),
        0,
        0);
  }

  @Test
  void successfulLoginUsesNormalizedIdentityAndRateLimiter() {
    when(users.byLoginEmail("login@example.com")).thenReturn(Optional.of(user()));
    when(passwords.matches("password", "hash")).thenReturn(true);
    assertThat(identity().login("LOGIN@example.com", "password", "client").id()).isEqualTo("user");
    verify(attempts).consume(anyString(), eq(10));
  }

  @Test
  void failedLoginDoesNotRevealWhetherIdentityExists() {
    when(users.byLoginEmail(anyString())).thenReturn(Optional.empty());
    when(passwords.encode(anyString())).thenReturn("dummy-hash");
    assertThatThrownBy(() -> identity().login("missing@example.com", "password", "client"))
        .isInstanceOf(RuleViolation.class)
        .hasMessage("Email or password is incorrect.");
    verify(passwords).matches("password", "dummy-hash");
  }

  @Test
  void unknownResetRequestDoesNotSendMail() {
    when(users.byLoginEmail(anyString())).thenReturn(Optional.empty());
    identity().requestReset("missing@example.com", "client");
    verifyNoInteractions(delivery);
    verify(resets, never()).insert(any());
  }

  @Test
  void resetRequestPersistsHashAndDeliversOnlyToLoginAddress() {
    when(users.byLoginEmail("login@example.com")).thenReturn(Optional.of(user()));
    identity().requestReset("login@example.com", "client");
    verify(resets)
        .insert(
            argThat(
                token ->
                    !token.id().equals("raw-token")
                        && token.expiresAt().equals(clock.instant().plusSeconds(900))));
    verify(delivery).deliver("login@example.com", "raw-token");
  }

  @Test
  void expiredResetDoesNotUpdateCredentials() {
    String id = serialization.hash("expired");
    when(resets.find(id))
        .thenReturn(Optional.of(new PasswordReset(id, "user", clock.instant(), null, 0)));
    assertThatThrownBy(() -> identity().resetPassword("expired", "NewPassword123!", "client"))
        .isInstanceOf(RuleViolation.class);
    verify(users, never()).save(any(), anyLong());
  }

  @Test
  void profileUsesCompareAndSwapVersion() {
    when(users.find("user")).thenReturn(Optional.of(user()));
    assertThatThrownBy(() -> identity().updateProfile("user", "New", "new@example.com", "", 1))
        .isInstanceOf(RuleViolation.class);
    verify(users, never()).save(any(), anyLong());
  }

  @Test
  void endedMembershipCannotReadCommunity() {
    var memberships = mock(CommunityPorts.Memberships.class);
    var units = mock(CommunityPorts.Units.class);
    var ended =
        new Membership(
            "m",
            "site",
            "user",
            "person",
            Set.of(Role.RESIDENT),
            Set.of(Permission.DIRECTORY_READ),
            List.of(new UnitAccess("unit", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 10, 31))),
            0);
    when(memberships.forUserInCommunity("user", "site")).thenReturn(Optional.of(ended));
    var access = new AccessService(memberships, units, clock);
    assertThatThrownBy(() -> access.membership("user", "site")).isInstanceOf(RuleViolation.class);
  }

  @Test
  void communityCalendarUsesIstanbul() {
    var access =
        new AccessService(
            mock(CommunityPorts.Memberships.class),
            mock(CommunityPorts.Units.class),
            Clock.fixed(Instant.parse("2026-11-07T22:30:00Z"), ZoneOffset.UTC));
    assertThat(access.today()).isEqualTo(LocalDate.of(2026, 11, 8));
  }

  @Test
  void idempotentReplayDoesNotExecuteWork() {
    var store = mock(FinancePorts.Idempotency.class);
    String id = serialization.hash("user:site:operation:key");
    String fingerprint = serialization.hash(serialization.canonical(Map.of("amount", 100)));
    when(store.find(id))
        .thenReturn(
            Optional.of(
                new IdempotentResult(
                    id,
                    "site",
                    fingerprint,
                    "\"original-result\"",
                    clock.instant().plusSeconds(60),
                    0)));
    var executor = new IdempotentExecutor(transactions, store, serialization, clock);
    @SuppressWarnings("unchecked")
    Supplier<String> work = mock(Supplier.class);
    assertThat(
            executor.execute(
                "user", "site", "operation", "key", Map.of("amount", 100), String.class, work))
        .isEqualTo("original-result");
    verifyNoInteractions(work);
  }

  @Test
  void idempotentKeyRejectsChangedPayload() {
    var store = mock(FinancePorts.Idempotency.class);
    String id = serialization.hash("user:site:operation:key");
    when(store.find(id))
        .thenReturn(
            Optional.of(
                new IdempotentResult(
                    id,
                    "site",
                    "different-fingerprint",
                    "\"original\"",
                    clock.instant().plusSeconds(60),
                    0)));
    var executor = new IdempotentExecutor(transactions, store, serialization, clock);
    assertThatThrownBy(
            () ->
                executor.execute(
                    "user",
                    "site",
                    "operation",
                    "key",
                    Map.of("amount", 200),
                    String.class,
                    () -> "new"))
        .isInstanceOf(RuleViolation.class)
        .satisfies(
            failure ->
                assertThat(((RuleViolation) failure).code()).isEqualTo("IDEMPOTENCY_CONFLICT"));
  }
}
