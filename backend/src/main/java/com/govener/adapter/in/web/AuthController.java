package com.govener.adapter.in.web;

import static com.govener.adapter.in.web.Views.map;

import com.govener.adapter.in.web.dto.*;
import com.govener.community.application.AccessService;
import com.govener.identity.adapter.in.security.*;
import com.govener.identity.application.IdentityService;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import java.time.Clock;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.*;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
  private final IdentityService identity;
  private final CurrentActor current;
  private final AccessService access;
  private final ViewMapper views;
  private final HttpSessionCsrfTokenRepository csrf;
  private final HttpSessionSecurityContextRepository contexts;
  private final Clock clock;

  public AuthController(
      IdentityService identity,
      CurrentActor current,
      AccessService access,
      ViewMapper views,
      HttpSessionCsrfTokenRepository csrf,
      HttpSessionSecurityContextRepository contexts,
      Clock clock) {
    this.identity = identity;
    this.current = current;
    this.access = access;
    this.views = views;
    this.csrf = csrf;
    this.contexts = contexts;
    this.clock = clock;
  }

  @GetMapping("/csrf")
  public Object csrf(HttpServletRequest request) {
    identity.csrfRequested(request.getRemoteAddr());
    return map(
        "csrfToken", ((CsrfToken) request.getAttribute(CsrfToken.class.getName())).getToken());
  }

  @PostMapping("/login")
  public Object login(
      @Valid @RequestBody LoginRequest body,
      HttpServletRequest request,
      HttpServletResponse response) {
    var user = identity.login(body.email(), body.password(), request.getRemoteAddr());
    request.getSession();
    request.changeSessionId();
    var context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(
        UsernamePasswordAuthenticationToken.authenticated(
            new SessionPrincipal(user.id(), user.securityVersion()), null, List.of()));
    SecurityContextHolder.setContext(context);
    contexts.saveContext(context, request, response);
    var token = csrf.generateToken(request);
    csrf.saveToken(token, request, response);
    return session(request, token.getToken());
  }

  @GetMapping("/session")
  public Object session(HttpServletRequest request) {
    return session(
        request, ((CsrfToken) request.getAttribute(CsrfToken.class.getName())).getToken());
  }

  private Object session(HttpServletRequest request, String token) {
    var user = current.user();
    var members = access.memberships(user.id());
    return map(
        "user",
        views.profile(user),
        "roles",
        members.stream().flatMap(m -> m.roles().stream()).distinct().sorted().toList(),
        "permissions",
        members.stream().flatMap(m -> m.permissions().stream()).distinct().sorted().toList(),
        "csrfToken",
        token,
        "expiresAt",
        clock.instant().plusSeconds(request.getSession().getMaxInactiveInterval()));
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout(HttpServletRequest request, HttpServletResponse response) {
    var session = request.getSession(false);
    if (session != null) session.invalidate();
    SecurityContextHolder.clearContext();
    csrf.saveToken(null, request, response);
  }

  @PostMapping("/password-reset-requests")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public void requestReset(@Valid @RequestBody ResetRequest body, HttpServletRequest request) {
    identity.requestReset(body.email(), request.getRemoteAddr());
  }

  @PostMapping("/password-resets")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void reset(@Valid @RequestBody PasswordReset body, HttpServletRequest request) {
    identity.resetPassword(body.token(), body.password(), request.getRemoteAddr());
  }
}
