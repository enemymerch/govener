package com.govener;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.govener.shared.adapter.out.crypto.HmacCursorSigner;
import com.govener.shared.adapter.out.json.JacksonSerializationAdapter;
import com.govener.shared.application.*;
import com.govener.shared.domain.RuleViolation;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class PaginationTest {
  private final Instant now = Instant.parse("2026-11-08T09:00:00Z");

  private Pagination pagination(Instant time) {
    return new Pagination(
        new HmacCursorSigner("test-secret-at-least-thirty-two-characters"),
        new JacksonSerializationAdapter(JsonMapper.builder().findAndAddModules().build()),
        Clock.fixed(time, ZoneOffset.UTC));
  }

  @Test
  void pagesWithoutDuplicateItems() {
    var pagination = pagination(now);
    var first = pagination.page(List.of("c", "a", "b"), "scope", null, 2, x -> x);
    assertThat(first.items()).containsExactly("a", "b");
    assertThat(
            pagination.page(List.of("a", "b", "c"), "scope", first.nextCursor(), 2, x -> x).items())
        .containsExactly("c");
  }

  @Test
  void rejectsDifferentUserOrFilterScope() {
    var pagination = pagination(now);
    var first = pagination.page(List.of("a", "b"), "user:site:filter", null, 1, x -> x);
    assertThatThrownBy(
            () ->
                pagination.page(
                    List.of("a", "b"), "other:site:filter", first.nextCursor(), 1, x -> x))
        .isInstanceOf(RuleViolation.class);
  }

  @Test
  void rejectsTamperedCursor() {
    var pagination = pagination(now);
    String cursor = pagination.page(List.of("a", "b"), "scope", null, 1, x -> x).nextCursor();
    assertThatThrownBy(() -> pagination.page(List.of("a", "b"), "scope", cursor + "x", 1, x -> x))
        .isInstanceOf(RuleViolation.class);
  }

  @Test
  void rejectsExpiredCursor() {
    String cursor = pagination(now).page(List.of("a", "b"), "scope", null, 1, x -> x).nextCursor();
    assertThatThrownBy(
            () ->
                pagination(now.plusSeconds(3600))
                    .page(List.of("a", "b"), "scope", cursor, 1, x -> x))
        .isInstanceOf(RuleViolation.class);
  }
}
