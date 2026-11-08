package com.govener.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.govener.adapter.in.web.ProblemResponses;
import com.govener.identity.adapter.in.security.SessionVersionFilter;
import com.govener.identity.application.IdentityService;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.*;
import org.springframework.web.cors.*;

@Configuration
public class SecurityConfiguration {
  @Bean
  HttpSessionCsrfTokenRepository csrfRepository() {
    var repository = new HttpSessionCsrfTokenRepository();
    repository.setHeaderName("X-CSRF-Token");
    return repository;
  }

  @Bean
  HttpSessionSecurityContextRepository securityContexts() {
    return new HttpSessionSecurityContextRepository();
  }

  @Bean
  SecurityFilterChain security(
      HttpSecurity http,
      HttpSessionCsrfTokenRepository csrf,
      HttpSessionSecurityContextRepository contexts,
      IdentityService identity,
      ObjectMapper mapper,
      @Value("${app.allowed-origins}") String origins)
      throws Exception {
    CorsConfiguration cors = new CorsConfiguration();
    cors.setAllowedOrigins(Arrays.asList(origins.split(",")));
    cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    cors.setAllowedHeaders(
        List.of("Content-Type", "X-CSRF-Token", "Idempotency-Key", "Accept-Language"));
    cors.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", cors);
    http.cors(config -> config.configurationSource(source))
        .csrf(
            config ->
                config
                    .csrfTokenRepository(csrf)
                    .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                    .ignoringRequestMatchers(
                        "/api/v1/auth/password-reset-requests", "/api/v1/auth/password-resets"))
        .securityContext(
            config -> config.securityContextRepository(contexts).requireExplicitSave(true))
        .sessionManagement(config -> config.sessionFixation(fix -> fix.changeSessionId()))
        .authorizeHttpRequests(
            config ->
                config
                    .requestMatchers(
                        "/api/v1/auth/csrf",
                        "/api/v1/auth/login",
                        "/api/v1/auth/password-reset-requests",
                        "/api/v1/auth/password-resets",
                        "/actuator/health",
                        "/error")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            config ->
                config
                    .authenticationEntryPoint(
                        (request, response, failure) ->
                            ProblemResponses.write(
                                mapper,
                                request,
                                response,
                                401,
                                "AUTHENTICATION_REQUIRED",
                                "Authentication is required."))
                    .accessDeniedHandler(
                        (request, response, failure) ->
                            ProblemResponses.write(
                                mapper,
                                request,
                                response,
                                403,
                                "ACCESS_DENIED",
                                "The operation or CSRF token is not permitted.")))
        .requestCache(config -> config.disable())
        .formLogin(config -> config.disable())
        .httpBasic(config -> config.disable())
        .logout(config -> config.disable())
        .addFilterBefore(new SessionVersionFilter(identity), CsrfFilter.class);
    return http.build();
  }
}
