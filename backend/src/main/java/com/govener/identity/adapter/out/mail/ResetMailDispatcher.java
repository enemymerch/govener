package com.govener.identity.adapter.out.mail;

import com.govener.identity.adapter.out.mongo.MongoResetEmailRepository;
import com.govener.identity.domain.ResetEmail;
import com.govener.shared.domain.RuleViolation;
import java.time.*;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

public final class ResetMailDispatcher {
  private final MongoResetEmailRepository outbox;
  private final ResetTokenCipher cipher;
  private final JavaMailSender sender;
  private final String from;
  private final String resetUrl;
  private final Clock clock;

  public ResetMailDispatcher(
      MongoResetEmailRepository outbox,
      ResetTokenCipher cipher,
      JavaMailSender sender,
      String from,
      String resetUrl,
      Clock clock) {
    this.outbox = outbox;
    this.cipher = cipher;
    this.sender = sender;
    this.from = from;
    this.resetUrl = resetUrl;
    this.clock = clock;
  }

  public void process() {
    for (ResetEmail candidate :
        outbox.all().stream()
            .filter(
                item ->
                    item.sentAt() == null
                        && item.expiresAt().isAfter(clock.instant())
                        && (item.claimedUntil() == null
                            || !item.claimedUntil().isAfter(clock.instant())))
            .limit(20)
            .toList()) {
      ResetEmail claimed = candidate.claim(clock.instant().plusSeconds(60));
      try {
        outbox.save(claimed, candidate.version());
      } catch (RuleViolation busy) {
        continue;
      }
      try {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(candidate.email());
        message.setSubject("Reset your yönetici password");
        message.setText(
            "Use this single-use link within 15 minutes: "
                + resetUrl
                + "?token="
                + cipher.decrypt(candidate.encryptedToken()));
        sender.send(message);
        outbox.save(claimed.sent(clock.instant()), claimed.version());
      } catch (org.springframework.mail.MailException failure) {
        /* Lease expiry permits a later retry without logging tokens or addresses. */
      }
    }
  }
}
