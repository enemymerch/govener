package com.govener.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.*;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
public class RepositoryConfiguration {
  @Bean
  com.govener.identity.application.IdentityPorts.Users userRepository(
      MongoTemplate mongo, ObjectMapper mapper) {
    return new com.govener.identity.adapter.out.mongo.MongoUserRepository(mongo, mapper);
  }

  @Bean
  com.govener.identity.application.IdentityPorts.Resets resetRepository(
      MongoTemplate mongo, ObjectMapper mapper) {
    return new com.govener.identity.adapter.out.mongo.MongoResetRepository(mongo, mapper);
  }

  @Bean
  com.govener.community.application.CommunityPorts.Communities communityRepository(
      MongoTemplate mongo, ObjectMapper mapper) {
    return new com.govener.community.adapter.out.mongo.MongoCommunityRepository(mongo, mapper);
  }

  @Bean
  com.govener.community.application.CommunityPorts.Units unitRepository(
      MongoTemplate mongo, ObjectMapper mapper) {
    return new com.govener.community.adapter.out.mongo.MongoUnitRepository(mongo, mapper);
  }

  @Bean
  com.govener.community.application.CommunityPorts.People personRepository(
      MongoTemplate mongo, ObjectMapper mapper) {
    return new com.govener.community.adapter.out.mongo.MongoPersonRepository(mongo, mapper);
  }

  @Bean
  com.govener.community.application.CommunityPorts.Memberships membershipRepository(
      MongoTemplate mongo, ObjectMapper mapper) {
    return new com.govener.community.adapter.out.mongo.MongoMembershipRepository(mongo, mapper);
  }

  @Bean
  com.govener.community.application.CommunityPorts.Relationships relationshipRepository(
      MongoTemplate mongo, ObjectMapper mapper) {
    return new com.govener.community.adapter.out.mongo.MongoRelationshipRepository(mongo, mapper);
  }

  @Bean
  com.govener.community.application.CommunityPorts.Residences residenceRepository(
      MongoTemplate mongo, ObjectMapper mapper) {
    return new com.govener.community.adapter.out.mongo.MongoResidenceRepository(mongo, mapper);
  }

  @Bean
  com.govener.finance.application.FinancePorts.Charges chargeRepository(
      MongoTemplate mongo, ObjectMapper mapper) {
    return new com.govener.finance.adapter.out.mongo.MongoChargeRepository(mongo, mapper);
  }

  @Bean
  com.govener.finance.application.FinancePorts.Payments paymentRepository(
      MongoTemplate mongo, ObjectMapper mapper) {
    return new com.govener.finance.adapter.out.mongo.MongoPaymentRepository(mongo, mapper);
  }

  @Bean
  com.govener.finance.application.FinancePorts.Accounts financialAccountRepository(
      MongoTemplate mongo, ObjectMapper mapper) {
    return new com.govener.finance.adapter.out.mongo.MongoFinancialAccountRepository(mongo, mapper);
  }

  @Bean
  com.govener.finance.application.FinancePorts.UnitAccounts unitAccountRepository(
      MongoTemplate mongo, ObjectMapper mapper) {
    return new com.govener.finance.adapter.out.mongo.MongoUnitAccountRepository(mongo, mapper);
  }

  @Bean
  com.govener.finance.application.FinancePorts.Entries accountEntryRepository(
      MongoTemplate mongo, ObjectMapper mapper) {
    return new com.govener.finance.adapter.out.mongo.MongoAccountEntryRepository(mongo, mapper);
  }

  @Bean
  com.govener.finance.application.FinancePorts.Expenses expenseRepository(
      MongoTemplate mongo, ObjectMapper mapper) {
    return new com.govener.finance.adapter.out.mongo.MongoExpenseRepository(mongo, mapper);
  }

  @Bean
  com.govener.finance.application.FinancePorts.ExpensePayments expensePaymentRepository(
      MongoTemplate mongo, ObjectMapper mapper) {
    return new com.govener.finance.adapter.out.mongo.MongoExpensePaymentRepository(mongo, mapper);
  }

  @Bean
  com.govener.finance.application.FinancePorts.Notifications paymentNotificationRepository(
      MongoTemplate mongo, ObjectMapper mapper) {
    return new com.govener.finance.adapter.out.mongo.MongoPaymentNotificationRepository(
        mongo, mapper);
  }

  @Bean
  com.govener.finance.application.FinancePorts.Batches chargeBatchRepository(
      MongoTemplate mongo, ObjectMapper mapper) {
    return new com.govener.finance.adapter.out.mongo.MongoChargeBatchRepository(mongo, mapper);
  }

  @Bean
  com.govener.finance.application.FinancePorts.Idempotency idempotencyRepository(
      MongoTemplate mongo, ObjectMapper mapper) {
    return new com.govener.finance.adapter.out.mongo.MongoIdempotencyRepository(mongo, mapper);
  }

  @Bean
  com.govener.service.application.RequestPorts.Requests requestRepository(
      MongoTemplate mongo, ObjectMapper mapper) {
    return new com.govener.service.adapter.out.mongo.MongoRequestRepository(mongo, mapper);
  }

  @Bean
  com.govener.service.application.RequestPorts.Comments commentRepository(
      MongoTemplate mongo, ObjectMapper mapper) {
    return new com.govener.service.adapter.out.mongo.MongoCommentRepository(mongo, mapper);
  }

  @Bean
  com.govener.service.application.RequestPorts.Events requestEventRepository(
      MongoTemplate mongo, ObjectMapper mapper) {
    return new com.govener.service.adapter.out.mongo.MongoRequestEventRepository(mongo, mapper);
  }

  @Bean
  com.govener.communication.application.AnnouncementPorts.Announcements announcementRepository(
      MongoTemplate mongo, ObjectMapper mapper) {
    return new com.govener.communication.adapter.out.mongo.MongoAnnouncementRepository(
        mongo, mapper);
  }

  @Bean
  com.govener.notification.application.NotificationPorts.Notifications notificationRepository(
      MongoTemplate mongo, ObjectMapper mapper) {
    return new com.govener.notification.adapter.out.mongo.MongoNotificationRepository(
        mongo, mapper);
  }

  @Bean
  com.govener.notification.application.NotificationPorts.Events eventRepository(
      MongoTemplate mongo, ObjectMapper mapper) {
    return new com.govener.notification.adapter.out.mongo.MongoEventRepository(mongo, mapper);
  }

  @Bean
  com.govener.attachment.application.AttachmentPorts.Metadata attachmentRepository(
      MongoTemplate mongo, ObjectMapper mapper) {
    return new com.govener.attachment.adapter.out.mongo.MongoAttachmentRepository(mongo, mapper);
  }

  @Bean
  com.govener.shared.application.AuditPort auditPort(MongoTemplate mongo, ObjectMapper mapper) {
    return new com.govener.shared.adapter.out.mongo.MongoAuditAdapter(mongo, mapper);
  }
}
