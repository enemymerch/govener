package com.govener.shared.adapter.out.mongo;

import com.govener.shared.application.TransactionPort;
import com.govener.shared.domain.RuleViolation;
import com.mongodb.MongoException;
import java.util.function.Supplier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.support.TransactionTemplate;

public final class MongoTransactionAdapter implements TransactionPort {
  private final TransactionTemplate transactions;

  public MongoTransactionAdapter(TransactionTemplate transactions) {
    this.transactions = transactions;
  }

  public <T> T execute(Supplier<T> work) {
    try {
      return transactions.execute(status -> work.get());
    } catch (RuntimeException failure) {
      Throwable cause = failure;
      while (cause != null) {
        if (cause instanceof DuplicateKeyException)
          throw RuleViolation.conflict("A unique record already exists.");
        if (cause instanceof MongoException mongo
            && (mongo.hasErrorLabel("TransientTransactionError")
                || mongo.getCode() == 112
                || mongo.getCode() == 11000))
          throw RuleViolation.conflict("Concurrent transaction conflict; retry deliberately.");
        cause = cause.getCause();
      }
      throw failure;
    }
  }
}
