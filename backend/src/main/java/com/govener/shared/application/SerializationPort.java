package com.govener.shared.application;

public interface SerializationPort {
  String canonical(Object value);

  <T> T read(String json, Class<T> type);

  String hash(String value);
}
