package com.govener.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.govener.attachment.adapter.out.mongo.*;
import com.govener.attachment.adapter.out.validation.*;
import com.govener.attachment.application.*;
import com.govener.communication.application.*;
import com.govener.community.application.*;
import com.govener.finance.application.*;
import com.govener.identity.adapter.out.mail.*;
import com.govener.identity.adapter.out.mongo.*;
import com.govener.identity.adapter.out.security.*;
import com.govener.identity.application.*;
import com.govener.notification.application.*;
import com.govener.service.adapter.out.mongo.*;
import com.govener.service.application.*;
import com.govener.shared.adapter.out.crypto.*;
import com.govener.shared.adapter.out.json.*;
import com.govener.shared.adapter.out.mongo.*;
import com.govener.shared.application.*;
import com.mongodb.*;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.context.annotation.*;
import org.springframework.data.mongodb.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class ApplicationConfiguration {
  @Bean
  @ConditionalOnMissingBean
  Clock clock() {
    return Clock.systemUTC();
  }

  @Bean
  SerializationPort serialization(ObjectMapper mapper) {
    return new JacksonSerializationAdapter(mapper);
  }

  @Bean
  MongoTransactionManager transactionManager(MongoDatabaseFactory factory) {
    return new MongoTransactionManager(
        factory,
        TransactionOptions.builder()
            .readConcern(ReadConcern.SNAPSHOT)
            .writeConcern(WriteConcern.MAJORITY)
            .build());
  }

  @Bean
  TransactionPort transactions(MongoTransactionManager manager) {
    return new MongoTransactionAdapter(new TransactionTemplate(manager));
  }

  @Bean
  CursorSigner cursorSigner(@Value("${app.cursor-secret}") String secret) {
    return new HmacCursorSigner(secret);
  }

  @Bean
  Pagination pagination(CursorSigner signer, SerializationPort serialization, Clock clock) {
    return new Pagination(signer, serialization, clock);
  }

  @Bean
  IdentityPorts.Passwords passwords() {
    return new Pbkdf2PasswordAdapter();
  }

  @Bean
  IdentityPorts.Attempts attempts(
      MongoTemplate mongo, SerializationPort serialization, Clock clock) {
    return new MongoAttemptLimiter(mongo, serialization, clock);
  }

  @Bean
  MongoResetEmailRepository resetEmailRepository(MongoTemplate mongo, ObjectMapper mapper) {
    return new MongoResetEmailRepository(mongo, mapper);
  }

  @Bean
  ResetTokenCipher resetCipher(@Value("${app.cursor-secret}") String secret) {
    return new ResetTokenCipher(secret);
  }

  @Bean
  @ConditionalOnMissingBean(IdentityPorts.ResetDelivery.class)
  IdentityPorts.ResetDelivery resetDelivery(
      MongoResetEmailRepository outbox, ResetTokenCipher cipher, Clock clock) {
    return new QueuedResetDelivery(outbox, cipher, clock);
  }

  @Bean
  IdentityService identity(
      IdentityPorts.Users users,
      IdentityPorts.Resets resets,
      IdentityPorts.Passwords passwords,
      IdentityPorts.ResetDelivery delivery,
      IdentityPorts.Attempts attempts,
      TransactionPort transactions,
      SerializationPort serialization,
      Clock clock) {
    SecureRandom random = new SecureRandom();
    Supplier<String> tokens =
        () -> {
          byte[] value = new byte[32];
          random.nextBytes(value);
          return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
        };
    return new IdentityService(
        users, resets, passwords, delivery, attempts, transactions, serialization, tokens, clock);
  }

  @Bean
  AccessService access(
      CommunityPorts.Memberships memberships, CommunityPorts.Units units, Clock clock) {
    return new AccessService(memberships, units, clock);
  }

  @Bean
  CommunityQueries communityQueries(
      CommunityPorts.Communities communities,
      CommunityPorts.Units units,
      CommunityPorts.People people,
      CommunityPorts.Relationships relationships,
      CommunityPorts.Residences residences,
      IdentityPorts.Users users,
      AccessService access) {
    return new CommunityQueries(
        communities, units, people, relationships, residences, users, access);
  }

  @Bean
  AttachmentPorts.Blobs blobs(GridFsTemplate grid) {
    return new GridFsBlobAdapter(grid);
  }

  @Bean
  AttachmentPorts.ContentValidator fileValidator() {
    return new FileContentValidator();
  }

  @Bean
  AttachmentService attachments(
      AttachmentPorts.Metadata metadata,
      AttachmentPorts.Blobs blobs,
      AttachmentPorts.ContentValidator validator,
      AccessService access,
      Clock clock) {
    return new AttachmentService(metadata, blobs, validator, access, clock);
  }

  @Bean
  NotificationPorts.Publisher eventPublisher(NotificationPorts.Events events) {
    return events::insert;
  }

  @Bean
  ActivityPublisher activity(AuditPort audit, NotificationPorts.Publisher publisher, Clock clock) {
    return new ActivityPublisher(audit, publisher, clock);
  }

  @Bean
  NotificationService notificationService(
      NotificationPorts.Notifications notifications,
      AccessService access,
      TransactionPort transactions,
      Clock clock) {
    return new NotificationService(notifications, access, transactions, clock);
  }

  @Bean
  OutboxProjector outboxProjector(
      NotificationPorts.Events events,
      NotificationPorts.Notifications notifications,
      IdentityPorts.Users users,
      AccessService access,
      TransactionPort transactions,
      SerializationPort serialization,
      Clock clock) {
    return new OutboxProjector(
        events, notifications, users, access, transactions, serialization, clock);
  }

  @Bean
  FinanceRepositories financeRepositories(
      FinancePorts.Charges charges,
      FinancePorts.Payments payments,
      FinancePorts.Accounts accounts,
      FinancePorts.UnitAccounts units,
      FinancePorts.Entries entries,
      FinancePorts.Expenses expenses,
      FinancePorts.ExpensePayments expensePayments,
      FinancePorts.Notifications notifications,
      FinancePorts.Batches batches) {
    return new FinanceRepositories(
        charges,
        payments,
        accounts,
        units,
        entries,
        expenses,
        expensePayments,
        notifications,
        batches);
  }

  @Bean
  IdempotentExecutor idempotency(
      TransactionPort transactions,
      FinancePorts.Idempotency results,
      SerializationPort serialization,
      Clock clock) {
    return new IdempotentExecutor(transactions, results, serialization, clock);
  }

  @Bean
  ManualAllocationPolicy allocationPolicy() {
    return new ManualAllocationPolicy();
  }

  @Bean
  FinanceService finance(
      FinanceRepositories store,
      AccessService access,
      IdempotentExecutor idempotency,
      TransactionPort transactions,
      AttachmentService attachments,
      ActivityPublisher activity,
      SerializationPort serialization,
      Clock clock,
      ManualAllocationPolicy allocations) {
    return new FinanceService(
        store,
        access,
        idempotency,
        transactions,
        attachments,
        activity,
        serialization,
        clock,
        allocations);
  }

  @Bean
  FinancialVisibility financialVisibility(
      @Value("${app.resident-financial-visibility:CURRENT_RELATIONSHIP_PERIOD}") String policy) {
    return switch (policy) {
      case "CURRENT_RELATIONSHIP_PERIOD" -> FinancialVisibility.relationshipPeriod();
      case "DENY_UNCONFIGURED" -> FinancialVisibility.denyUnconfigured();
      default ->
          throw new IllegalArgumentException("Unknown resident financial visibility policy.");
    };
  }

  @Bean
  FinanceQueries financeQueries(
      FinanceRepositories store,
      AccessService access,
      FinanceService finance,
      FinancialVisibility visibility,
      CommunityPorts.Units units) {
    return new FinanceQueries(store, access, finance, visibility, units);
  }

  @Bean
  RequestPorts.Sequences sequences(MongoTemplate mongo) {
    return new MongoSequenceAdapter(mongo);
  }

  @Bean
  RequestService requests(
      RequestPorts.Requests requests,
      RequestPorts.Comments comments,
      RequestPorts.Events events,
      RequestPorts.Sequences sequences,
      AccessService access,
      IdentityPorts.Users users,
      TransactionPort transactions,
      AttachmentService attachments,
      ActivityPublisher activity,
      Clock clock) {
    return new RequestService(
        requests,
        comments,
        events,
        sequences,
        access,
        users,
        transactions,
        attachments,
        activity,
        clock);
  }

  @Bean
  AnnouncementService announcements(
      AnnouncementPorts.Announcements announcements,
      AccessService access,
      CommunityPorts.Units units,
      TransactionPort transactions,
      ActivityPublisher activity,
      Clock clock) {
    return new AnnouncementService(announcements, access, units, transactions, activity, clock);
  }

  @Bean
  DashboardQueries dashboardQueries(
      AccessService access,
      CommunityQueries communities,
      FinanceQueries finance,
      RequestService requests) {
    return new DashboardQueries(access, communities, finance, requests);
  }
}
