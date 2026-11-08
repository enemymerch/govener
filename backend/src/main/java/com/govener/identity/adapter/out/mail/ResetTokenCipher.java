package com.govener.identity.adapter.out.mail;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.*;

public final class ResetTokenCipher {
  private final SecretKeySpec key;
  private final SecureRandom random = new SecureRandom();

  public ResetTokenCipher(String rootSecret) {
    try {
      key =
          new SecretKeySpec(
              MessageDigest.getInstance("SHA-256")
                  .digest(
                      (rootSecret + ":password-reset-mail:v1").getBytes(StandardCharsets.UTF_8)),
              "AES");
    } catch (GeneralSecurityException failure) {
      throw new IllegalStateException(failure);
    }
  }

  public String encrypt(String token) {
    try {
      byte[] nonce = new byte[12];
      random.nextBytes(nonce);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, nonce));
      byte[] encrypted = cipher.doFinal(token.getBytes(StandardCharsets.UTF_8));
      byte[] value = new byte[nonce.length + encrypted.length];
      System.arraycopy(nonce, 0, value, 0, nonce.length);
      System.arraycopy(encrypted, 0, value, nonce.length, encrypted.length);
      return Base64.getEncoder().encodeToString(value);
    } catch (GeneralSecurityException failure) {
      throw new IllegalStateException("Reset delivery encryption failed.", failure);
    }
  }

  public String decrypt(String encoded) {
    try {
      byte[] value = Base64.getDecoder().decode(encoded);
      if (value.length < 28) throw new GeneralSecurityException();
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, Arrays.copyOf(value, 12)));
      return new String(
          cipher.doFinal(Arrays.copyOfRange(value, 12, value.length)), StandardCharsets.UTF_8);
    } catch (GeneralSecurityException | IllegalArgumentException failure) {
      throw new IllegalStateException("Reset delivery content is invalid.", failure);
    }
  }
}
