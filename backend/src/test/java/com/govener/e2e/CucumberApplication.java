package com.govener.e2e;

import com.govener.GovenerApplication;
import io.cucumber.spring.CucumberContextConfiguration;
import java.time.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.test.context.*;
import org.testcontainers.containers.MongoDBContainer;

@CucumberContextConfiguration
@SpringBootTest(
    classes = {GovenerApplication.class, CucumberApplication.TimeConfiguration.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "debug=false",
      "trace=false",
      "app.seed.enabled=true",
      "app.jobs.enabled=false",
      "app.cursor-secret=acceptance-test-secret-at-least-32-characters",
      "server.servlet.session.cookie.secure=false"
    })
@ActiveProfiles("test")
public class CucumberApplication {
  static final MongoDBContainer MONGO = new MongoDBContainer("mongo:8.0.12");

  static {
    MONGO.start();
  }

  @DynamicPropertySource
  static void mongo(DynamicPropertyRegistry properties) {
    properties.add("spring.data.mongodb.uri", MONGO::getReplicaSetUrl);
  }

  @TestConfiguration
  static class TimeConfiguration {
    @Bean
    @Primary
    Clock testClock() {
      return Clock.fixed(Instant.parse("2026-11-08T09:00:00Z"), ZoneOffset.UTC);
    }
  }
}
