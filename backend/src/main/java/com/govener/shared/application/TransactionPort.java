package com.govener.shared.application;

import java.util.function.Supplier;

public interface TransactionPort {
  <T> T execute(Supplier<T> work);
}
