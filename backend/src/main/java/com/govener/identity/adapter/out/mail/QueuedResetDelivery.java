package com.govener.identity.adapter.out.mail;

import com.govener.identity.adapter.out.mongo.MongoResetEmailRepository;
import com.govener.identity.application.IdentityPorts;
import com.govener.identity.domain.ResetEmail;
import java.time.*;
import java.util.UUID;

public final class QueuedResetDelivery implements IdentityPorts.ResetDelivery {
  private final MongoResetEmailRepository outbox;
  private final ResetTokenCipher cipher;
  private final Clock clock;

  public QueuedResetDelivery(
      MongoResetEmailRepository outbox, ResetTokenCipher cipher, Clock clock) {
    this.outbox = outbox;
    this.cipher = cipher;
    this.clock = clock;
  }

  public void deliver(String email, String rawToken) {
    outbox.insert(
        new ResetEmail(
            UUID.randomUUID().toString(),
            email,
            cipher.encrypt(rawToken),
            clock.instant(),
            clock.instant().plus(Duration.ofMinutes(15)),
            null,
            null,
            0));
  }
}
