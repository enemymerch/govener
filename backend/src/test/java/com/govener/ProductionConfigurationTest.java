package com.govener;

import static org.assertj.core.api.Assertions.*;

import com.govener.configuration.ProductionConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ProductionConfigurationTest {
  private MockEnvironment environment() {
    var env =
        new MockEnvironment()
            .withProperty("app.seed.enabled", "false")
            .withProperty("server.servlet.session.cookie.secure", "true")
            .withProperty("server.servlet.session.cookie.http-only", "true")
            .withProperty("app.cursor-secret", "production-test-secret-at-least-32-characters")
            .withProperty(
                "spring.data.mongodb.uri",
                "mongodb://app:test-password@localhost:27017/govener?replicaSet=rs0")
            .withProperty("app.allowed-origins", "https://pilot.example.com");
    env.setActiveProfiles("prod");
    return env;
  }

  @Test
  void dedicatedProductionConfigurationIsAccepted() {
    assertThatCode(() -> new ProductionConfiguration(environment()).validate())
        .doesNotThrowAnyException();
  }

  @Test
  void demoProfilesAndFixturesAreRejected() {
    var env = environment();
    env.setActiveProfiles("prod", "dev");
    assertThatThrownBy(() -> new ProductionConfiguration(env).validate())
        .isInstanceOf(IllegalStateException.class);
    env.setActiveProfiles("prod");
    env.setProperty("app.seed.enabled", "true");
    assertThatThrownBy(() -> new ProductionConfiguration(env).validate())
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void insecureCookiesAreRejected() {
    var env = environment().withProperty("server.servlet.session.cookie.secure", "false");
    assertThatThrownBy(() -> new ProductionConfiguration(env).validate())
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void unauthenticatedDatabaseIsRejected() {
    var env =
        environment().withProperty("spring.data.mongodb.uri", "mongodb://localhost:27017/govener");
    assertThatThrownBy(() -> new ProductionConfiguration(env).validate())
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void plaintextAndWildcardOriginsAreRejected() {
    for (String origin :
        new String[] {"http://pilot.example.com", "*", "https://pilot.example.com/path"}) {
      var env = environment().withProperty("app.allowed-origins", origin);
      assertThatThrownBy(() -> new ProductionConfiguration(env).validate())
          .isInstanceOf(IllegalStateException.class);
    }
  }

  @Test
  void developmentSecretIsRejected() {
    var env =
        environment()
            .withProperty("app.cursor-secret", "development-only-replace-before-deployment-2026");
    assertThatThrownBy(() -> new ProductionConfiguration(env).validate())
        .isInstanceOf(IllegalStateException.class);
  }
}
