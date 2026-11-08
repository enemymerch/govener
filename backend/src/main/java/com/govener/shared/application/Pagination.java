package com.govener.shared.application;

import com.govener.shared.domain.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.function.Function;

public final class Pagination {
  private final CursorSigner signer;
  private final SerializationPort serialization;
  private final Clock clock;

  public record Cursor(String scope, String after, Instant expiresAt) {}

  public Pagination(CursorSigner signer, SerializationPort serialization, Clock clock) {
    this.signer = signer;
    this.serialization = serialization;
    this.clock = clock;
  }

  public <T> Page<T> page(
      List<T> records, String scope, String cursor, int limit, Function<T, String> sortKey) {
    if (limit < 1 || limit > 100)
      throw RuleViolation.invalid("Page limit must be between 1 and 100.");
    String binding = serialization.hash(scope);
    String after = null;
    if (cursor != null) {
      try {
        if (cursor.length() > 4096) throw new IllegalArgumentException();
        String[] parts = cursor.split("\\.", -1);
        if (parts.length != 2 || !signer.valid(parts[0], parts[1]))
          throw new IllegalArgumentException();
        Cursor decoded =
            serialization.read(
                new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8),
                Cursor.class);
        if (!binding.equals(decoded.scope()) || !decoded.expiresAt().isAfter(clock.instant()))
          throw new IllegalArgumentException();
        after = decoded.after();
      } catch (RuntimeException failure) {
        throw RuleViolation.invalid("Cursor is invalid, expired or outside this query scope.");
      }
    }
    String position = after;
    List<T> remaining =
        records.stream()
            .sorted(Comparator.comparing(sortKey))
            .filter(item -> position == null || sortKey.apply(item).compareTo(position) > 0)
            .limit(limit + 1L)
            .toList();
    boolean more = remaining.size() > limit;
    List<T> items = more ? remaining.subList(0, limit) : remaining;
    String next = null;
    if (more) {
      String payload =
          Base64.getUrlEncoder()
              .withoutPadding()
              .encodeToString(
                  serialization
                      .canonical(
                          new Cursor(
                              binding,
                              sortKey.apply(items.getLast()),
                              clock.instant().plus(Duration.ofHours(1))))
                      .getBytes(StandardCharsets.UTF_8));
      next = payload + "." + signer.sign(payload);
    }
    return new Page<>(items, next, more);
  }
}
