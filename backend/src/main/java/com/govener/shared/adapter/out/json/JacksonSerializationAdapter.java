package com.govener.shared.adapter.out.json;

import com.fasterxml.jackson.databind.*;
import com.govener.shared.application.SerializationPort;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.HexFormat;

public final class JacksonSerializationAdapter implements SerializationPort {
  private final ObjectMapper mapper;

  public JacksonSerializationAdapter(ObjectMapper mapper) {
    this.mapper = mapper.copy().configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
  }

  public String canonical(Object value) {
    try {
      return mapper.writeValueAsString(value);
    } catch (com.fasterxml.jackson.core.JsonProcessingException failure) {
      throw new IllegalStateException("Serialization failed.", failure);
    }
  }

  public <T> T read(String json, Class<T> type) {
    try {
      return mapper.readValue(json, type);
    } catch (com.fasterxml.jackson.core.JsonProcessingException failure) {
      throw new IllegalStateException("Stored result is invalid.", failure);
    }
  }

  public String hash(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException impossible) {
      throw new IllegalStateException(impossible);
    }
  }
}
