package com.govener.shared.application;

public interface CursorSigner {
  String sign(String value);

  boolean valid(String value, String signature);
}
