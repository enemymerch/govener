package com.govener.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.govener.administration.adapter.out.mongo.MongoAdminAuthorityRepository;
import com.govener.administration.application.*;
import com.govener.community.application.CommunityPorts;
import com.govener.finance.application.FinancePorts;
import com.govener.identity.application.IdentityPorts;
import com.govener.shared.application.*;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
public class AdministrationConfiguration {
  @Bean
  AdministrationPorts.Authorities authorities(MongoTemplate mongo, ObjectMapper mapper) {
    return new MongoAdminAuthorityRepository(mongo, mapper);
  }

  @Bean
  AdminAccess adminAccess(
      AdministrationPorts.Authorities authorities, CommunityPorts.Communities communities) {
    return new AdminAccess(authorities, communities);
  }

  @Bean
  AdministrationService administrationService(
      AdminAccess access,
      AdministrationPorts.Authorities authorities,
      CommunityPorts.Communities communities,
      CommunityPorts.Units units,
      CommunityPorts.People people,
      CommunityPorts.Memberships memberships,
      CommunityPorts.Relationships relationships,
      CommunityPorts.Residences residences,
      IdentityPorts.Users users,
      IdentityPorts.Passwords passwords,
      FinancePorts.Accounts accounts,
      TransactionPort transactions,
      AuditPort audit,
      Clock clock) {
    return new AdministrationService(
        access,
        authorities,
        communities,
        units,
        people,
        memberships,
        relationships,
        residences,
        users,
        passwords,
        accounts,
        transactions,
        audit,
        clock);
  }

  @Bean
  @ConditionalOnProperty(name = "app.root-bootstrap.enabled", havingValue = "true")
  ApplicationRunner rootBootstrap(
      AdministrationService service,
      @Value("${GOVENER_ROOT_NAME:System Root}") String name,
      @Value("${GOVENER_ROOT_EMAIL:}") String email,
      @Value("${GOVENER_ROOT_PASSWORD:}") String password) {
    return args -> service.bootstrapRoot(name, email, password);
  }
}
