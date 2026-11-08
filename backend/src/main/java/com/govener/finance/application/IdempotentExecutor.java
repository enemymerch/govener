package com.govener.finance.application;

import com.govener.finance.domain.IdempotentResult;
import com.govener.shared.application.*;
import com.govener.shared.domain.*;
import java.time.*;
import java.util.function.Supplier;

public final class IdempotentExecutor {
  private final TransactionPort transactions;
  private final FinancePorts.Idempotency results;
  private final SerializationPort serialization;
  private final Clock clock;

  public IdempotentExecutor(
      TransactionPort transactions,
      FinancePorts.Idempotency results,
      SerializationPort serialization,
      Clock clock) {
    this.transactions = transactions;
    this.results = results;
    this.serialization = serialization;
    this.clock = clock;
  }

  public <T> T execute(
      String userId,
      String communityId,
      String operation,
      String key,
      Object command,
      Class<T> type,
      Supplier<T> work) {
    Checks.text(key, 100);
    String id = serialization.hash(userId + ":" + communityId + ":" + operation + ":" + key);
    String fingerprint = serialization.hash(serialization.canonical(command));
    try {
      return transactions.execute(
          () -> {
            var existing = results.find(id);
            if (existing.isPresent()) return replay(existing.get(), fingerprint, type);
            T result = work.get();
            results.insert(
                new IdempotentResult(
                    id,
                    communityId,
                    fingerprint,
                    serialization.canonical(result),
                    clock.instant().plus(Duration.ofDays(7)),
                    0));
            return result;
          });
    } catch (RuleViolation failure) {
      if (failure.kind() == RuleViolation.Kind.CONFLICT) {
        var committed = results.find(id);
        if (committed.isPresent()) return replay(committed.get(), fingerprint, type);
      }
      throw failure;
    }
  }

  private <T> T replay(IdempotentResult result, String fingerprint, Class<T> type) {
    if (!result.fingerprint().equals(fingerprint))
      throw new RuleViolation(
          RuleViolation.Kind.CONFLICT,
          "IDEMPOTENCY_CONFLICT",
          "The operation key was used with a different payload.");
    return serialization.read(result.json(), type);
  }
}
