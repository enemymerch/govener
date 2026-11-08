package com.govener.configuration;

import com.govener.identity.adapter.out.mail.*;
import com.govener.identity.adapter.out.mongo.MongoResetEmailRepository;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true")
public class MailConfiguration {
  @Bean
  ResetMailDispatcher resetMailDispatcher(
      MongoResetEmailRepository outbox,
      ResetTokenCipher cipher,
      JavaMailSender sender,
      @Value("${app.mail.from}") String from,
      @Value("${app.mail.reset-url}") String url,
      Clock clock) {
    return new ResetMailDispatcher(outbox, cipher, sender, from, url, clock);
  }

  @Bean
  MailJob mailJob(ResetMailDispatcher dispatcher) {
    return new MailJob(dispatcher);
  }

  public static class MailJob {
    private final ResetMailDispatcher dispatcher;

    public MailJob(ResetMailDispatcher dispatcher) {
      this.dispatcher = dispatcher;
    }

    @Scheduled(fixedDelay = 10000)
    public void deliver() {
      dispatcher.process();
    }
  }
}
