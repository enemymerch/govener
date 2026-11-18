package com.govener.configuration;

import com.mongodb.ConnectionString;
import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.util.*;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;

/** Reject development credentials and transport settings before a production instance starts. */
@Configuration
@Profile("prod")
public class ProductionConfiguration {
  private final Environment environment;

  public ProductionConfiguration(Environment environment) {
    this.environment = environment;
  }

  @PostConstruct
  public void validate() {
    if (Arrays.stream(environment.getActiveProfiles())
            .anyMatch(profile -> Set.of("dev", "test").contains(profile))
        || environment.getProperty("app.seed.enabled", Boolean.class, false))
      throw new IllegalStateException("Production cannot enable development fixtures.");
    if (!environment.getProperty("server.servlet.session.cookie.secure", Boolean.class, false)
        || !environment.getProperty(
            "server.servlet.session.cookie.http-only", Boolean.class, false))
      throw new IllegalStateException("Production requires Secure and HttpOnly session cookies.");
    String secret = environment.getProperty("app.cursor-secret", "");
    if (secret.length() < 32 || secret.startsWith("development-") || secret.startsWith("replace-"))
      throw new IllegalStateException("Production requires a dedicated random cursor secret.");
    try {
      var connection = new ConnectionString(environment.getProperty("spring.data.mongodb.uri", ""));
      if (connection.getCredential() == null || connection.getCredential().getPassword() == null)
        throw new IllegalArgumentException();
    } catch (RuntimeException failure) {
      throw new IllegalStateException("Production requires an authenticated MongoDB connection.");
    }
    String origins = environment.getProperty("app.allowed-origins", "");
    try {
      if (origins.isBlank()) throw new IllegalArgumentException();
      for (String origin : origins.split(",")) {
        var uri = URI.create(origin);
        if (!"https".equals(uri.getScheme())
            || uri.getHost() == null
            || uri.getUserInfo() != null
            || uri.getQuery() != null
            || uri.getFragment() != null
            || !uri.getPath().isEmpty()) throw new IllegalArgumentException();
      }
    } catch (RuntimeException failure) {
      throw new IllegalStateException("Production requires explicit HTTPS frontend origins.");
    }
  }
}
