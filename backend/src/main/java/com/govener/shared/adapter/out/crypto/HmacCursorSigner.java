package com.govener.shared.adapter.out.crypto;

import com.govener.shared.application.CursorSigner;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class HmacCursorSigner implements CursorSigner {
  private final byte[] secret;

  public HmacCursorSigner(String secret) {
    if (secret == null || secret.length() < 32)
      throw new IllegalArgumentException(
          "GOVENER_CURSOR_SECRET must contain at least 32 characters.");
    this.secret = secret.getBytes(StandardCharsets.UTF_8);
  }

  public String sign(String value) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret, "HmacSHA256"));
      return Base64.getUrlEncoder()
          .withoutPadding()
          .encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    } catch (java.security.GeneralSecurityException failure) {
      throw new IllegalStateException(failure);
    }
  }

  public boolean valid(String value, String signature) {
    return MessageDigest.isEqual(
        sign(value).getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));
  }
}
